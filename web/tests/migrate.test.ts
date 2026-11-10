import { beforeEach, expect, test } from "bun:test";
import { mapReadChapters, matchChapter, migrateSeries, titleScore } from "../src/lib/migrate";
import { getProgress, getReadChapters, isBookmarked, markChapterRead, setProgress, toggleBookmark } from "../src/lib/local-library";
import { getSeriesCategories, setSeriesCategories } from "../src/lib/categories";
import type { SeriesCard } from "../src/engine/types";

const card = (sourceId: string, key: string): SeriesCard =>
  ({ sourceId, key, title: "One Piece", cover: null, authors: [], tags: [], status: 0, contentRating: 0 }) as unknown as SeriesCard;

const A = [{ key: "a1", number: 1 }, { key: "a2", number: 2 }, { key: "a2.5", number: 2.5 }, { key: "a3", number: 3 }];
const B = [{ key: "b3", number: 3 }, { key: "b2", number: 2 }, { key: "b1", number: 1 }, { key: "bx", number: null }];

beforeEach(() => localStorage.clear());

test("read chapters map across sources by number", () => {
  expect(mapReadChapters(["a1", "a2", "a2.5"], A, B).sort()).toEqual(["b1", "b2"]);
  expect(matchChapter("a3", A, B)?.key).toBe("b3");
  expect(matchChapter("a2.5", A, B)).toBeNull();
});

test("title score ranks exact > contains > overlap", () => {
  expect(titleScore("One Piece", "one-piece")).toBe(1);
  expect(titleScore("One Piece", "One Piece (Official)")).toBe(0.8);
  expect(titleScore("Solo Leveling", "Na Honjaman Level Up")).toBe(0);
});

test("migrate moves library entry, categories, reads and progress", () => {
  const from = card("src.a", "1");
  const to = card("src.b", "9");
  toggleBookmark(from);
  setSeriesCategories("src.a::1", ["Reading"]);
  markChapterRead("src.a::1", "a1");
  markChapterRead("src.a::1", "a2");
  setProgress("src.a::1", { chapterKey: "a3", chapterLabel: "Ch. 3", page: 7, total: 20, at: 1 });

  const r = migrateSeries(from, to, A, B, { chapters: true, categories: true, removeOld: true });
  expect(r.readCarried).toBe(2);
  expect(isBookmarked("src.b::9")).toBe(true);
  expect(isBookmarked("src.a::1")).toBe(false);
  expect(getSeriesCategories("src.b::9")).toEqual(["Reading"]);
  expect(getSeriesCategories("src.a::1")).toEqual([]);
  expect(getReadChapters("src.b::9").sort()).toEqual(["b1", "b2"]);
  expect(getProgress("src.b::9")?.chapterKey).toBe("b3");
});

test("copy keeps the old entry", () => {
  const from = card("src.a", "1");
  toggleBookmark(from);
  migrateSeries(from, card("src.b", "9"), A, B, { chapters: false, categories: false, removeOld: false });
  expect(isBookmarked("src.a::1")).toBe(true);
  expect(isBookmarked("src.b::9")).toBe(true);
  expect(getReadChapters("src.b::9")).toEqual([]);
});

test("source text entities are decoded", async () => {
  const { decodeEntities } = await import("../src/engine/runtime");
  expect(decodeEntities("the &quot;Pirate King,&quot; &amp; &#39;crew&#x27;")).toBe(`the "Pirate King," & 'crew'`);
  expect(decodeEntities("&bogus; fine")).toBe("&bogus; fine");
});
