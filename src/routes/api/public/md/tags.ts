import { createFileRoute } from "@tanstack/react-router";
import { json, tags } from "@/lib/mangadex.server";

export const Route = createFileRoute("/api/public/md/tags")({
  server: {
    handlers: {
      GET: async () => {
        try {
          return json({ ok: true, tags: await tags() }, 3600);
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          return json({ ok: false, error: msg, tags: [] }, 0, 502);
        }
      },
    },
  },
});
