import { createFileRoute } from "@tanstack/react-router";
import { relay } from "@/server/relay";

export const Route = createFileRoute("/api/relay")({
  server: {
    handlers: {
      GET: ({ request }) => relay(request),
      POST: ({ request }) => relay(request),
      HEAD: ({ request }) => relay(request),
    },
  },
});
