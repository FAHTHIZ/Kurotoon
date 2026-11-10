// PWA service worker registration with strict iframe / preview guard.
// Service workers are intentionally NOT registered inside Lovable preview
// iframes — they cause stale content. They activate on the published site only.

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
