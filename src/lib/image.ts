// Route MangaDex-hosted images through our same-origin proxy so browsers
// that block hotlinking / cross-origin image loads still render them.
export function proxied(url: string | null | undefined): string {
  if (!url) return "";
  if (url.startsWith("/api/")) return url;
  try {
    const u = new URL(url);
    const h = u.hostname.toLowerCase();
    if (h.endsWith(".mangadex.network") || h.endsWith("mangadex.org")) {
      return `/api/public/proxy-image?url=${encodeURIComponent(url)}`;
    }
  } catch {
    return url;
  }
  return url;
}
