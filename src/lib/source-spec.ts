/**
 * KuroSource spec v1 — a declarative JSON recipe that lets ANY repository
 * describe how KuroToon reads it. No per-source code, no plugins to install:
 * a repo ships JSON, KuroToon follows it.
 *
 * Shape (all paths are dot/bracket paths into the JSON response, "|" separates
 * fallback candidates, "" means the response root):
 *
 * {
 *   "api": "https://example.org/api",
 *   "headers": { "x-key": "..." },
 *   "list": {
 *     "path": "/search?q={query}&page={page}&limit={limit}",
 *     "items": "data",
 *     "id": "id", "title": "attributes.title|title",
 *     "cover": "cover_url", "rating": "score", "genres": "tags"
 *   },
 *   "series": {
 *     "path": "/manga/{id}", "root": "data",
 *     "title": "title", "description": "synopsis", "cover": "cover_url",
 *     "author": "author", "status": "status", "year": "year", "genres": "tags",
 *     "chapters": { "items": "chapters", "id": "id", "number": "number",
 *                   "title": "title", "date": "published_at", "group": "group" }
 *   },
 *   "chapter": {
 *     "path": "/chapter/{id}", "pages": "data.pages", "image": "url",
 *     "number": "data.number", "title": "data.title", "seriesId": "data.manga_id"
 *   }
 * }
 */

export type SpecList = {
  path: string;
  items: string;
  id: string;
  title: string;
  cover?: string;
  rating?: string;
  genres?: string;
  nextCursor?: string;
  /** "page" (1-based) or "offset" (records). Defaults to "page". */
  paging?: "page" | "offset";
};

export type SpecChapters = {
  items: string;
  id: string;
  number?: string;
  title?: string;
  date?: string;
  group?: string;
  pages?: string;
};

export type SpecSeries = {
  path: string;
  root?: string;
  title?: string;
  description?: string;
  cover?: string;
  author?: string;
  artist?: string;
  status?: string;
  year?: string;
  genres?: string;
  chapters?: SpecChapters;
};

export type SpecChapter = {
  path: string;
  pages: string;
  /** Field inside each page entry when pages are objects rather than strings. */
  image?: string;
  number?: string;
  title?: string;
  seriesId?: string;
  seriesTitle?: string;
};

export type SourceSpec = {
  api: string;
  headers?: Record<string, string>;
  list?: SpecList;
  series?: SpecSeries;
  chapter?: SpecChapter;
};

function s(v: unknown, max = 400): string | undefined {
  return typeof v === "string" && v.trim() ? v.trim().slice(0, max) : undefined;
}

function headers(v: unknown): Record<string, string> | undefined {
  if (!v || typeof v !== "object" || Array.isArray(v)) return undefined;
  const out: Record<string, string> = {};
  let n = 0;
  for (const [k, val] of Object.entries(v as Record<string, unknown>)) {
    const key = s(k, 60);
    const value = s(val, 400);
    // Never forward hop-by-hop or cookie headers from an untrusted manifest.
    if (!key || !value || /^(host|cookie|content-length|connection)$/i.test(key)) continue;
    out[key] = value;
    if (++n >= 12) break;
  }
  return n ? out : undefined;
}

