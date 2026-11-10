/**
 * Source runtime manager.
 *
 * Each installed source is an Aidoku WASM package executed by
 * @nemu.pm/aidoku-runtime (MIT) inside its own Web Worker. Workers are created
 * lazily and evicted LRU so a phone never holds more than a few at once.
 * Every call is time-boxed, isolated (one source failing never affects
 * another) and recorded for health monitoring.
 */
import type { AsyncAidokuSource } from "@nemu.pm/aidoku-runtime";
import {
  getEngineState,
  readSourcePackage,
  readSourceSettings,
  recordHealth,
  writeSourceSettings,
  bootEngine,
} from "./store";
import type { Chapter, ChapterRow, FilterValue, Listing, Manga, Page, SeriesCard, SeriesDetail } from "./types";

const MAX_LIVE = 4;
const CALL_TIMEOUT_MS = 35_000;

type Live = { source: AsyncAidokuSource; usedAt: number };
const live = new Map<string, Live>();
const loading = new Map<string, Promise<AsyncAidokuSource>>();

export class SourceUnavailableError extends Error {}

function relayBase(): string {
  return `${window.location.origin}/api/relay?url=`;
}

function evict() {
  if (live.size <= MAX_LIVE) return;
  const oldest = [...live.entries()].sort((a, b) => a[1].usedAt - b[1].usedAt)[0];
  if (oldest) {
    try {
      oldest[1].source.dispose();
    } catch {
      /* already gone */
    }
    live.delete(oldest[0]);
  }
}

if (typeof window !== "undefined") {
  window.addEventListener("kt:source-updated", (e) => {
    const id = (e as CustomEvent<{ id: string }>).detail?.id;
    const l = id ? live.get(id) : undefined;
    if (l && id) {
      try {
        l.source.dispose();
      } catch {
        /* ignore */
      }
      live.delete(id);
    }
  });
}

export async function getSource(id: string): Promise<AsyncAidokuSource> {
  if (typeof window === "undefined") throw new SourceUnavailableError("Sources run in the browser only");
  const hit = live.get(id);
  if (hit) {
    hit.usedAt = Date.now();
    return hit.source;
  }
  const pending = loading.get(id);
  if (pending) return pending;

  const p = (async () => {
    await bootEngine();
    const meta = getEngineState().installed.find((s) => s.id === id);
    if (!meta) throw new SourceUnavailableError(`Source "${id}" is not installed`);
    const bytes = await readSourcePackage(id);
    if (!bytes) throw new SourceUnavailableError(`Package for "${meta.name}" is missing — reinstall it`);
    let values = await readSourceSettings(id);
    const { loadSource } = await import("@nemu.pm/aidoku-runtime");
    const source = await loadSource(bytes, id, {
      proxyUrl: relayBase(),
      settings: {
        get: () => values,
        set: (key, value) => {
          values = { ...values, [key]: value };
          void writeSourceSettings(id, values);
        },
      },
    });
    live.set(id, { source, usedAt: Date.now() });
    evict();
    return source;
  })();
  loading.set(id, p);
  try {
    return await p;
  } finally {
    loading.delete(id);
  }
}

function timeout<T>(p: Promise<T>, ms: number, label: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const t = setTimeout(() => reject(new Error(`${label} timed out`)), ms);
    p.then(
      (v) => {
        clearTimeout(t);
        resolve(v);
      },
      (e) => {
        clearTimeout(t);
        reject(e);
      },
    );
  });
}

function messageOf(e: unknown): string {
  if (e instanceof Error) return e.message || e.name;
  return String(e);
}

/** Runs one source call with timeout + health accounting. */
async function call<T>(id: string, label: string, fn: (s: AsyncAidokuSource) => Promise<T>): Promise<T> {
  const started = performance.now();
  try {
    const source = await getSource(id);
    const out = await timeout(fn(source), CALL_TIMEOUT_MS, label);
    recordHealth(id, true, Math.round(performance.now() - started));
    return out;
  } catch (e) {
    const msg = messageOf(e);
    if (!(e instanceof SourceUnavailableError)) recordHealth(id, false, Math.round(performance.now() - started), msg);
    throw e instanceof Error ? e : new Error(msg);
  }
}

