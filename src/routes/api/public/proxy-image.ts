import { createFileRoute } from "@tanstack/react-router";

const ALLOWED_HOST_SUFFIXES = [
  ".mangadex.network",
  ".mangadex.org",
  "mangadex.org",
  "uploads.mangadex.org",
];

function isAllowed(url: URL): boolean {
  const h = url.hostname.toLowerCase();
  return ALLOWED_HOST_SUFFIXES.some((s) => h === s || h.endsWith(s));
}

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

export const Route = createFileRoute("/api/public/proxy-image")({
  server: {
    handlers: {
      OPTIONS: async () => new Response(null, { status: 204, headers: CORS }),
      GET: async ({ request }) => {
        const u = new URL(request.url);
        const target = u.searchParams.get("url");
        if (!target) {
          return new Response("missing url", { status: 400, headers: CORS });
        }
        let parsed: URL;
        try {
          parsed = new URL(target);
        } catch {
          return new Response("bad url", { status: 400, headers: CORS });
        }
        if (parsed.protocol !== "https:" || !isAllowed(parsed)) {
          return new Response("forbidden host", { status: 403, headers: CORS });
        }
        try {
          const upstream = await fetch(parsed.toString(), {
            headers: {
              // MangaDex's uploads host returns 400 for browser-style UAs
              // (Mozilla/...). A short, non-browser UA passes through.
              "User-Agent": "KurotoonProxy/1.0",
              Accept: "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
            },
          });
          if (!upstream.ok || !upstream.body) {
            return new Response(`upstream ${upstream.status}`, {
              status: 502,
              headers: CORS,
            });
          }
          const headers = new Headers(CORS);
          headers.set(
            "Content-Type",
            upstream.headers.get("content-type") ?? "image/jpeg",
          );
          const len = upstream.headers.get("content-length");
          if (len) headers.set("Content-Length", len);
          headers.set("Cache-Control", "public, max-age=86400, immutable");
          return new Response(upstream.body, { status: 200, headers });
        } catch (e) {
          return new Response(`proxy error: ${(e as Error).message}`, {
            status: 502,
            headers: CORS,
          });
        }
      },
    },
  },
});