/** Normalises an untrusted manifest fragment into a SourceSpec, or null. */
export function parseSpec(raw: unknown): SourceSpec | null {
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) return null;
  const o = raw as Record<string, unknown>;
  const api = s(o["api"] ?? o["apiUrl"] ?? o["api_url"] ?? o["baseUrl"] ?? o["base_url"], 300);
  if (!api) return null;

  const spec: SourceSpec = { api };
  const h = headers(o["headers"]);
  if (h) spec.headers = h;

  const l = (o["list"] ?? o["search"] ?? o["browse"]) as Record<string, unknown> | undefined;
  if (l && typeof l === "object") {
    const path = s(l["path"] ?? l["url"], 500);
    const items = s(l["items"] ?? l["results"], 200) ?? "";
    const id = s(l["id"], 200);
    const title = s(l["title"] ?? l["name"], 200);
    if (path && id && title) {
      spec.list = {
        path,
        items,
        id,
        title,
        ...(s(l["cover"] ?? l["thumbnail"], 200) ? { cover: s(l["cover"] ?? l["thumbnail"], 200)! } : {}),
        ...(s(l["rating"] ?? l["score"], 200) ? { rating: s(l["rating"] ?? l["score"], 200)! } : {}),
        ...(s(l["genres"] ?? l["tags"], 200) ? { genres: s(l["genres"] ?? l["tags"], 200)! } : {}),
        ...(s(l["nextCursor"] ?? l["next"], 200) ? { nextCursor: s(l["nextCursor"] ?? l["next"], 200)! } : {}),
        paging: l["paging"] === "offset" ? "offset" : "page",
      };
    }
  }

  const se = o["series"] ?? o["manga"] ?? o["detail"];
  if (se && typeof se === "object" && !Array.isArray(se)) {
    const e = se as Record<string, unknown>;
    const path = s(e["path"] ?? e["url"], 500);
    if (path) {
      const ch = e["chapters"] as Record<string, unknown> | undefined;
      let chapters: SpecChapters | undefined;
      if (ch && typeof ch === "object") {
        const cid = s(ch["id"], 200);
        if (cid) {
          chapters = {
            items: s(ch["items"] ?? ch["list"], 200) ?? "",
            id: cid,
            ...(s(ch["number"] ?? ch["chapter"], 200) ? { number: s(ch["number"] ?? ch["chapter"], 200)! } : {}),
            ...(s(ch["title"], 200) ? { title: s(ch["title"], 200)! } : {}),
            ...(s(ch["date"] ?? ch["published_at"], 200) ? { date: s(ch["date"] ?? ch["published_at"], 200)! } : {}),
            ...(s(ch["group"] ?? ch["scanlator"], 200) ? { group: s(ch["group"] ?? ch["scanlator"], 200)! } : {}),
            ...(s(ch["pages"] ?? ch["pageCount"], 200) ? { pages: s(ch["pages"] ?? ch["pageCount"], 200)! } : {}),
          };
        }
      }
      spec.series = {
        path,
        ...(s(e["root"], 200) !== undefined ? { root: s(e["root"], 200)! } : {}),
        ...(s(e["title"] ?? e["name"], 200) ? { title: s(e["title"] ?? e["name"], 200)! } : {}),
        ...(s(e["description"] ?? e["synopsis"], 200) ? { description: s(e["description"] ?? e["synopsis"], 200)! } : {}),
        ...(s(e["cover"] ?? e["thumbnail"], 200) ? { cover: s(e["cover"] ?? e["thumbnail"], 200)! } : {}),
        ...(s(e["author"], 200) ? { author: s(e["author"], 200)! } : {}),
        ...(s(e["artist"], 200) ? { artist: s(e["artist"], 200)! } : {}),
        ...(s(e["status"], 200) ? { status: s(e["status"], 200)! } : {}),
        ...(s(e["year"], 200) ? { year: s(e["year"], 200)! } : {}),
        ...(s(e["genres"] ?? e["tags"], 200) ? { genres: s(e["genres"] ?? e["tags"], 200)! } : {}),
        ...(chapters ? { chapters } : {}),
      };
    }
  }

  const c = o["chapter"] ?? o["pages"];
  if (c && typeof c === "object" && !Array.isArray(c)) {
    const e = c as Record<string, unknown>;
    const path = s(e["path"] ?? e["url"], 500);
    const pages = s(e["pages"] ?? e["items"] ?? e["images"], 200) ?? "";
    if (path) {
      spec.chapter = {
        path,
        pages,
        ...(s(e["image"] ?? e["url_field"] ?? e["src"], 200) ? { image: s(e["image"] ?? e["url_field"] ?? e["src"], 200)! } : {}),
        ...(s(e["number"], 200) ? { number: s(e["number"], 200)! } : {}),
        ...(s(e["title"], 200) ? { title: s(e["title"], 200)! } : {}),
        ...(s(e["seriesId"] ?? e["manga_id"], 200) ? { seriesId: s(e["seriesId"] ?? e["manga_id"], 200)! } : {}),
        ...(s(e["seriesTitle"], 200) ? { seriesTitle: s(e["seriesTitle"], 200)! } : {}),
      };
    }
  }

  if (!spec.list && !spec.series && !spec.chapter) return null;
  return spec;
}

/** What a spec can actually do, used for honest capability badges. */
export function specCapabilities(spec: SourceSpec | null): string[] {
  if (!spec) return [];
  const caps: string[] = [];
  if (spec.list) caps.push("search");
  if (spec.series?.chapters) caps.push("chapters");
  if (spec.chapter) caps.push("pages");
  return caps;
}

/** A source is fully readable only when it can search, list chapters AND serve pages. */
export function specIsReadable(spec: SourceSpec | null): boolean {
  return !!(spec?.list && spec.series?.chapters && spec.chapter);
}
