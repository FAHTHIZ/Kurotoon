import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { useEffect, useMemo, useRef, useState } from "react";
import { ArrowLeft, ExternalLink, Loader2, RefreshCcw, Search, X } from "lucide-react";
import { z } from "zod";
import { CardSkeletons, MangaCard } from "@/components/MangaCard";
import { SourceIcon } from "@/components/Cover";
import { KuroTip } from "@/components/Kuro";
import { spring } from "@/lib/motion";
import { useEngineReady, useSourceFeed } from "@/engine/hooks";
import { listListings } from "@/engine/runtime";
import { allowsRating } from "@/engine/store";
import type { Listing } from "@/engine/types";

export const Route = createFileRoute("/source/$sourceId")({
  validateSearch: z.object({ q: z.string().optional().catch(undefined), l: z.string().optional().catch(undefined) }),
  head: () => ({
    meta: [
      { title: "Source — KuroToon" },
      { name: "description", content: "Browse and search a single source." },
    ],
  }),
  component: SourcePage,
});

function SourcePage() {
  const { sourceId } = Route.useParams();
  const search = Route.useSearch();
  const navigate = Route.useNavigate();
  const engine = useEngineReady();
  const meta = engine.installed.find((s) => s.id === sourceId);
  const [q, setQ] = useState(search.q ?? "");

  const listings = useQuery({
    queryKey: ["src", sourceId, "listings"],
    queryFn: () => listListings(sourceId),
    enabled: typeof window !== "undefined" && !!meta,
    staleTime: 60 * 60_000,
    retry: 1,
  });

  const tabs = listings.data ?? [];
  const activeListing: Listing | null = search.q ? null : (tabs.find((l) => l.id === search.l) ?? tabs[0] ?? null);
  const mode = useMemo(
    () => (search.q || !activeListing ? { query: search.q ?? "" } : { listing: activeListing }),
    [search.q, activeListing],
  );

  if (engine.ready && !meta) {
    return (
      <div className="space-y-3">
        <KuroTip mood="sleepy">This source isn't installed on this device.</KuroTip>
        <Link to="/extensions" className="inline-block rounded-full bg-primary px-4 py-2 text-xs font-bold text-primary-foreground">
          Open Extensions
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Link to="/browse" aria-label="Back" className="grid h-9 w-9 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border">
          <ArrowLeft className="h-4 w-4" />
        </Link>
        {meta && <SourceIcon name={meta.name} url={meta.iconUrl} className="h-9 w-9" />}
        <div className="min-w-0 flex-1">
          <h1 className="truncate text-base font-black">{meta?.name ?? sourceId}</h1>
          {meta?.baseUrl && <p className="truncate text-[11px] text-muted-foreground">{new URL(meta.baseUrl).hostname}</p>}
        </div>
        {meta?.baseUrl && (
          <a
            href={meta.baseUrl}
            target="_blank"
            rel="noreferrer noopener"
            aria-label="Open website"
            className="grid h-9 w-9 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border"
          >
            <ExternalLink className="h-4 w-4" />
          </a>
        )}
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          navigate({ search: { q: q.trim() || undefined }, replace: true });
        }}
        className="flex items-center gap-2 rounded-full bg-[color:var(--surface)] px-3 py-2 ring-1 ring-border focus-within:ring-primary/60"
      >
        <Search className="h-4 w-4 text-muted-foreground" />
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          enterKeyHint="search"
          placeholder={`Search ${meta?.name ?? "source"}…`}
          className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
        {search.q && (
          <button
            type="button"
            aria-label="Clear search"
            onClick={() => {
              setQ("");
              navigate({ search: {}, replace: true });
            }}
            className="grid h-5 w-5 place-items-center rounded-full bg-white/10"
          >
            <X className="h-3 w-3" />
          </button>
        )}
      </form>

      {!search.q && tabs.length > 1 && (
        <div className="no-scrollbar -mx-3 flex gap-1.5 overflow-x-auto px-3">
          {tabs.map((l) => {
            const on = activeListing?.id === l.id;
            return (
              <motion.button
                key={l.id}
                whileTap={{ scale: 0.94 }}
                transition={spring.tap}
                onClick={() => navigate({ search: { l: l.id }, replace: true })}
                className={`relative shrink-0 rounded-full px-3.5 py-1.5 text-[11px] font-bold ${on ? "text-primary-foreground" : "bg-[color:var(--surface)] text-muted-foreground ring-1 ring-border"}`}
              >
                {on && <motion.span layoutId="kt-listing-tab" transition={spring.glide} className="absolute inset-0 -z-10 rounded-full bg-primary" />}
                {l.name || l.id}
              </motion.button>
            );
          })}
        </div>
      )}

      {meta && !listings.isLoading && <Feed key={JSON.stringify(mode)} sourceId={sourceId} mode={mode} />}
      {listings.isLoading && (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          <CardSkeletons />
        </div>
      )}
      {meta && !allowsRating(meta.contentRating, engine.prefs) && (
        <p className="text-center text-[11px] text-muted-foreground">This source is hidden by your content filters.</p>
      )}
    </div>
  );
}

function Feed({ sourceId, mode }: { sourceId: string; mode: { listing: Listing } | { query: string } }) {
  const { prefs } = useEngineReady();
  const feed = useSourceFeed(sourceId, mode);
  const sentinel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = sentinel.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && feed.hasNextPage && !feed.isFetchingNextPage) void feed.fetchNextPage();
      },
      { rootMargin: "800px 0px" },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [feed]);

  const seen = new Set<string>();
  const items = (feed.data?.pages ?? [])
    .flatMap((p) => p.items)
    .filter((m) => allowsRating(m.contentRating, prefs))
    .filter((m) => (seen.has(m.key) ? false : (seen.add(m.key), true)));

  if (feed.isLoading) {
    return (
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        <CardSkeletons />
      </div>
    );
  }
  if (feed.error) {
    return (
      <div className="kt-card space-y-3 rounded-2xl p-4 text-center">
        <p className="text-xs text-muted-foreground">
          This source failed: {feed.error instanceof Error ? feed.error.message : "unknown error"}
        </p>
        <button
          onClick={() => void feed.refetch()}
          className="inline-flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-xs font-bold text-primary-foreground"
        >
          <RefreshCcw className="h-3.5 w-3.5" /> Retry
        </button>
      </div>
    );
  }
  if (items.length === 0) return <KuroTip mood="sleepy">Nothing here.</KuroTip>;

  return (
    <>
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        {items.map((s, i) => (
          <MangaCard key={s.key} s={s} index={i % 24} />
        ))}
      </div>
      <div ref={sentinel} className="grid h-16 place-items-center">
        {feed.isFetchingNextPage && <Loader2 className="h-5 w-5 animate-spin text-primary" />}
      </div>
    </>
  );
}
