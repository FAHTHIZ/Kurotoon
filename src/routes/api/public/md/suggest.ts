import { createFileRoute } from "@tanstack/react-router";
import { CONTENT_RATINGS, coverUrl, json, md, pick, type MdManga } from "@/lib/mangadex.server";

const RATING_PARAMS = CONTENT_RATINGS.map((r) => `contentRating[]=${r}`);

async function readable(id: string): Promise<boolean> {
  try {
    const d = await md<{ total: number }>(
      `/chapter?limit=1&manga=${id}&translatedLanguage[]=en&includeExternalUrl=0&${RATING_PARAMS.join("&")}`,
    );
    return d.total > 0;
  } catch {
    return true;
  }
}

/** Typeahead: only suggests titles the reader can actually open. */
export const Route = createFileRoute("/api/public/md/suggest")({
  server: {
    handlers: {
      GET: async ({ request }) => {
        const q = (new URL(request.url).searchParams.get("q") ?? "").trim();
        if (q.length < 2) return json({ ok: true, items: [] }, 60);
        try {
          const data = await md<{ data: MdManga[] }>(
            `/manga?limit=20&title=${encodeURIComponent(q)}&availableTranslatedLanguage[]=en&hasAvailableChapters=true&order[relevance]=desc&includes[]=cover_art&${RATING_PARAMS.join("&")}`,
          );
          // MangaDex relevance is fuzzy; re-rank so literal prefix/substring
          // matches (across main + alt titles) surface first.
          const needle = q.toLowerCase();
          const score = (m: MdManga) => {
            const names = [pick(m.attributes.title), ...(m.attributes.altTitles ?? []).map(pick)]
              .filter(Boolean)
              .map((s) => s.toLowerCase());
            if (names.some((n) => n === needle)) return 0;
            if (names.some((n) => n.startsWith(needle))) return 1;
            if (names.some((n) => n.includes(needle))) return 2;
            return 3;
          };
          const ranked = data.data
            .map((m) => ({ m, s: score(m) }))
            .sort((a, b) => a.s - b.s)
            .slice(0, 10)
            .map((r) => r.m);
          const flags = await Promise.all(ranked.map((m) => readable(m.id)));
          const items = ranked
            .filter((_, i) => flags[i])
            .slice(0, 6)
            .map((m) => ({
              id: m.id,
              title: pick(m.attributes.title) || "Untitled",
              cover_url: coverUrl(m, 256),
              year: m.attributes.year,
              status: m.attributes.status,
            }));

          return json({ ok: true, items }, 300);
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          return json({ ok: false, error: msg, items: [] }, 0, 502);
        }
      },
    },
  },
});
