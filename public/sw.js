/* KuroToon service worker — app shell + hashed assets for offline reading.
 * Downloaded chapter pages live in IndexedDB (src/lib/downloads.ts), not here.
 * kt-precache.json is emitted by the build (vite.config.ts → ktPrecache). */
const VERSION = "kt-sw-1";
const ASSETS = `${VERSION}-assets`;
const SHELL = `${VERSION}-shell`;

self.addEventListener("install", (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(ASSETS);
      try {
        const res = await fetch("/kt-precache.json", { cache: "no-store" });
        if (res.ok) {
          const files = await res.json();
          await Promise.allSettled(files.map((f) => cache.add(f)));
        }
      } catch {}
      const shell = await caches.open(SHELL);
      await shell.add("/").catch(() => {});
      await self.skipWaiting();
    })(),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(keys.filter((k) => !k.startsWith(VERSION)).map((k) => caches.delete(k)));
      await self.clients.claim();
    })(),
  );
});

function timeout(ms) {
  return new Promise((_, reject) => setTimeout(() => reject(new Error("timeout")), ms));
}

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return; // source images go through the relay / IDB
  if (url.pathname.startsWith("/api/")) return;

  if (req.mode === "navigate") {
    // Network first so online users always get fresh HTML; cached shell when offline.
    event.respondWith(
      (async () => {
        const shell = await caches.open(SHELL);
        try {
          const res = await Promise.race([fetch(req), timeout(5000)]);
          if (res.ok) shell.put(url.pathname, res.clone());
          return res;
        } catch {
          return (
            (await shell.match(url.pathname)) ||
            (await shell.match("/downloads")) ||
            (await shell.match("/")) ||
            new Response("Offline", { status: 503, headers: { "content-type": "text/plain" } })
          );
        }
      })(),
    );
    return;
  }

  if (url.pathname.startsWith("/assets/") || /\.(?:png|svg|ico|webmanifest|woff2?)$/.test(url.pathname)) {
    // Hashed / static files: cache first.
    event.respondWith(
      (async () => {
        const cache = await caches.open(ASSETS);
        const hit = await cache.match(req);
        if (hit) return hit;
        const res = await fetch(req);
        if (res.ok) cache.put(req, res.clone());
        return res;
      })(),
    );
  }
});
