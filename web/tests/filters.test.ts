import { expect, test } from "bun:test";
import {
  activeCount,
  cycleMulti,
  normalizeFilters,
  toFilterValues,
  type UiFilter,
} from "../src/engine/filters";
import { normalizeSettings, settingDefaults, visible } from "../src/engine/source-settings";

// Shapes taken from the real en.mangabat and en.comix filters.json.
const mangabat = [
  { type: "sort", title: "Sort", canAscend: false, options: ["Newest", "Latest", "Top read"] },
  { type: "select", id: "status", title: "Status", options: ["All", "Completed", "Ongoing"] },
  {
    type: "select",
    id: "genre",
    title: "Genre",
    isGenre: true,
    options: ["All", "Action", "Adult"],
  },
];
const comix = [
  { type: "text", id: "author", title: "Author", placeholder: "Author Name" },
  { type: "sort", id: "order", canAscend: true, options: ["Best Match", "Updated Date"] },
  {
    type: "multi-select",
    id: "genres[]",
    title: "Genres",
    isGenre: true,
    canExclude: true,
    options: ["Action", "Adult"],
    ids: ["6", "87264"],
  },
  {
    type: "select",
    id: "genres_mode",
    title: "Genres Inclusion Mode",
    options: ["AND", "OR"],
    ids: ["and", "or"],
    hideFromHeader: true,
  },
];

test("manifest filters normalise", () => {
  const f = normalizeFilters(mangabat);
  expect(f.map((x) => `${x.kind}:${x.id}`)).toEqual(["sort:Sort", "select:status", "select:genre"]);
  const c = normalizeFilters(comix);
  expect(c.map((x) => x.kind)).toEqual(["text", "sort", "multi", "select"]);
});

test("only non-default values are sent, with ids", () => {
  const c = normalizeFilters(comix);
  expect(toFilterValues(c, {})).toEqual([]);
  const multi = c[2] as Extract<UiFilter, { kind: "multi" }>;
  let m = cycleMulti(multi, undefined, "6"); // include Action
  m = cycleMulti(multi, m, "87264"); // include Adult
  m = cycleMulti(multi, m, "87264"); // → exclude Adult
  const state = {
    author: " Oda ",
    order: { index: 1, ascending: true },
    "genres[]": m,
    genres_mode: 1,
  };
  expect(activeCount(c, state)).toBe(5);
  expect(toFilterValues(c, state)).toEqual([
    { type: 0, name: "author", value: "Oda" },
    { type: 3, name: "order", value: { index: 1, ascending: true } },
    { type: 6, name: "genres[]", value: { included: ["6"], excluded: ["87264"] } },
    { type: 2, name: "genres_mode", value: "or" },
  ]);
  const b = normalizeFilters(mangabat);
  expect(toFilterValues(b, { status: 1, genre: 0 })).toEqual([
    { type: 2, name: "status", value: "Completed" },
  ]);
});

test("runtime (numeric) filters and groups normalise", () => {
  const f = normalizeFilters([
    { type: 4, name: "Completed only", default: false },
    { type: 5, name: "Genres", filters: [{ type: "genre", name: "Action" }] },
  ]);
  expect(f[0]).toMatchObject({ kind: "check", id: "Completed only" });
  expect(f[1]).toMatchObject({ kind: "group", filters: [{ kind: "check", id: "Action" }] });
  expect(toFilterValues(f, { Action: true })).toEqual([
    { type: 5, name: "Genres", filters: [{ type: 4, name: "Action", value: true }] },
  ]);
});

// From the real multi.mangaplus settings.json.
const mangaplus = [
  {
    type: "group",
    title: "SETTINGS",
    items: [
      {
        type: "select",
        key: "imgQuality",
        title: "Image Quality",
        values: ["low", "high", "super_high"],
        titles: ["Low", "Medium", "High"],
        default: "super_high",
      },
      { type: "switch", key: "split", title: "Split Double Pages", default: false },
    ],
  },
  {
    type: "group",
    title: "Mobile API",
    items: [
      { type: "switch", key: "mobile", title: "Use Mobile API", default: false },
      {
        type: "text",
        key: "os",
        title: "os",
        placeholder: "os (e.g., android)",
        requires: "mobile",
      },
      { type: "login", key: "acct", title: "Log in" },
    ],
  },
];

test("settings normalise with defaults and requires", () => {
  const s = normalizeSettings(mangaplus);
  expect(s.length).toBe(2);
  expect(settingDefaults(s)).toEqual({
    imgQuality: "super_high",
    split: false,
    mobile: false,
    os: "",
  });
  const os = (s[1] as { items: Parameters<typeof visible>[0][] }).items[1]!;
  expect(visible(os, { mobile: false })).toBe(false);
  expect(visible(os, { mobile: true })).toBe(true);
  expect((s[1] as { items: unknown[] }).items.length).toBe(2); // login skipped
});

import { hideAdultOptions } from "../src/engine/filters";
test("hideAdultOptions strips explicit genres and keeps ids aligned", () => {
  const out = hideAdultOptions([
    {
      kind: "multi",
      id: "g",
      title: "Genre",
      options: ["Action", "Smut", "Romance", "Hentai"],
      ids: ["1", "2", "3", "4"],
      canExclude: true,
    },
    {
      kind: "group",
      id: "gr",
      title: "Genres",
      filters: [
        { kind: "check", id: "a", title: "Adult", def: false },
        { kind: "check", id: "c", title: "Comedy", def: false },
      ],
    },
  ]);
  expect(out[0]).toMatchObject({ options: ["Action", "Romance"], ids: ["1", "3"] });
  expect((out[1] as { filters: unknown[] }).filters).toHaveLength(1);
});
