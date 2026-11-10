// Library, reading progress and history live on the device (like Mihon's
// local database). Series are identified by `${sourceId}::${key}`.
import type { SeriesCard } from "@/engine/types";
import { track } from "./rewards";
import { seriesId } from "@/engine/ids";
import { sfx } from "@/lib/fx";

export { seriesId };

const BOOKMARKS = "kt.v2.bookmarks";
const PROGRESS = "kt.v2.progress";
const RECENT_SERIES = "kt.v2.seen";
const HISTORY = "kt.v2.history";
const FAVOURITES = "kt.v2.favourites";
const READ = "kt.v2.read";
const SEARCHES = "kt.searches.v1";

export const idOf = (s: Pick<SeriesCard, "sourceId" | "key">) => seriesId(s.sourceId, s.key);

export type ProgressEntry = {
  chapterKey: string;
  chapterLabel: string;
  /** 1-based page. */
  page: number;
  total: number;
  at: number;
};

export type HistoryEntry = {
  seriesId: string;
  chapterKey: string;
  chapterLabel: string;
  at: number;
};

function read<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function write(key: string, value: unknown) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* storage full / private mode */
  }
  window.dispatchEvent(new CustomEvent("kt-local-change", { detail: { key } }));
}

/** Slim card for storage (drops large arrays). */
function slim(s: SeriesCard): SeriesCard {
  return { ...s, tags: s.tags.slice(0, 6), authors: s.authors.slice(0, 3) };
}

/* ------------------------------- bookmarks ------------------------------- */

export function getBookmarks(): SeriesCard[] {
  return read<SeriesCard[]>(BOOKMARKS, []);
}

export function isBookmarked(id: string): boolean {
  return getBookmarks().some((s) => idOf(s) === id);
}

export function toggleBookmark(series: SeriesCard): boolean {
  const id = idOf(series);
  const list = getBookmarks();
  const exists = list.some((s) => idOf(s) === id);
  write(BOOKMARKS, exists ? list.filter((s) => idOf(s) !== id) : [slim(series), ...list]);
  rememberSeries(series);
  if (!exists) track("bookmark");
  sfx(exists ? "toggleOff" : "toggleOn");
  return !exists;
}

/* ------------------------------- progress -------------------------------- */

export function getProgress(id: string): ProgressEntry | null {
  return read<Record<string, ProgressEntry>>(PROGRESS, {})[id] ?? null;
}

export function setProgress(id: string, entry: ProgressEntry) {
  const all = read<Record<string, ProgressEntry>>(PROGRESS, {});
  all[id] = entry;
  write(PROGRESS, all);
}

/** Chapter keys the user finished, per series (Mihon's "read" flag). */
export function getReadChapters(id: string): string[] {
  return read<Record<string, string[]>>(READ, {})[id] ?? [];
}

export function markChapterRead(id: string, chapterKey: string, readFlag = true) {
  const all = read<Record<string, string[]>>(READ, {});
  const set = new Set(all[id] ?? []);
  if (readFlag) set.add(chapterKey);
  else set.delete(chapterKey);
  all[id] = [...set].slice(-2000);
  write(READ, all);
}

/* ------------------------------ seen series ------------------------------ */

export function rememberSeries(series: SeriesCard) {
  const all = read<Record<string, SeriesCard>>(RECENT_SERIES, {});
  const id = idOf(series);
  delete all[id];
  all[id] = slim(series);
  const entries = Object.entries(all).slice(-200);
  write(RECENT_SERIES, Object.fromEntries(entries));
}

export function getSeen(id: string): SeriesCard | null {
  return read<Record<string, SeriesCard>>(RECENT_SERIES, {})[id] ?? getBookmarks().find((s) => idOf(s) === id) ?? null;
}

/** Series with saved reading progress, most recent first. */
export function getContinueReading(): Array<{ series: SeriesCard; progress: ProgressEntry }> {
  const all = read<Record<string, ProgressEntry>>(PROGRESS, {});
  const seen = read<Record<string, SeriesCard>>(RECENT_SERIES, {});
  const marks = getBookmarks();
  return Object.entries(all)
    .map(([id, progress]) => {
      const series = seen[id] ?? marks.find((s) => idOf(s) === id);
      return series ? { series, progress } : null;
    })
    .filter((x): x is { series: SeriesCard; progress: ProgressEntry } => !!x)
    .sort((a, b) => b.progress.at - a.progress.at)
    .slice(0, 20);
}

/* -------------------------------- history -------------------------------- */

export function getHistory(): Array<HistoryEntry & { series: SeriesCard | null }> {
  const seen = read<Record<string, SeriesCard>>(RECENT_SERIES, {});
  const marks = getBookmarks();
  return read<HistoryEntry[]>(HISTORY, [])
    .slice()
    .sort((a, b) => b.at - a.at)
    .map((h) => ({ ...h, series: seen[h.seriesId] ?? marks.find((s) => idOf(s) === h.seriesId) ?? null }));
}

export function recordHistory(entry: HistoryEntry) {
  const list = read<HistoryEntry[]>(HISTORY, []).filter(
    (h) => !(h.seriesId === entry.seriesId && h.chapterKey === entry.chapterKey),
  );
  write(HISTORY, [entry, ...list].slice(0, 300));
}

export function clearHistory() {
  write(HISTORY, []);
}

/* ------------------------------ favourites ------------------------------- */

export function getFavouriteIds(): string[] {
  return read<string[]>(FAVOURITES, []);
}

export function isFavourite(id: string): boolean {
  return getFavouriteIds().includes(id);
}

export function toggleFavourite(series: SeriesCard): boolean {
  const id = idOf(series);
  const ids = getFavouriteIds();
  const exists = ids.includes(id);
  write(FAVOURITES, exists ? ids.filter((x) => x !== id) : [id, ...ids]);
  rememberSeries(series);
  if (!exists) track("bookmark");
  sfx(exists ? "toggleOff" : "toggleOn");
  return !exists;
}

export function getFavourites(): SeriesCard[] {
  return getFavouriteIds()
    .map((id) => getSeen(id))
    .filter((s): s is SeriesCard => !!s);
}

/* ---------------------------- search history ----------------------------- */

export function getRecentSearches(): string[] {
  return read<string[]>(SEARCHES, []);
}

export function pushRecentSearch(q: string) {
  const term = q.trim();
  if (term.length < 2) return;
  write(SEARCHES, [term, ...getRecentSearches().filter((s) => s.toLowerCase() !== term.toLowerCase())].slice(0, 8));
}

export function clearRecentSearches() {
  write(SEARCHES, []);
}

/** Local typeahead over everything the user has seen or saved. */
export function searchLocal(q: string, limit = 6): SeriesCard[] {
  const term = q.trim().toLowerCase();
  if (term.length < 2) return [];
  const seen = Object.values(read<Record<string, SeriesCard>>(RECENT_SERIES, {}));
  const pool = new Map<string, SeriesCard>();
  [...getBookmarks(), ...seen].forEach((s) => pool.set(idOf(s), s));
  return [...pool.values()].filter((s) => s.title.toLowerCase().includes(term)).slice(0, limit);
}

/* --------------------------------- misc ---------------------------------- */

export function chapterLabel(c: { number: number | null; volume?: number | null; title: string | null }): string {
  if (c.number != null) return `Ch. ${Number.isInteger(c.number) ? c.number : c.number.toFixed(1)}`;
  if (c.title) return c.title;
  if (c.volume != null) return `Vol. ${c.volume}`;
  return "Chapter";
}
