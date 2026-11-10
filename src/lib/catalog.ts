// Client-side access to the real-time MangaDex layer (no stored content).
export type SeriesCard = {
  id: string;
  slug: string;
  title: string;
  cover_url: string | null;
  rating: number | null;
  genres: string[];
};

export type SeriesDetail = SeriesCard & {
  description: string;
  author: string | null;
  artist: string | null;
  status: string;
  year: number | null;
};

export type ChapterRow = {
  id: string;
  number: string;
  title: string | null;
  pages: number;
  publishAt: string;
  group: string | null;
};

export type TagRow = { id: string; name: string; group: string };

async function get<T>(url: string): Promise<T> {
  const res = await fetch(url);
  const body = (await res.json()) as T & { ok?: boolean; error?: string };
  if (!res.ok || body.ok === false) throw new Error(body.error ?? `request failed (${res.status})`);
  return body;
}

export type ListParams = {
  sort?: "latest" | "popular" | "rating" | "title" | "year";
  q?: string;
  genres?: string[];
  excluded?: string[];
  status?: string[];
  demos?: string[];
  yearFrom?: number;
  yearTo?: number;
  mode?: "any" | "all";
  limit?: number;
  cursor?: number;
};

export type ListResult = { items: SeriesCard[]; nextCursor: number | null; hasMore: boolean };

export async function listSeries(p: ListParams = {}): Promise<ListResult> {
  const sp = new URLSearchParams();
  if (p.sort) sp.set("sort", p.sort);
  if (p.q) sp.set("q", p.q);
  if (p.genres?.length) sp.set("g", p.genres.join(","));
  if (p.excluded?.length) sp.set("x", p.excluded.join(","));
  if (p.status?.length) sp.set("status", p.status.join(","));
  if (p.demos?.length) sp.set("demo", p.demos.join(","));
  if (p.yearFrom) sp.set("from", String(p.yearFrom));
  if (p.yearTo) sp.set("to", String(p.yearTo));
  if (p.mode) sp.set("mode", p.mode);
  if (p.limit) sp.set("limit", String(p.limit));
  if (p.cursor) sp.set("cursor", String(p.cursor));
  const body = await get<ListResult>(`/api/public/md/list?${sp.toString()}`);
  return { items: body.items, nextCursor: body.nextCursor ?? null, hasMore: !!body.hasMore };
}

export type Suggestion = { id: string; title: string; cover_url: string | null; year: number | null; status: string };

export async function suggest(q: string): Promise<Suggestion[]> {
  const body = await get<{ items: Suggestion[] }>(`/api/public/md/suggest?q=${encodeURIComponent(q)}`);
  return body.items;
}


export async function getSeries(id: string): Promise<{ series: SeriesDetail; chapters: ChapterRow[] }> {
  return get<{ series: SeriesDetail; chapters: ChapterRow[] }>(`/api/public/md/series/${id}`);
}

export async function getChapter(id: string): Promise<{
  chapter: { id: string; number: string | null; title: string | null; series_id: string | null; series_title: string; group: string | null };
  siblings: Array<{ id: string; number: string }>;
  pages: Array<{ page_number: number; image_url: string }>;
}> {
  return get(`/api/public/md/chapter/${id}`);
}

export async function listTags(): Promise<TagRow[]> {
  const body = await get<{ tags: TagRow[] }>("/api/public/md/tags");
  return body.tags;
}
