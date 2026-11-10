import { createFileRoute } from "@tanstack/react-router";
import { CONTENT_RATINGS, json, md, ratings, tagIds, toCard, type MdManga } from "@/lib/mangadex.server";

const ORDER: Record<string, string> = {
  latest: "order[latestUploadedChapter]=desc",
  popular: "order[followedCount]=desc",
  rating: "order[rating]=desc",
  title: "order[title]=asc",
  year: "order[year]=desc",
  relevance: "order[relevance]=desc",
};

const RATING_PARAMS = CONTENT_RATINGS.map((r) => `contentRating[]=${r}`);
const STATUSES = ["ongoing", "completed", "hiatus", "cancelled"];
const DEMOS = ["shounen", "shoujo", "seinen", "josei", "none"];

/**
 * Many titles are listed with English chapters that are external publisher
 * links only (no readable pages). We verify each candidate has at least one
 * *readable* English chapter before showing it, so every result opens.
 */
async function hasReadableChapters(id: string): Promise<boolean> {
  try {
    const data = await md<{ total: number }>(
      `/chapter?limit=1&manga=${id}&translatedLanguage[]=en&includeExternalUrl=0&${RATING_PARAMS.join("&")}`,
    );
    return data.total > 0;
  } catch {
    // Optimistic on upstream hiccups — better than hiding a valid title.
    return true;
  }
}

async function keepReadable(items: MdManga[]): Promise<MdManga[]> {
  const out: MdManga[] = [];
  const CONCURRENCY = 8;
  for (let i = 0; i < items.length; i += CONCURRENCY) {
    const batch = items.slice(i, i + CONCURRENCY);
    const flags = await Promise.all(batch.map((m) => hasReadableChapters(m.id)));
    batch.forEach((m, j) => { if (flags[j]) out.push(m); });
  }
  return out;
}

const MAX_OFFSET = 9500;

/**
 * Cursor pagination: the client's cursor is an *upstream* offset, not a page
 * number. Because unreadable titles are dropped, a page of N results can
 * consume more than N upstream rows — we return the exact offset to resume
 * from so no title is skipped or shown twice.
 */
export const Route = createFileRoute("/api/public/md/list")({
  server: {
    handlers: {
      GET: async ({ request }) => {
        const url = new URL(request.url);
        const p = url.searchParams;
        const q = (p.get("q") ?? "").trim();
        const sortParam = p.get("sort") ?? "latest";
        const sort = q ? "relevance" : (ORDER[sortParam] ? sortParam : "latest");
        const csv = (k: string) => (p.get(k) ?? "").split(",").map((s) => s.trim()).filter(Boolean);
        const genres = csv("g");
        const excluded = csv("x");
        const status = csv("status").filter((s) => STATUSES.includes(s));
        const demos = csv("demo").filter((s) => DEMOS.includes(s));
        const yearFrom = Number(p.get("from") ?? 0) || 0;
        const yearTo = Number(p.get("to") ?? 0) || 0;
        const mode = p.get("mode") === "all" ? "AND" : "OR";
        const limit = Math.min(48, Math.max(1, Number(p.get("limit") ?? 24)));
        const cursor = Math.max(0, Math.min(MAX_OFFSET, Number(p.get("cursor") ?? 0)));

        const plainLatest =
          sort === "latest" && !q && !genres.length && !excluded.length && !status.length && !demos.length && !yearFrom && !yearTo;

        try {
          const picked: MdManga[] = [];
          let offset = cursor;
          let exhausted = false;

          if (plainLatest) {
            // Latest updates come straight from the readable-chapter feed.
            const ids: string[] = [];
            for (let round = 0; round < 3 && ids.length < limit && offset <= MAX_OFFSET; round++) {
              const feed = await md<{ data: Array<{ relationships: Array<{ type: string; id: string }> }>; total: number }>(
                `/chapter?limit=100&offset=${offset}&translatedLanguage[]=en&includeExternalUrl=0&order[readableAt]=desc&${RATING_PARAMS.join("&")}`,
              );
              for (const c of feed.data) {
                const mid = c.relationships.find((r) => r.type === "manga")?.id;
                if (mid && !ids.includes(mid)) ids.push(mid);
              }
              offset += feed.data.length;
              if (feed.data.length === 0 || offset >= feed.total) { exhausted = true; break; }
            }
            if (ids.length) {
              const slice = ids.slice(0, limit);
              const mangas = await md<{ data: MdManga[] }>(
                `/manga?limit=${slice.length}&includes[]=cover_art&includes[]=author&${RATING_PARAMS.join("&")}&${slice.map((id) => `ids[]=${id}`).join("&")}`,
              );
              const order = new Map(slice.map((id, i) => [id, i]));
              picked.push(...mangas.data.sort((a, b) => (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0)));
            }
          } else {
            const incIds = await tagIds(genres);
            const excIds = await tagIds(excluded);
            for (let round = 0; round < 3 && picked.length < limit && offset <= MAX_OFFSET; round++) {
              const fetchCount = Math.min(96, Math.max(24, (limit - picked.length) * 2 + 8));
              const parts = [
                `limit=${fetchCount}`,
                `offset=${offset}`,
                "availableTranslatedLanguage[]=en",
                "hasAvailableChapters=true",
                "includes[]=cover_art",
                "includes[]=author",
                ...RATING_PARAMS,
                ORDER[sort]!,
              ];
              if (q) parts.push(`title=${encodeURIComponent(q)}`);
              if (yearFrom) parts.push(`createdAtSince=${yearFrom}-01-01T00:00:00`);
              for (const s of status) parts.push(`status[]=${s}`);
              for (const d of demos) parts.push(`publicationDemographic[]=${d}`);
              if (incIds.length) {
                parts.push(`includedTagsMode=${mode}`);
                for (const id of incIds) parts.push(`includedTags[]=${id}`);
              }
              if (excIds.length) {
                parts.push("excludedTagsMode=OR");
                for (const id of excIds) parts.push(`excludedTags[]=${id}`);
              }
              const data = await md<{ data: MdManga[]; total: number }>(`/manga?${parts.join("&")}`);
              offset += data.data.length;
              let batch = data.data;
              if (yearFrom || yearTo) {
                batch = batch.filter((m) => {
                  const y = m.attributes.year ?? 0;
                  if (yearFrom && (!y || y < yearFrom)) return false;
                  if (yearTo && (!y || y > yearTo)) return false;
                  return true;
                });
              }
              const readable = await keepReadable(batch);
              for (const m of readable) {
                if (picked.length < limit && !picked.some((x) => x.id === m.id)) picked.push(m);
              }
              if (data.data.length === 0 || offset >= data.total) { exhausted = true; break; }
            }
          }

          const stats = await ratings(picked.map((m) => m.id));
          return json(
            {
              ok: true,
              items: picked.map((m) => toCard(m, stats[m.id] ?? null)),
              cursor,
              nextCursor: exhausted || offset > MAX_OFFSET ? null : offset,
              hasMore: !exhausted && offset <= MAX_OFFSET && picked.length > 0,
            },
            300,
          );
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          return json({ ok: false, error: msg, items: [], nextCursor: null, hasMore: false }, 0, 502);
        }
      },
    },
  },
});
