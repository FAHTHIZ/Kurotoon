import { describe, expect, test } from "bun:test";
import { candidateIndexUrls, parseIndex, RepoFormatError } from "@/engine/repos";
import { decodeKey, encodeKey, seriesId } from "@/engine/ids";

describe("candidateIndexUrls", () => {
  test("keeps a direct .json URL", () => {
    expect(candidateIndexUrls("https://aidoku-community.github.io/sources/index.min.json")).toEqual([
      "https://aidoku-community.github.io/sources/index.min.json",
    ]);
  });
  test("unwraps aidoku:// links", () => {
    expect(candidateIndexUrls("aidoku://addSourceList?url=https://x.dev/s/index.json")).toEqual(["https://x.dev/s/index.json"]);
  });
  test("expands GitHub repo URLs to Pages and raw candidates", () => {
    const c = candidateIndexUrls("https://github.com/Aidoku-Community/sources");
    expect(c[0]).toBe("https://aidoku-community.github.io/sources/index.min.json");
    expect(c).toContain("https://raw.githubusercontent.com/Aidoku-Community/sources/gh-pages/index.min.json");
  });
  test("rejects non-http schemes and junk", () => {
    expect(candidateIndexUrls("javascript:alert(1)")).toEqual([]);
    expect(candidateIndexUrls("not a url")).toEqual([]);
  });
});

describe("parseIndex", () => {
  const url = "https://example.dev/repo/index.min.json";
  test("parses Aidoku source lists and resolves relative assets", () => {
    const r = parseIndex(url, {
      name: "Example",
      sources: [
        { id: "en.a", name: "A", version: 3, downloadURL: "sources/a.aix", iconURL: "icons/a.png", languages: ["en"], contentRating: 0 },
        { id: "en.a", name: "dup", downloadURL: "x.aix" },
        { id: "en.b", name: "B", file: "b.aix", nsfw: true },
        { name: "missing id", downloadURL: "c.aix" },
      ],
    });
    expect(r.name).toBe("Example");
    expect(r.sources.map((s) => s.id)).toEqual(["en.a", "en.b"]);
    expect(r.sources[0].downloadURL).toBe("https://example.dev/repo/sources/a.aix");
    expect(r.sources[0].iconURL).toBe("https://example.dev/repo/icons/a.png");
    expect(r.sources[1].contentRating).toBe(2);
    expect(r.sources[1].languages).toEqual(["en"]);
  });
  test("explains Mihon APK repositories", () => {
    expect(() => parseIndex(url, [{ apk: "x.apk" }])).toThrow(RepoFormatError);
    expect(() => parseIndex(url, [{ apk: "x.apk" }])).toThrow(/Mihon/);
  });
  test("rejects objects without sources", () => {
    expect(() => parseIndex(url, { name: "x" })).toThrow(/sources/);
  });
});

describe("ids", () => {
  test("encode/decode round-trips arbitrary keys", () => {
    for (const k of ["/manga/123", "a b?c=d&e#f", "日本語/タイトル", "plain"]) {
      const e = encodeKey(k);
      expect(e).not.toContain("/");
      expect(decodeKey(e)).toBe(k);
    }
    expect(seriesId("multi.mangaplus", "100")).toBe("multi.mangaplus::100");
  });
});

describe("updates diff", async () => {
  const { diffChapters } = await import("@/lib/updates");
  test("first check only seeds", () => {
    expect(diffChapters(undefined, ["a", "b"])).toEqual({ fresh: [], known: ["a", "b"] });
  });
  test("later checks report new keys", () => {
    expect(diffChapters(["a", "b"], ["c", "b", "a"])).toEqual({ fresh: ["c"], known: ["a", "b", "c"] });
  });
});

describe("library arrangement", async () => {
  const c = await import("@/lib/categories");
  const mk = (key: string, title: string) => ({ sourceId: "s", key, title, cover: null, authors: [], tags: [], status: null, contentRating: 0 as const });
  test("categories, default bucket and sort", () => {
    localStorage.clear();
    const items = [mk("3", "Charlie"), mk("2", "alpha"), mk("1", "Bravo")];
    expect(c.addCategory("Reading")).toBe(true);
    expect(c.addCategory("reading")).toBe(false);
    expect(c.addCategory("Default")).toBe(false);
    c.setSeriesCategories("s::2", ["Reading"]);
    const v = { ...c.DEFAULT_VIEW };
    expect(c.arrangeLibrary(items, v).map((s) => s.key)).toEqual(["3", "2", "1"]);
    expect(c.arrangeLibrary(items, { ...v, category: "Reading" }).map((s) => s.key)).toEqual(["2"]);
    expect(c.arrangeLibrary(items, { ...v, category: "__default" }).map((s) => s.key)).toEqual(["3", "1"]);
    expect(c.arrangeLibrary(items, { ...v, sort: "title", desc: false }).map((s) => s.title)).toEqual(["alpha", "Bravo", "Charlie"]);
    c.renameCategory("Reading", "Now");
    expect(c.getSeriesCategories("s::2")).toEqual(["Now"]);
    c.deleteCategory("Now");
    expect(c.getSeriesCategories("s::2")).toEqual([]);
  });
});
