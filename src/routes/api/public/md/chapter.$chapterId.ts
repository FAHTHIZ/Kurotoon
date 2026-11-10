import { createFileRoute } from "@tanstack/react-router";
import { json, md, pick, type MdManga } from "@/lib/mangadex.server";

type Chapter = {
  id: string;
  attributes: { chapter: string | null; title: string | null; pages: number; translatedLanguage: string };
  relationships: Array<{ type: string; id: string; attributes?: Record<string, unknown> }>;
};

// Live chapter payload for the reader: metadata + fresh page URLs + sibling
// chapter list for prev/next. Zero database reads or writes.
export const Route = createFileRoute("/api/public/md/chapter/$chapterId")({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const id = params.chapterId;
        if (!/^[0-9a-f-]{36}$/i.test(id)) return json({ ok: false, error: "bad id" }, 0, 400);

        try {
          const [chRes, atRes] = await Promise.all([
            md<{ data: Chapter }>(`/chapter/${id}?includes[]=manga&includes[]=scanlation_group`),
            md<{ baseUrl: string; chapter: { hash: string; data: string[] } }>(`/at-home/server/${id}`),
          ]);
          const ch = chRes.data;
          const mangaId = ch.relationships.find((r) => r.type === "manga")?.id ?? null;

          let seriesTitle = "";
          if (mangaId) {
            try {
              const m = await md<{ data: MdManga }>(`/manga/${mangaId}`);
              seriesTitle = pick(m.data.attributes.title);
            } catch {
              /* title is cosmetic */
            }
          }

          // Sibling numbers for prev/next navigation (single feed page is enough
          // for nav; the series page holds the full list).
          let siblings: Array<{ id: string; number: string }> = [];
          if (mangaId && ch.attributes.chapter) {
            try {
              const feed = await md<{ data: Chapter[] }>(
                `/manga/${mangaId}/feed?limit=100&translatedLanguage[]=en&order[chapter]=asc&includeExternalUrl=0&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&offset=0`,
              );
              const seen = new Set<string>();
              siblings = feed.data
                .filter((c) => c.attributes.chapter && c.attributes.pages > 0)
                .filter((c) => {
                  const n = c.attributes.chapter!;
                  if (seen.has(n)) return false;
                  seen.add(n);
                  return true;
                })
                .map((c) => ({ id: c.id, number: c.attributes.chapter! }));
            } catch {
              /* nav degrades gracefully */
            }
          }

          const pages = atRes.chapter.data.map((file, i) => ({
            page_number: i + 1,
            image_url: `${atRes.baseUrl}/data/${atRes.chapter.hash}/${file}`,
          }));

          return json(
            {
              ok: true,
              chapter: {
                id: ch.id,
                number: ch.attributes.chapter,
                title: ch.attributes.title,
                series_id: mangaId,
                series_title: seriesTitle,
                group: (ch.relationships.find((r) => r.type === "scanlation_group")?.attributes?.["name"] as string) ?? null,
              },
              siblings,
              pages,
            },
            // Page tokens expire ~15 min upstream; keep edge cache short.
            120,
          );
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          return json({ ok: false, error: msg, pages: [] }, 0, 502);
        }
      },
    },
  },
});
