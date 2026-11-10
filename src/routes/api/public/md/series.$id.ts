import { createFileRoute } from "@tanstack/react-router";
import { coverUrl, json, md, pick, ratings, relName, type MdManga } from "@/lib/mangadex.server";

type FeedItem = {
  id: string;
  attributes: { chapter: string | null; title: string | null; pages: number; publishAt: string; translatedLanguage: string };
  relationships: Array<{ type: string; attributes?: Record<string, unknown> }>;
};

export const Route = createFileRoute("/api/public/md/series/$id")({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const id = params.id;
        if (!/^[0-9a-f-]{36}$/i.test(id)) return json({ ok: false, error: "bad id" }, 0, 400);

        try {
          const [detail, stats] = await Promise.all([
            md<{ data: MdManga }>(`/manga/${id}?includes[]=cover_art&includes[]=author&includes[]=artist`),
            ratings([id]),
          ]);
          const m = detail.data;

          // Chapter feed — paginate live, dedupe by chapter number.
          const seen = new Map<string, { id: string; number: string; title: string | null; pages: number; publishAt: string; group: string | null }>();
          let offset = 0;
          for (let i = 0; i < 6; i++) {
            const feed = await md<{ data: FeedItem[]; total: number }>(
              `/manga/${id}/feed?limit=100&offset=${offset}&translatedLanguage[]=en&order[chapter]=desc&includeExternalUrl=0&includes[]=scanlation_group&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica`,
            );
            for (const c of feed.data) {
              const num = c.attributes.chapter;
              if (!num || c.attributes.pages <= 0) continue;
              if (seen.has(num)) continue;
              seen.set(num, {
                id: c.id,
                number: num,
                title: c.attributes.title,
                pages: c.attributes.pages,
                publishAt: c.attributes.publishAt,
                group: (c.relationships.find((r) => r.type === "scanlation_group")?.attributes?.["name"] as string) ?? null,
              });
            }
            offset += 100;
            if (offset >= feed.total) break;
          }

          const chapters = [...seen.values()].sort((a, b) => Number(b.number) - Number(a.number));

          return json(
            {
              ok: true,
              series: {
                id: m.id,
                slug: m.id,
                title: pick(m.attributes.title) || "Untitled",
                description: pick(m.attributes.description),
                author: relName(m, "author"),
                artist: relName(m, "artist"),
                cover_url: coverUrl(m),
                status: m.attributes.status,
                year: m.attributes.year,
                rating: stats[id] ?? null,
                genres: m.attributes.tags.map((t) => pick(t.attributes.name)).filter(Boolean),
              },
              chapters,
            },
            180,
          );
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          return json({ ok: false, error: msg }, 0, 502);
        }
      },
    },
  },
});
