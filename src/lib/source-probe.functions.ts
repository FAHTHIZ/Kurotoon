import { createServerFn } from "@tanstack/react-start";

/**
 * Real source-capability engine.
 *
 * A repository can list any number of sources. This probes each one over the
 * network and reports what KuroToon can *actually* do with it — nothing is
 * assumed from the manifest. Every capability below is verified by a live
 * request, so "Readable" means the endpoint answered with usable data.
 */

export type SourceCapability = "search" | "chapters" | "pages";

export type SourceStatus =
  | "ready" // built-in adapter + endpoint verified
  | "compatible" // unknown source, but exposes a MangaDex-compatible API that answered
  | "online" // host reachable, no API KuroToon can read
  | "offline" // host did not answer
  | "blocked" // refused for safety (non-https / private host)
  | "unknown"; // manifest gave no base URL to probe

export type SourceProbe = {
  id: string;
  code: string;
  status: SourceStatus;
  latencyMs: number | null;
  httpStatus: number | null;
  api: string | null;
  capabilities: SourceCapability[];
  detail: string;
};

const BLOCKED_HOST =
  /^(localhost$|127\.|0\.|10\.|192\.168\.|169\.254\.|172\.(1[6-9]|2\d|3[01])\.|\[?::1\]?$)/i;

const MANGADEX_API = "https://api.mangadex.org";
const MANGADEX_CODES = new Set(["mangadex", "md", "mangadex-en", "mangadex.org"]);
const UA = "KuroToon/1.0 (+source-probe)";
const TIMEOUT_MS = 9_000;

type Input = {
  sources: Array<{ id: string; code: string; baseUrl?: string | null }>;
};

async function timedFetch(url: string, accept: string) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  const started = Date.now();
  try {
    const res = await fetch(url, {
      headers: { accept, "user-agent": UA },
      signal: controller.signal,
      redirect: "follow",
    });
    return { res, latencyMs: Date.now() - started };
  } finally {
    clearTimeout(timer);
  }
}

/** True when the body looks like a MangaDex-style `{ result, data: [...] }` list. */
function isMangadexList(json: unknown): boolean {
  const o = json as { result?: unknown; data?: unknown } | null;
  return !!o && Array.isArray(o.data) && (o.result === "ok" || o.result === undefined);
}

async function probeMangadexApi(
  api: string,
): Promise<{ ok: boolean; latencyMs: number; httpStatus: number | null; caps: SourceCapability[] }> {
  const caps: SourceCapability[] = [];
  let latencyMs = 0;
  let httpStatus: number | null = null;

  // 1. Search / listing
  let firstId: string | null = null;
  try {
    const { res, latencyMs: ms } = await timedFetch(
      `${api}/manga?limit=1&contentRating[]=safe&contentRating[]=suggestive`,
      "application/json",
    );
    latencyMs = ms;
    httpStatus = res.status;
    if (!res.ok) return { ok: false, latencyMs, httpStatus, caps };
    const json = (await res.json()) as { data?: Array<{ id?: string }> };
    if (!isMangadexList(json)) return { ok: false, latencyMs, httpStatus, caps };
    caps.push("search");
    firstId = json.data?.[0]?.id ?? null;
  } catch {
    return { ok: false, latencyMs, httpStatus, caps };
  }

  // 2. Chapter feed for that title
  let chapterId: string | null = null;
  if (firstId) {
    try {
      const { res } = await timedFetch(
        `${api}/manga/${firstId}/feed?limit=1&translatedLanguage[]=en&order[chapter]=asc`,
        "application/json",
      );
      if (res.ok) {
        const json = (await res.json()) as { data?: Array<{ id?: string }> };
        if (isMangadexList(json)) {
          caps.push("chapters");
          chapterId = json.data?.[0]?.id ?? null;
        }
      }
    } catch {
      /* capability simply not confirmed */
    }
  }

  // 3. Page delivery for that chapter
  if (chapterId) {
    try {
      const { res } = await timedFetch(`${api}/at-home/server/${chapterId}`, "application/json");
      if (res.ok) {
        const json = (await res.json()) as { chapter?: { data?: unknown[] } };
        if (Array.isArray(json.chapter?.data) && json.chapter.data.length > 0) caps.push("pages");
      }
    } catch {
      /* capability simply not confirmed */
    }
  }

  return { ok: true, latencyMs, httpStatus, caps };
}

