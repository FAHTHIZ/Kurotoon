/**
 * Library updates — Mihon's "Updates" tab. For every series in the library we
 * fetch the chapter list from its source, compare it with the chapter keys we
 * have already seen, and record anything new. Runs on the device only.
 */
import { useSyncExternalStore } from "react";
import { chapterList } from "@/engine/runtime";
import { getEngineState } from "@/engine/store";
import type { SeriesCard } from "@/engine/types";
import { chapterLabel, getBookmarks, idOf } from "./local-library";

export type UpdateEntry = {
  seriesId: string;
  series: SeriesCard;
  chapterKey: string;
  label: string;
  title: string | null;
  date: number | null;
  foundAt: number;
};

export type UpdateRun = {
  running: boolean;
  done: number;
  total: number;
  lastRunAt: number | null;
  errors: Array<{ seriesId: string; title: string; error: string }>;
};

const KNOWN = "kt.v2.known-chapters";
const FEED = "kt.v2.updates";
const LAST = "kt.v2.updates-last";
const MAX_FEED = 500;
const CONCURRENCY = 3;

function read<T>(k: string, fb: T): T {
  try {
    const v = localStorage.getItem(k);
    return v ? (JSON.parse(v) as T) : fb;
  } catch {
    return fb;
  }
}
function write(k: string, v: unknown) {
  try {
    localStorage.setItem(k, JSON.stringify(v));
  } catch {
    /* full */
  }
}

let run: UpdateRun = { running: false, done: 0, total: 0, lastRunAt: null, errors: [] };
let feedCache: UpdateEntry[] | null = null;
const listeners = new Set<() => void>();
const emit = () => listeners.forEach((l) => l());

export function getUpdates(): UpdateEntry[] {
  if (feedCache) return feedCache;
  feedCache = typeof window === "undefined" ? [] : read<UpdateEntry[]>(FEED, []);
  return feedCache;
}

function setFeed(f: UpdateEntry[]) {
  feedCache = f.slice(0, MAX_FEED);
  write(FEED, feedCache);
  emit();
}

export function clearUpdates() {
  setFeed([]);
}

const subscribe = (fn: () => void) => {
  listeners.add(fn);
  return () => listeners.delete(fn);
};

export function useUpdates() {
  const feed = useSyncExternalStore(subscribe, getUpdates, () => EMPTY_FEED);
  const status = useSyncExternalStore(subscribe, () => run, () => IDLE);
  return { feed, status };
}
const EMPTY_FEED: UpdateEntry[] = [];
const IDLE: UpdateRun = { running: false, done: 0, total: 0, lastRunAt: null, errors: [] };

/**
 * Pure diff used by the checker (exported for tests). The first time a series
 * is checked every chapter is just remembered — nothing is reported as new,
 * like Mihon when a series is added to the library.
 */
export function diffChapters(known: string[] | undefined, current: string[]): { fresh: string[]; known: string[] } {
  if (!known) return { fresh: [], known: current };
  const k = new Set(known);
  const fresh = current.filter((c) => !k.has(c));
  return { fresh, known: [...new Set([...known, ...current])].slice(-3000) };
}

/** Remember the current chapters of a series without reporting them. */
export function seedKnownChapters(seriesId: string, keys: string[]) {
  const known = read<Record<string, string[]>>(KNOWN, {});
  if (known[seriesId]) return;
  known[seriesId] = keys;
  write(KNOWN, known);
}

export async function checkLibraryUpdates(): Promise<number> {
  if (run.running) return 0;
  const installed = new Set(getEngineState().installed.filter((s) => s.enabled).map((s) => s.id));
  const library = getBookmarks().filter((s) => installed.has(s.sourceId));
  run = { running: true, done: 0, total: library.length, lastRunAt: run.lastRunAt, errors: [] };
  emit();
  const known = read<Record<string, string[]>>(KNOWN, {});
  const found: UpdateEntry[] = [];
  const queue = [...library];
  const worker = async () => {
    for (let s = queue.shift(); s; s = queue.shift()) {
      const id = idOf(s);
      try {
        const chapters = await chapterList(s.sourceId, s.key);
        const d = diffChapters(known[id], chapters.map((c) => c.key));
        known[id] = d.known;
        const fresh = new Set(d.fresh);
        for (const c of chapters) {
          if (!fresh.has(c.key)) continue;
          found.push({ seriesId: id, series: s, chapterKey: c.key, label: chapterLabel(c), title: c.title, date: c.date, foundAt: Date.now() });
        }
      } catch (e) {
        run = { ...run, errors: [...run.errors, { seriesId: id, title: s.title, error: e instanceof Error ? e.message : String(e) }] };
      }
      run = { ...run, done: run.done + 1 };
      emit();
    }
  };
  await Promise.all(Array.from({ length: CONCURRENCY }, worker));
  write(KNOWN, known);
  const now = Date.now();
  write(LAST, now);
  run = { ...run, running: false, lastRunAt: now };
  if (found.length) setFeed([...found, ...getUpdates()]);
  else emit();
  return found.length;
}

export function lastUpdateRun(): number | null {
  return run.lastRunAt ?? (typeof window === "undefined" ? null : read<number | null>(LAST, null));
}
