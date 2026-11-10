// Real-time MangaDex access layer. Nothing here writes to the database —
// every request is fetched live and only edge-cached via Cache-Control.
const MD = "https://api.mangadex.org";
const UA = "KuroToon/1.0";

export const CONTENT_RATINGS = ["safe", "suggestive", "erotica"] as const;

export async function md<T>(path: string): Promise<T> {
  const res = await fetch(`${MD}${path}`, { headers: { "User-Agent": UA } });
  if (!res.ok) throw new Error(`mangadex ${res.status} on ${path.split("?")[0]}`);
  return (await res.json()) as T;
}

export function pick(t: Record<string, string> | undefined | null): string {
  if (!t) return "";
  return t["en"] ?? t["en-us"] ?? Object.values(t)[0] ?? "";
}

type MdRel = { type: string; attributes?: Record<string, unknown> };
export type MdManga = {
  id: string;
  attributes: {
    title: Record<string, string>;
    altTitles?: Array<Record<string, string>>;
    description: Record<string, string>;
    status: string;
    year: number | null;
    contentRating: string;
    lastChapter: string | null;
    tags: Array<{ id: string; attributes: { name: Record<string, string>; group: string } }>;
  };
  relationships: MdRel[];
};

export type SeriesCard = {
  id: string;
  slug: string;
  title: string;
  cover_url: string | null;
  rating: number | null;
  genres: string[];
};

export function coverUrl(m: MdManga, size: 256 | 512 = 512): string | null {
  const rel = m.relationships.find((r) => r.type === "cover_art");
  const file = rel?.attributes?.["fileName"] as string | undefined;
  return file ? `https://uploads.mangadex.org/covers/${m.id}/${file}.${size}.jpg` : null;
}

export function relName(m: MdManga, type: "author" | "artist"): string | null {
  const rel = m.relationships.find((r) => r.type === type);
  return (rel?.attributes?.["name"] as string | undefined) ?? null;
}

export function toCard(m: MdManga, rating: number | null): SeriesCard {
  return {
    id: m.id,
    slug: m.id,
    title: pick(m.attributes.title) || "Untitled",
    cover_url: coverUrl(m),
    rating,
    genres: m.attributes.tags
      .filter((t) => t.attributes.group === "genre" || t.attributes.group === "theme")
      .map((t) => pick(t.attributes.name))
      .filter(Boolean),
  };
}

/** Batch rating lookup (10-point scale) — one call for a whole page of results. */
export async function ratings(ids: string[]): Promise<Record<string, number | null>> {
  if (ids.length === 0) return {};
  try {
    const qs = ids.map((id) => `manga[]=${id}`).join("&");
    const data = await md<{ statistics: Record<string, { rating?: { average: number | null; bayesian: number | null } }> }>(
      `/statistics/manga?${qs}`,
    );
    const out: Record<string, number | null> = {};
    for (const id of ids) {
      const r = data.statistics?.[id]?.rating;
      out[id] = r?.bayesian ?? r?.average ?? null;
    }
    return out;
  } catch {
    return {};
  }
}

// ---- Tag catalogue (name -> uuid), cached in module memory per isolate ----
type Tag = { id: string; name: string; group: string };
let tagCache: { at: number; tags: Tag[] } | null = null;

export async function tags(): Promise<Tag[]> {
  if (tagCache && Date.now() - tagCache.at < 60 * 60_000) return tagCache.tags;
  const data = await md<{ data: Array<{ id: string; attributes: { name: Record<string, string>; group: string } }> }>(
    "/manga/tag",
  );
  const list = data.data
    .map((t) => ({ id: t.id, name: pick(t.attributes.name), group: t.attributes.group }))
    .filter((t) => !!t.name)
    .sort((a, b) => a.group.localeCompare(b.group) || a.name.localeCompare(b.name));
  tagCache = { at: Date.now(), tags: list };
  return list;
}

export async function tagIds(names: string[]): Promise<string[]> {
  if (names.length === 0) return [];
  const all = await tags();
  const byName = new Map(all.map((t) => [t.name.toLowerCase(), t.id]));
  return names.map((n) => byName.get(n.trim().toLowerCase())).filter((v): v is string => !!v);
}

export function json(body: unknown, sMaxAge: number, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json",
      "cache-control": `public, max-age=30, s-maxage=${sMaxAge}, stale-while-revalidate=120`,
    },
  });
}
