/**
 * KuroToon engine domain types.
 *
 * The engine follows the Mihon / Tachiyomi / Aidoku model: KuroToon ships no
 * content. Users add *extension repositories*; each repository lists *sources*
 * (packaged Aidoku WASM extensions, `.aix`). Installed sources run on-device in
 * a Web Worker, and every network request goes through the same-origin relay.
 */
import type { Chapter, Filter, FilterValue, Listing, Manga, Page } from "@nemu.pm/aidoku-runtime";

export type { Chapter, Filter, FilterValue, Listing, Manga, Page };

/** 0 = safe, 1 = suggestive, 2 = adult (Aidoku `contentRating`). */
export type ContentRating = 0 | 1 | 2;

/** One entry in a repository's `index.min.json`. */
export type RepoSourceEntry = {
  id: string;
  name: string;
  version: number;
  iconURL?: string;
  downloadURL: string;
  languages: string[];
  contentRating: ContentRating;
  baseURL?: string;
  altNames?: string[];
  minAppVersion?: string;
};

export type Repository = {
  /** Canonical index URL (always ends in .json). */
  url: string;
  name: string;
  addedAt: number;
  fetchedAt: number | null;
  error: string | null;
  sources: RepoSourceEntry[];
};

export type InstalledSource = {
  id: string;
  name: string;
  version: number;
  languages: string[];
  contentRating: ContentRating;
  iconUrl: string | null;
  baseUrl: string | null;
  repoUrl: string;
  downloadUrl: string;
  installedAt: number;
  enabled: boolean;
};

/** Normalised card used everywhere in the UI. */
export type SeriesCard = {
  /** `${sourceId}` */
  sourceId: string;
  /** Source-specific manga key. */
  key: string;
  title: string;
  cover: string | null;
  authors: string[];
  tags: string[];
  status: number | null;
  contentRating: ContentRating;
};

export type SeriesDetail = SeriesCard & {
  description: string;
  artists: string[];
  url: string | null;
  viewer: number | null;
};

export type ChapterRow = {
  key: string;
  title: string | null;
  number: number | null;
  volume: number | null;
  scanlator: string | null;
  date: number | null;
  lang: string | null;
  url: string | null;
  locked: boolean;
  /** Original runtime object, needed to request pages. */
  raw: Chapter;
};

export type PageList = { pages: Page[] };

export type SourceHealth = {
  ok: number;
  fail: number;
  lastError: string | null;
  lastLatencyMs: number | null;
  lastCheckedAt: number | null;
};

export const STATUS_LABEL: Record<number, string> = {
  0: "Unknown",
  1: "Ongoing",
  2: "Completed",
  3: "Cancelled",
  4: "Hiatus",
};
