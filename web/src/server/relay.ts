/**
 * Network relay for on-device extensions.
 *
 * Sources run inside the reader's browser (a Web Worker executing Aidoku WASM
 * packages). Browsers block cross-origin reads and forbid headers such as
 * Referer / User-Agent / Cookie, so the worker sends every request here:
 *
 *   GET|POST /api/relay?url=<encoded target>
 *     x-proxy-<header>: <value>   → forwarded upstream as <header>
 *     h=<base64url JSON>          → same, for plain <img src> usage
 *
 * The relay is deliberately dumb: it never parses content, stores nothing and
 * keeps no logs. It only enforces safety rules (SSRF, size, methods, origin).
 */

const PRIVATE_HOST =
  /^(localhost|.*\.localhost|.*\.local|.*\.internal|0\.0\.0\.0|127\.\d+\.\d+\.\d+|10\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+|169\.254\.\d+\.\d+|172\.(1[6-9]|2\d|3[01])\.\d+\.\d+|100\.(6[4-9]|[7-9]\d|1[01]\d|12[0-7])\.\d+\.\d+|\[?::1?\]?|\[?f[cd][0-9a-f]{2}:.*|\[?fe80:.*)$/i;

/** Headers a client may never set upstream, whatever it asks for. */
const DROP_REQUEST = new Set([
  "host",
  "content-length",
  "connection",
  "keep-alive",
  "transfer-encoding",
  "upgrade",
  "te",
  "trailer",
  "proxy-authorization",
  "x-forwarded-for",
  "x-real-ip",
  "cf-connecting-ip",
  "forwarded",
]);

/** Upstream response headers we never pass back. */
const DROP_RESPONSE = new Set([
  "content-encoding",
  "content-length",
  "transfer-encoding",
  "connection",
  "keep-alive",
  "set-cookie",
  "strict-transport-security",
  "content-security-policy",
  "content-security-policy-report-only",
  "alt-svc",
  "report-to",
  "nel",
]);

const DEFAULT_UA =
  "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36";

const MAX_BODY_BYTES = 25 * 1024 * 1024;
const TIMEOUT_MS = 25_000;
const MAX_REDIRECTS = 5;

export type RelayError = { status: number; message: string };

export function validateTarget(raw: string | null): URL | RelayError {
  if (!raw) return { status: 400, message: "missing url" };
  let u: URL;
  try {
    u = new URL(raw);
  } catch {
    return { status: 400, message: "bad url" };
  }
  if (u.protocol !== "https:" && u.protocol !== "http:") return { status: 400, message: "unsupported scheme" };
  if (u.username || u.password) return { status: 400, message: "credentials in url" };
  const host = u.hostname.replace(/^\[|\]$/g, "");
  if (!host || PRIVATE_HOST.test(host) || PRIVATE_HOST.test(u.hostname)) {
    return { status: 403, message: "blocked host" };
  }
  if (u.port && !["80", "443", "8080", "8443"].includes(u.port)) {
    return { status: 403, message: "blocked port" };
  }
  return u;
}

function decodeHeaderParam(h: string | null): Record<string, string> {
  if (!h) return {};
  try {
    const b64 = h.replace(/-/g, "+").replace(/_/g, "/");
    const json = new TextDecoder().decode(Uint8Array.from(atob(b64), (c) => c.charCodeAt(0)));
    const parsed = JSON.parse(json) as unknown;
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return {};
    const out: Record<string, string> = {};
    for (const [k, v] of Object.entries(parsed as Record<string, unknown>)) {
      if (typeof v === "string") out[k] = v;
    }
    return out;
  } catch {
    return {};
  }
}

export function upstreamHeaders(request: Request, url: URL): Headers {
  const out = new Headers();
  const fromParam = decodeHeaderParam(url.searchParams.get("h"));
  for (const [k, v] of Object.entries(fromParam)) {
    if (!DROP_REQUEST.has(k.toLowerCase())) out.set(k, v);
  }
  request.headers.forEach((value, key) => {
    const k = key.toLowerCase();
    if (!k.startsWith("x-proxy-")) return;
    const name = k.slice("x-proxy-".length);
    if (!name || DROP_REQUEST.has(name)) return;
    out.set(name, value);
  });
  if (!out.has("user-agent")) out.set("user-agent", DEFAULT_UA);
  if (!out.has("accept")) out.set("accept", request.headers.get("accept") ?? "*/*");
  if (!out.has("accept-language")) out.set("accept-language", "en-US,en;q=0.9");
  const ct = request.headers.get("content-type");
  if (ct && !out.has("content-type")) out.set("content-type", ct);
  return out;
}

function errorResponse(status: number, message: string): Response {
  return new Response(message, {
    status,
    headers: { "content-type": "text/plain; charset=utf-8", "cache-control": "no-store" },
  });
}

/** Rejects cross-site callers so the relay cannot be hot-linked as an open proxy. */
export function isAllowedCaller(request: Request): boolean {
  const site = request.headers.get("sec-fetch-site");
  if (site && site !== "same-origin" && site !== "none") return false;
  const origin = request.headers.get("origin");
  if (origin) {
    try {
      if (new URL(origin).host !== new URL(request.url).host) return false;
    } catch {
      return false;
    }
  }
  return true;
}

export async function relay(request: Request): Promise<Response> {
  const self = new URL(request.url);
  if (!isAllowedCaller(request)) return errorResponse(403, "cross-site relay use is not allowed");
  if (!["GET", "POST", "HEAD"].includes(request.method)) return errorResponse(405, "method not allowed");

  let target = validateTarget(self.searchParams.get("url"));
  if (!(target instanceof URL)) return errorResponse(target.status, target.message);

  const headers = upstreamHeaders(request, self);
  const body = request.method === "POST" ? await request.arrayBuffer() : undefined;
  if (body && body.byteLength > 2 * 1024 * 1024) return errorResponse(413, "request body too large");

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  let upstream: Response | null = null;
  try {
    // Follow redirects manually so every hop is re-validated against SSRF rules.
    let method = request.method;
    let payload: ArrayBuffer | undefined = body;
    for (let hop = 0; hop <= MAX_REDIRECTS; hop++) {
      upstream = await fetch(target.toString(), {
        method,
        headers,
        body: payload,
        redirect: "manual",
        signal: controller.signal,
      });
      if (upstream.status < 300 || upstream.status >= 400 || !upstream.headers.get("location")) break;
      const next = validateTarget(new URL(upstream.headers.get("location")!, target).toString());
      if (!(next instanceof URL)) return errorResponse(next.status, next.message);
      if (upstream.status === 303 || ((upstream.status === 301 || upstream.status === 302) && method === "POST")) {
        method = "GET";
        payload = undefined;
        headers.delete("content-type");
      }
      target = next;
      if (hop === MAX_REDIRECTS) return errorResponse(508, "too many redirects");
    }
  } catch (e) {
    clearTimeout(timer);
    const aborted = e instanceof Error && e.name === "AbortError";
    return errorResponse(aborted ? 504 : 502, aborted ? "upstream timeout" : "upstream unreachable");
  }
  clearTimeout(timer);
  if (!upstream) return errorResponse(502, "no response");

  const declared = Number(upstream.headers.get("content-length") ?? 0);
  if (declared > MAX_BODY_BYTES) return errorResponse(413, "upstream body too large");

  const out = new Headers();
  upstream.headers.forEach((value, key) => {
    if (!DROP_RESPONSE.has(key.toLowerCase())) out.set(key, value);
  });
  // Cookies are surfaced under a readable name; the runtime keeps its own jar.
  const setCookie = upstream.headers.get("set-cookie");
  if (setCookie) out.set("x-proxy-set-cookie", setCookie);
  out.set("x-nemu-final-url", target.toString());
  out.set("x-content-type-options", "nosniff");
  out.set("referrer-policy", "no-referrer");
  const ct = upstream.headers.get("content-type") ?? "";
  if (/^image\//i.test(ct) && upstream.ok) {
    out.set("cache-control", "public, max-age=604800, immutable");
  } else {
    out.set("cache-control", "no-store");
  }
  // Never let relayed HTML/JS execute in our origin.
  if (!/^image\//i.test(ct)) out.set("content-security-policy", "sandbox; default-src 'none'");

  return new Response(request.method === "HEAD" ? null : upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: out,
  });
}