async function probeReachable(url: string): Promise<{ latencyMs: number; httpStatus: number } | null> {
  try {
    const { res, latencyMs } = await timedFetch(url, "text/html,application/json");
    return { latencyMs, httpStatus: res.status };
  } catch {
    return null;
  }
}

async function probeOne(src: Input["sources"][number]): Promise<SourceProbe> {
  const base: Omit<SourceProbe, "status" | "detail"> = {
    id: src.id,
    code: src.code,
    latencyMs: null,
    httpStatus: null,
    api: null,
    capabilities: [],
  };

  const isMangadex =
    MANGADEX_CODES.has(src.code.toLowerCase()) ||
    (src.baseUrl ?? "").toLowerCase().includes("mangadex.org");

  if (isMangadex) {
    const r = await probeMangadexApi(MANGADEX_API);
    if (r.ok) {
      return {
        ...base,
        api: MANGADEX_API,
        latencyMs: r.latencyMs,
        httpStatus: r.httpStatus,
        capabilities: r.caps,
        status: "ready",
        detail: `Built-in adapter verified · ${r.caps.length}/3 capabilities live`,
      };
    }
    return {
      ...base,
      api: MANGADEX_API,
      latencyMs: r.latencyMs,
      httpStatus: r.httpStatus,
      status: "offline",
      detail: r.httpStatus ? `Source API answered ${r.httpStatus}` : "Source API did not answer",
    };
  }

  const raw = (src.baseUrl ?? "").trim();
  if (!raw) {
    return { ...base, status: "unknown", detail: "Repository listed no base URL to test" };
  }

  let u: URL;
  try {
    u = new URL(raw);
  } catch {
    return { ...base, status: "blocked", detail: "Base URL is not a valid address" };
  }
  if (u.protocol !== "https:") {
    return { ...base, status: "blocked", detail: "Only https sources are probed" };
  }
  if (BLOCKED_HOST.test(u.hostname)) {
    return { ...base, status: "blocked", detail: "Private or loopback hosts are refused" };
  }

  // Try the source as a MangaDex-compatible API (many indexes ship these).
  const apiBase = u.toString().replace(/\/+$/, "");
  const compat = await probeMangadexApi(apiBase);
  if (compat.ok && compat.caps.includes("search")) {
    return {
      ...base,
      api: apiBase,
      latencyMs: compat.latencyMs,
      httpStatus: compat.httpStatus,
      capabilities: compat.caps,
      status: "compatible",
      detail: `Compatible API verified · ${compat.caps.join(", ")}`,
    };
  }

  // Otherwise just report honest reachability.
  const reach = await probeReachable(u.toString());
  if (!reach) {
    return { ...base, status: "offline", detail: "Host did not respond within 9s" };
  }
  return {
    ...base,
    latencyMs: reach.latencyMs,
    httpStatus: reach.httpStatus,
    status: reach.httpStatus < 400 ? "online" : "offline",
    detail:
      reach.httpStatus < 400
        ? "Host is up, but exposes no API KuroToon can read"
        : `Host answered ${reach.httpStatus}`,
  };
}

/** Runs probes with bounded concurrency so a big repo can't stall the request. */
async function mapLimit<T, R>(items: T[], limit: number, fn: (item: T) => Promise<R>): Promise<R[]> {
  const out: R[] = new Array(items.length);
  let cursor = 0;
  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (cursor < items.length) {
      const i = cursor++;
      out[i] = await fn(items[i]!);
    }
  });
  await Promise.all(workers);
  return out;
}

/**
 * Public by design: it only performs outbound reachability checks against
 * user-supplied public https hosts and returns no account data.
 */
export const probeSources = createServerFn({ method: "POST" })
  .inputValidator((data: Input) => {
    const list = Array.isArray(data?.sources) ? data.sources.slice(0, 60) : [];
    return {
      sources: list
        .filter((s) => s && typeof s.id === "string" && typeof s.code === "string")
        .map((s) => ({
          id: s.id.slice(0, 100),
          code: s.code.slice(0, 80),
          baseUrl: typeof s.baseUrl === "string" ? s.baseUrl.slice(0, 300) : null,
        })),
    };
  })
  .handler(async ({ data }): Promise<{ probes: SourceProbe[]; scannedAt: string }> => {
    const probes = await mapLimit(data.sources, 6, probeOne);
    return { probes, scannedAt: new Date().toISOString() };
  });
