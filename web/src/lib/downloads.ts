/**
 * Offline downloads — Mihon's download queue for the browser. Page images
 * are resolved through the source (including scrambled-image processors),
 * fetched via the same-origin relay and stored as Blobs in IndexedDB, so a
 * downloaded chapter opens with no network at all.
 */
import { createStore, del, get, keys, set, type UseStore } from "idb-keyval";
import { useSyncExternalStore } from "react";
import { pageList, resolvePage } from "@/engine/runtime";
import type { ChapterRow, Page, SeriesCard } from "@/engine/types";
import { seriesId } from "@/engine/ids";
import { chapterLabel } from "./local-library";

export type DownloadMeta = {
  seriesId: string;
  series: SeriesCard;
  chapter: Omit<ChapterRow, "raw">;
  label: string;
  pages: Array<{ text: string | null }>;
  bytes: number;
  at: number;
};

export type QueueItem = {
  id: string;
  series: SeriesCard;
  chapter: ChapterRow;
  label: string;
  done: number;
  total: number;
  status: "queued" | "downloading" | "error";
  error?: string;
};

const OFFLINE_SCHEME = "kt-offline:";
let idb: UseStore | null = null;
const db = () => (idb ??= createStore("kurotoon-downloads", "kv"));
const dlId = (sid: string, chapterKey: string) => `${sid}|${chapterKey}`;
const metaKey = (id: string) => `meta:${id}`;
const pageKey = (id: string, i: number) => `page:${id}|${i}`;

type State = { queue: QueueItem[]; downloaded: Record<string, DownloadMeta>; loaded: boolean };
let state: State = { queue: [], downloaded: {}, loaded: false };
const listeners = new Set<() => void>();
const emit = (patch: Partial<State>) => {
  state = { ...state, ...patch };
  listeners.forEach((l) => l());
};
const SERVER: State = { queue: [], downloaded: {}, loaded: false };

let loading: Promise<void> | null = null;
export function loadDownloads(): Promise<void> {
  if (typeof window === "undefined") return Promise.resolve();
  loading ??= (async () => {
    const all = (await keys(db())).filter((k): k is string => typeof k === "string" && k.startsWith("meta:"));
    const metas = await Promise.all(all.map((k) => get<DownloadMeta>(k, db())));
    const downloaded: Record<string, DownloadMeta> = {};
    for (const m of metas) if (m) downloaded[dlId(m.seriesId, m.chapter.key)] = m;
    emit({ downloaded, loaded: true });
  })().catch(() => emit({ loaded: true }));
  return loading;
}

export function useDownloads(): State {
  return useSyncExternalStore(
    (fn) => {
      listeners.add(fn);
      void loadDownloads();
      return () => listeners.delete(fn);
    },
    () => state,
    () => SERVER,
  );
}

export function isDownloaded(sid: string, chapterKey: string) {
  return !!state.downloaded[dlId(sid, chapterKey)];
}

export async function getOfflineChapter(sid: string, chapterKey: string): Promise<DownloadMeta | null> {
  await loadDownloads();
  return state.downloaded[dlId(sid, chapterKey)] ?? null;
}

/** Page objects for a downloaded chapter; resolve them with resolveOfflinePage. */
export function offlinePages(meta: DownloadMeta): Page[] {
  const id = dlId(meta.seriesId, meta.chapter.key);
  return meta.pages.map((p, i) => (p.text != null ? ({ text: p.text } as Page) : ({ url: `${OFFLINE_SCHEME}${id}|${i}` } as Page)));
}

export function isOfflinePage(page: Page) {
  return typeof page.url === "string" && page.url.startsWith(OFFLINE_SCHEME);
}

export async function resolveOfflinePage(page: Page): Promise<{ kind: "blob"; src: string; fallback: null; revoke: () => void }> {
  const key = page.url!.slice(OFFLINE_SCHEME.length);
  const blob = await get<Blob>(`page:${key}`, db());
  if (!blob) throw new Error("Downloaded page missing");
  const src = URL.createObjectURL(blob);
  return { kind: "blob", src, fallback: null, revoke: () => URL.revokeObjectURL(src) };
}

