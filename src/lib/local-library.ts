// Library + reading progress live in the browser, because the catalogue is
// streamed live from the source and no series/chapter rows are stored anymore.
import type { SeriesCard } from "@/lib/catalog";

const BOOKMARKS = "kt.bookmarks.v1";
const PROGRESS = "kt.progress.v1";

export type ProgressEntry = { chapterId: string; chapterNumber: string; page: number; at: number };

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

export function getBookmarks(): SeriesCard[] {
  return read<SeriesCard[]>(BOOKMARKS, []);
}

export function isBookmarked(id: string): boolean {
  return getBookmarks().some((s) => s.id === id);
}

export function toggleBookmark(series: SeriesCard): boolean {
  const list = getBookmarks();
  const exists = list.some((s) => s.id === series.id);
  const next = exists ? list.filter((s) => s.id !== series.id) : [series, ...list];
  write(BOOKMARKS, next);
  if (!exists) rememberSeries(series);
  return !exists;
}

export function getProgress(seriesId: string): ProgressEntry | null {
  return read<Record<string, ProgressEntry>>(PROGRESS, {})[seriesId] ?? null;
}

export function setProgress(seriesId: string, entry: ProgressEntry) {
  const all = read<Record<string, ProgressEntry>>(PROGRESS, {});
  all[seriesId] = entry;
  write(PROGRESS, all);
}

// ===== Search history (device-local) =====
const SEARCHES = "kt.searches.v1";

export function getRecentSearches(): string[] {
  return read<string[]>(SEARCHES, []);
}

export function pushRecentSearch(q: string) {
  const term = q.trim();
  if (term.length < 2) return;
  const next = [term, ...getRecentSearches().filter((s) => s.toLowerCase() !== term.toLowerCase())].slice(0, 8);
  write(SEARCHES, next);
}

export function clearRecentSearches() {
  write(SEARCHES, []);
}

/** Series with saved reading progress, most recent first. */
export function getContinueReading(): Array<{ series: SeriesCard; progress: ProgressEntry }> {
  const all = read<Record<string, ProgressEntry>>(PROGRESS, {});
  const marks = getBookmarks();
  const seen = read<Record<string, SeriesCard>>(RECENT_SERIES, {});
  return Object.entries(all)
    .map(([seriesId, progress]) => {
      const series = marks.find((s) => s.id === seriesId) ?? seen[seriesId];
      return series ? { series, progress } : null;
    })
    .filter((x): x is { series: SeriesCard; progress: ProgressEntry } => !!x)
    .sort((a, b) => b.progress.at - a.progress.at)
    .slice(0, 12);
}

const RECENT_SERIES = "kt.recentseries.v1";

/** Remember minimal series metadata so Continue Reading can render cards. */
export function rememberSeries(series: SeriesCard) {
  const all = read<Record<string, SeriesCard>>(RECENT_SERIES, {});
  all[series.id] = series;
  const entries = Object.entries(all).slice(-60);
  write(RECENT_SERIES, Object.fromEntries(entries));
}

// ===== Reading history (device-local) =====
const HISTORY = "kt.history.v1";

export type HistoryEntry = {
  seriesId: string;
  chapterId: string;
  chapterNumber: string;
  at: number;
};

export function getHistory(): Array<HistoryEntry & { series: SeriesCard | null }> {
  const seen = read<Record<string, SeriesCard>>(RECENT_SERIES, {});
  const marks = getBookmarks();
  return read<HistoryEntry[]>(HISTORY, [])
    .slice()
    .sort((a, b) => b.at - a.at)
    .map((h) => ({ ...h, series: marks.find((s) => s.id === h.seriesId) ?? seen[h.seriesId] ?? null }));
}

export function recordHistory(entry: HistoryEntry) {
  const list = read<HistoryEntry[]>(HISTORY, []).filter(
    (h) => !(h.seriesId === entry.seriesId && h.chapterId === entry.chapterId),
  );
  write(HISTORY, [entry, ...list].slice(0, 120));
}

export function clearHistory() {
  write(HISTORY, []);
}

// ===== Favourites (a curated subset of the library) =====
const FAVOURITES = "kt.favourites.v1";

export function getFavouriteIds(): string[] {
  return read<string[]>(FAVOURITES, []);
}

export function isFavourite(id: string): boolean {
  return getFavouriteIds().includes(id);
}

export function toggleFavourite(series: SeriesCard): boolean {
  const ids = getFavouriteIds();
  const exists = ids.includes(series.id);
  write(FAVOURITES, exists ? ids.filter((x) => x !== series.id) : [series.id, ...ids]);
  rememberSeries(series);
  return !exists;
}

export function getFavourites(): SeriesCard[] {
  const seen = read<Record<string, SeriesCard>>(RECENT_SERIES, {});
  const marks = getBookmarks();
  return getFavouriteIds()
    .map((id) => marks.find((s) => s.id === id) ?? seen[id] ?? null)
    .filter((s): s is SeriesCard => !!s);
}

