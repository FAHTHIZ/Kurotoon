// PWA service worker registration with strict iframe / preview guard.
// Service workers are intentionally NOT registered inside Lovable preview
// iframes — they cause stale content. They activate on the published site only.

const CHAPTER_CACHE = "kt-offline-chapters";

function isUnsafeHost(): boolean {
  if (typeof window === "undefined") return true;
  const inIframe = (() => {
    try { return window.self !== window.top; } catch { return true; }
  })();
  const host = window.location.hostname;
  const previewHost =
    host.includes("id-preview--") ||
    host.includes("lovableproject.com") ||
    host.includes("lovableproject-dev.com") ||
    host.startsWith("preview--");
  return inIframe || previewHost;
}

export async function registerPWA() {
  // The current production build (TanStack Start on Cloudflare Workers) does
  // not emit `/sw.js` — registration was 404ing and old SWs on user devices
  // were serving stale HTML, which looked like "scraped series disappeared".
  // Aggressively unregister any existing SW and purge caches on every load
  // so every device always fetches fresh HTML + data from the network.
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) return;
  try {
    const regs = await navigator.serviceWorker.getRegistrations();
    await Promise.all(regs.map((r) => r.unregister()));
    if ("caches" in window) {
      const keys = await caches.keys();
      // Keep offline-downloaded chapter caches; drop shell/asset caches only.
      const kill = keys.filter(
        (k) => !k.startsWith("kt-offline-") && k !== "kt-chapter-pages",
      );
      await Promise.all(kill.map((k) => caches.delete(k)));
    }
  } catch (e) {
    console.warn("[pwa] cleanup failed", e);
  }
}

export function pwaEnabled() {
  // SW is disabled in production until a real /sw.js ships; offline chapter
  // downloads still work via the Cache API without a service worker.
  return typeof window !== "undefined" && "caches" in window;
}

/** Pre-cache every page image of a chapter for offline reading. */
export async function downloadChapter(chapterId: string, urls: string[]) {
  if (!("caches" in window)) throw new Error("Cache API unavailable");
  const cache = await caches.open(CHAPTER_CACHE);
  let done = 0;
  for (const url of urls) {
    try {
      const req = new Request(url, { mode: "cors", credentials: "omit" });
      const res = await fetch(req);
      if (res.ok || res.type === "opaque") await cache.put(req, res.clone());
    } catch (e) {
      console.warn("[offline] failed to cache", url, e);
    } finally {
      done++;
      window.dispatchEvent(new CustomEvent("kt-download-progress", {
        detail: { chapterId, done, total: urls.length },
      }));
    }
  }
  const manifest = await caches.open("kt-offline-manifest");
  await manifest.put(
    new Request(`/offline/${chapterId}`),
    new Response(JSON.stringify({ chapterId, urls, savedAt: Date.now() }), {
      headers: { "content-type": "application/json" },
    }),
  );
}

export async function isChapterDownloaded(chapterId: string): Promise<boolean> {
  if (!("caches" in window)) return false;
  const manifest = await caches.open("kt-offline-manifest");
  const m = await manifest.match(`/offline/${chapterId}`);
  return !!m;
}

export async function removeChapter(chapterId: string) {
  if (!("caches" in window)) return;
  const manifest = await caches.open("kt-offline-manifest");
  const m = await manifest.match(`/offline/${chapterId}`);
  if (!m) return;
  const { urls } = await m.json();
  const cache = await caches.open(CHAPTER_CACHE);
  await Promise.all((urls as string[]).map((u) => cache.delete(u)));
  await manifest.delete(`/offline/${chapterId}`);
}