export function enqueueDownloads(series: SeriesCard, chapters: ChapterRow[]) {
  const sid = seriesId(series.sourceId, series.key);
  const add: QueueItem[] = [];
  for (const c of chapters) {
    const id = dlId(sid, c.key);
    if (c.locked || state.downloaded[id] || state.queue.some((q) => q.id === id)) continue;
    add.push({ id, series, chapter: c, label: chapterLabel(c), done: 0, total: 0, status: "queued" });
  }
  if (!add.length) return 0;
  emit({ queue: [...state.queue, ...add] });
  void pump();
  return add.length;
}

export function cancelDownload(id: string) {
  emit({ queue: state.queue.filter((q) => q.id !== id || q.status === "downloading") });
  cancelled.add(id);
}

export function clearFailed() {
  emit({ queue: state.queue.filter((q) => q.status !== "error") });
}

export function retryFailed() {
  emit({ queue: state.queue.map((q) => (q.status === "error" ? { ...q, status: "queued", error: undefined } : q)) });
  void pump();
}

export async function deleteDownload(sid: string, chapterKey: string) {
  const id = dlId(sid, chapterKey);
  const meta = state.downloaded[id];
  if (!meta) return;
  await Promise.all(meta.pages.map((_, i) => del(pageKey(id, i), db())));
  await del(metaKey(id), db());
  const downloaded = { ...state.downloaded };
  delete downloaded[id];
  emit({ downloaded });
}

export async function deleteSeriesDownloads(sid: string) {
  for (const m of Object.values(state.downloaded)) if (m.seriesId === sid) await deleteDownload(sid, m.chapter.key);
}

const cancelled = new Set<string>();
let pumping = false;
const update = (id: string, patch: Partial<QueueItem>) =>
  emit({ queue: state.queue.map((q) => (q.id === id ? { ...q, ...patch } : q)) });

async function fetchPageBlob(sourceId: string, page: Page): Promise<Blob | string> {
  const r = await resolvePage(sourceId, page);
  if (r.kind === "text") return r.text;
  if (r.kind === "blob") {
    const b = await (await fetch(r.src)).blob();
    r.revoke();
    return b;
  }
  // Always use the same-origin relay copy when there is one (no CORS issues).
  for (const u of [r.fallback, r.src]) {
    if (!u) continue;
    try {
      const res = await fetch(u);
      if (res.ok) return await res.blob();
    } catch {
      /* try next */
    }
  }
  throw new Error("Image download failed");
}

async function pump() {
  if (pumping) return;
  pumping = true;
  try {
    for (let item = state.queue.find((q) => q.status === "queued"); item; item = state.queue.find((q) => q.status === "queued")) {
      const { id, series, chapter } = item;
      cancelled.delete(id);
      update(id, { status: "downloading" });
      try {
        const pages = await pageList(series.sourceId, series.key, chapter.raw);
        update(id, { total: pages.length });
        const out: Array<{ text: string | null }> = new Array(pages.length);
        let bytes = 0;
        let next = 0;
        const worker = async () => {
          while (next < pages.length) {
            if (cancelled.has(id)) throw new Error("Cancelled");
            const i = next++;
            const r = await fetchPageBlob(series.sourceId, pages[i]!);
            if (typeof r === "string") out[i] = { text: r };
            else {
              await set(pageKey(id, i), r, db());
              bytes += r.size;
              out[i] = { text: null };
            }
            update(id, { done: (state.queue.find((q) => q.id === id)?.done ?? 0) + 1 });
          }
        };
        await Promise.all([worker(), worker(), worker()]);
        const { raw: _raw, ...plain } = chapter;
        const meta: DownloadMeta = {
          seriesId: seriesId(series.sourceId, series.key),
          series,
          chapter: JSON.parse(JSON.stringify(plain)),
          label: item.label,
          pages: out,
          bytes,
          at: Date.now(),
        };
        await set(metaKey(id), meta, db());
        emit({ downloaded: { ...state.downloaded, [id]: meta }, queue: state.queue.filter((q) => q.id !== id) });
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e);
        if (msg === "Cancelled") emit({ queue: state.queue.filter((q) => q.id !== id) });
        else update(id, { status: "error", error: msg });
      }
    }
  } finally {
    pumping = false;
  }
}

export function formatBytes(n: number) {
  if (n < 1024) return `${n} B`;
  if (n < 1024 ** 2) return `${(n / 1024).toFixed(0)} KB`;
  if (n < 1024 ** 3) return `${(n / 1024 ** 2).toFixed(1)} MB`;
  return `${(n / 1024 ** 3).toFixed(2)} GB`;
}
