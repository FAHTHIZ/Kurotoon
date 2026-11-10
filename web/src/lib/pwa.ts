// Service worker registration. The SW (public/sw.js) caches the app shell and
// hashed build assets so downloaded chapters open with no connection at all.
// It is never registered in dev or inside preview iframes.

function unsafeHost(): boolean {
  try {
    if (window.self !== window.top) return true;
  } catch {
    return true;
  }
  const h = window.location.hostname;
  return h === "localhost" || h === "127.0.0.1" || h.includes("lovableproject") || h.includes("id-preview--");
}

export async function registerPWA() {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) return;
  if (import.meta.env.DEV || unsafeHost()) return;
  try {
    await navigator.serviceWorker.register("/sw.js", { scope: "/" });
  } catch (e) {
    console.warn("[pwa] register failed", e);
  }
}
