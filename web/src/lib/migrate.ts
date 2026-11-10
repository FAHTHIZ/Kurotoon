/**
 * Source migration — Mihon's "Migrate": move a library entry to the same
 * series on another source, carrying over library membership, categories,
 * read chapters and the reading position. Chapters are matched by number
 * (sources key chapters differently, but chapter numbers line up).
 */
import type { ChapterRow, SeriesCard } from "@/engine/types";
import { seriesId } from "@/engine/ids";
import {
  getProgress,
  getReadChapters,
  isBookmarked,
  isFavourite,
  markChapterRead,
  rememberSeries,
  setProgress,
  toggleBookmark,
  toggleFavourite,
} from "./local-library";
import { getSeriesCategories, setSeriesCategories } from "./categories";
import { seedKnownChapters } from "./updates";

export type MigrateOptions = {
  chapters: boolean;
  categories: boolean;
  /** Remove the old entry from the library (Mihon "Migrate"); false = "Copy". */
  removeOld: boolean;
};

type ChapterLike = Pick<ChapterRow, "key" | "number">;

const numKey = (n: number) => n.toFixed(3);

/** Keys of `to` chapters whose number matches a read chapter in `from`. */
export function mapReadChapters(
  readKeys: string[],
  from: ChapterLike[],
  to: ChapterLike[],
): string[] {
  const read = new Set(readKeys);
  const readNums = new Set(
    from.filter((c) => read.has(c.key) && c.number != null).map((c) => numKey(c.number!)),
  );
  return to.filter((c) => c.number != null && readNums.has(numKey(c.number))).map((c) => c.key);
}

/** The `to` chapter matching a chapter in `from`, by number. */
export function matchChapter(
  fromKey: string,
  from: ChapterLike[],
  to: ChapterLike[],
): ChapterLike | null {
  const n = from.find((c) => c.key === fromKey)?.number;
  if (n == null) return null;
  return to.find((c) => c.number != null && numKey(c.number) === numKey(n)) ?? null;
}

/** Normalised title for ranking search results. */
export function titleScore(a: string, b: string): number {
  const norm = (s: string) =>
    s
      .toLowerCase()
      .normalize("NFKD")
      .replace(/[^\p{L}\p{N}]+/gu, " ")
      .trim();
  const x = norm(a);
  const y = norm(b);
  if (!x || !y) return 0;
  if (x === y) return 1;
  if (x.includes(y) || y.includes(x)) return 0.8;
  const wa = new Set(x.split(" "));
  const wb = y.split(" ");
  const hit = wb.filter((w) => wa.has(w)).length;
  return (hit / Math.max(wa.size, wb.length)) * 0.7;
}

export function migrateSeries(
  from: SeriesCard,
  to: SeriesCard,
  fromChapters: ChapterLike[],
  toChapters: ChapterLike[],
  opts: MigrateOptions,
): { readCarried: number } {
  const oldId = seriesId(from.sourceId, from.key);
  const newId = seriesId(to.sourceId, to.key);
  rememberSeries(to);

  // The migrated series always lands in the library.
  if (!isBookmarked(newId)) toggleBookmark(to);
  if (isFavourite(oldId) && !isFavourite(newId)) toggleFavourite(to);

  if (opts.categories) {
    const cats = getSeriesCategories(oldId);
    if (cats.length)
      setSeriesCategories(newId, [...new Set([...getSeriesCategories(newId), ...cats])]);
  }

  let readCarried = 0;
  if (opts.chapters) {
    const keys = mapReadChapters(getReadChapters(oldId), fromChapters, toChapters);
    for (const k of keys) markChapterRead(newId, k, true);
    readCarried = keys.length;
    const p = getProgress(oldId);
    if (p) {
      const m = matchChapter(p.chapterKey, fromChapters, toChapters);
      if (m) setProgress(newId, { ...p, chapterKey: m.key, page: 1, at: Date.now() });
    }
  }

  // Don't flood Updates with every chapter of the new source.
  seedKnownChapters(
    newId,
    toChapters.map((c) => c.key),
  );

  if (opts.removeOld) {
    if (isBookmarked(oldId)) toggleBookmark(from);
    if (isFavourite(oldId)) toggleFavourite(from);
    if (opts.categories) setSeriesCategories(oldId, []);
  }
  return { readCarried };
}