/* ----------------------------- normalisation ----------------------------- */

function rating(m: Manga): 0 | 1 | 2 {
  const r = Number(m.contentRating ?? m.nsfw ?? 0);
  return r === 2 ? 2 : r === 1 ? 1 : 0;
}

export function toCard(sourceId: string, m: Manga): SeriesCard {
  return {
    sourceId,
    key: m.key,
    title: decodeEntities((m.title ?? "").trim()) || "Untitled",
    cover: m.cover ?? null,
    authors: m.authors ?? [],
    tags: m.tags ?? [],
    status: typeof m.status === "number" ? m.status : null,
    contentRating: rating(m),
  };
}

export function toDetail(sourceId: string, m: Manga): SeriesDetail {
  return {
    ...toCard(sourceId, m),
    description: decodeEntities((m.description ?? "").trim()),
    artists: m.artists ?? [],
    url: m.url ?? null,
    viewer: typeof m.viewer === "number" ? m.viewer : null,
  };
}

export function toChapterRow(c: Chapter): ChapterRow {
  return {
    key: c.key,
    title: decodeEntities(c.title?.trim() ?? "") || null,
    number: typeof c.chapterNumber === "number" && c.chapterNumber >= 0 ? c.chapterNumber : null,
    volume: typeof c.volumeNumber === "number" && c.volumeNumber >= 0 ? c.volumeNumber : null,
    scanlator: c.scanlator?.trim() || null,
    date: typeof c.dateUploaded === "number" && c.dateUploaded > 0 ? c.dateUploaded : null,
    lang: c.lang ?? null,
    url: c.url ?? null,
    locked: !!c.locked,
    raw: c,
  };
}

/* ------------------------------- public API ------------------------------ */

export type MangaPage = { items: SeriesCard[]; hasNextPage: boolean };

const minimal = (key: string): Manga => ({ key });

export async function listListings(id: string): Promise<Listing[]> {
  return call(id, "listings", (s) => s.getListings());
}

export async function browseListing(id: string, listing: Listing, page = 1): Promise<MangaPage> {
  const r = await call(id, "listing", (s) => s.getMangaListForListing(listing, page));
  return { items: r.entries.map((m) => toCard(id, m)), hasNextPage: r.hasNextPage };
}

export async function search(id: string, query: string | null, page = 1, filters: FilterValue[] = []): Promise<MangaPage> {
  const r = await call(id, "search", (s) => s.getSearchMangaList(query, page, filters));
  return { items: r.entries.map((m) => toCard(id, m)), hasNextPage: r.hasNextPage };
}

/**
 * "Popular" for a source: first listing when it has listings (Mihon's
 * popular tab), otherwise an empty search (Aidoku's onlySearch sources).
 */
export async function popular(id: string, page = 1): Promise<MangaPage & { listing: Listing | null }> {
  const listings = await listListings(id).catch(() => [] as Listing[]);
  if (listings.length > 0) {
    const preferred =
      listings.find((l) => /popular|rank|trend|hot/i.test(`${l.id} ${l.name ?? ""}`)) ?? listings[0]!;
    try {
      return { ...(await browseListing(id, preferred, page)), listing: preferred };
    } catch {
      /* fall through to search */
    }
  }
  return { ...(await search(id, null, page)), listing: null };
}

export async function mangaDetails(id: string, key: string): Promise<SeriesDetail> {
  const m = await call(id, "details", (s) => s.getMangaDetails(minimal(key)));
  return toDetail(id, { ...m, key: m.key || key });
}

export async function chapterList(id: string, key: string): Promise<ChapterRow[]> {
  const list = await call(id, "chapters", (s) => s.getChapterList(minimal(key)));
  return list.map(toChapterRow);
}

export async function pageList(id: string, mangaKey: string, chapter: Chapter): Promise<Page[]> {
  return call(id, "pages", (s) => s.getPageList(minimal(mangaKey), chapter));
}

/* -------------------------------- images --------------------------------- */

export function relayUrl(url: string, headers?: Record<string, string>): string {
  let out = `/api/relay?url=${encodeURIComponent(url)}`;
  const h = headers && Object.keys(headers).length ? headers : null;
  if (h) {
    const json = new TextEncoder().encode(JSON.stringify(h));
    let bin = "";
    json.forEach((b) => (bin += String.fromCharCode(b)));
    out += `&h=${btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")}`;
  }
  return out;
}

const processorCache = new Map<string, Promise<boolean>>();

async function hasProcessor(id: string): Promise<boolean> {
  let p = processorCache.get(id);
  if (!p) {
    p = getSource(id)
      .then((s) => s.hasImageProcessor())
      .catch(() => false);
    processorCache.set(id, p);
  }
  return p;
}

export type ResolvedImage =
  | { kind: "url"; src: string; fallback: string | null; revoke?: undefined }
  | { kind: "blob"; src: string; fallback: null; revoke: () => void }
  | { kind: "text"; text: string };

/**
 * Resolves a page into something an <img> can show. Sources that scramble or
 * encrypt pages (e.g. MANGA Plus) run their own image processor; everything
 * else loads directly, falling back to the relay with the source's headers.
 */
export async function resolvePage(id: string, page: Page): Promise<ResolvedImage> {
  if (page.text) return { kind: "text", text: page.text };
  if (page.base64) return { kind: "url", src: `data:image/jpeg;base64,${page.base64}`, fallback: null };
  if (!page.url) throw new Error("Page has no image");

  const source = await getSource(id);
  const req = await source.modifyImageRequest(page.url, page.context ?? null).catch(() => ({
    url: page.url!,
    headers: {} as Record<string, string>,
  }));
  const headers = req.headers ?? {};

  if (await hasProcessor(id)) {
    const res = await fetch(relayUrl(req.url, headers));
    if (!res.ok) throw new Error(`Image ${res.status}`);
    const bytes = new Uint8Array(await res.arrayBuffer());
    const resHeaders: Record<string, string> = {};
    res.headers.forEach((v, k) => (resHeaders[k] = v));
    const processed = await source
      .processPageImage(bytes, page.context ?? null, req.url, headers, res.status, resHeaders)
      .catch(() => null);
    const blob = new Blob([(processed ?? bytes) as BlobPart], {
      type: processed ? "image/png" : (res.headers.get("content-type") ?? "image/jpeg"),
    });
    const src = URL.createObjectURL(blob);
    return { kind: "blob", src, fallback: null, revoke: () => URL.revokeObjectURL(src) };
  }

  const needsHeaders = Object.keys(headers).some((k) => !/^user-agent$/i.test(k));
  const proxied = relayUrl(req.url, headers);
  return needsHeaders ? { kind: "url", src: proxied, fallback: null } : { kind: "url", src: req.url, fallback: proxied };
}

/** Cover URLs: direct first (no referrer), relay with the source's base as Referer on failure. */
export function coverFallback(sourceId: string, url: string | null): string | null {
  if (!url) return null;
  const meta = getEngineState().installed.find((s) => s.id === sourceId);
  const headers: Record<string, string> = meta?.baseUrl ? { Referer: `${meta.baseUrl.replace(/\/$/, "")}/` } : {};
  return relayUrl(url, headers);
}

const ENTITIES: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", nbsp: "\u00a0", "#39": "'" };
/** Some sources leave HTML entities in text fields ("&quot;Pirate King&quot;"). */
export function decodeEntities(s: string): string {
  if (!s.includes("&")) return s;
  return s.replace(/&(#x[0-9a-f]+|#\d+|[a-z]+);/gi, (m, e: string) => {
    if (e[0] === "#") {
      const n = e[1] === "x" || e[1] === "X" ? parseInt(e.slice(2), 16) : parseInt(e.slice(1), 10);
      return Number.isFinite(n) && n > 0 && n < 0x110000 ? String.fromCodePoint(n) : m;
    }
    return ENTITIES[e.toLowerCase()] ?? m;
  });
}
