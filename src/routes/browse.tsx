import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { MangaCard } from "@/components/MangaCard";
import { useEffect, useMemo, useState } from "react";
import { z } from "zod";
import { ChevronDown, X, SlidersHorizontal } from "lucide-react";
import { listSeries, listTags } from "@/lib/catalog";
import { SourceStrip } from "@/components/SourceStrip";

const sortValues = ["latest", "popular", "rating", "title", "year"] as const;
type Sort = (typeof sortValues)[number];

const STATUSES = ["ongoing", "completed", "hiatus", "cancelled"] as const;
const DEMOS = ["shounen", "shoujo", "seinen", "josei"] as const;

const searchSchema = z.object({
  q: z.string().optional().catch(undefined),
  g: z.string().optional().catch(undefined),
  x: z.string().optional().catch(undefined),
  status: z.string().optional().catch(undefined),
  demo: z.string().optional().catch(undefined),
  from: z.number().optional().catch(undefined),
  to: z.number().optional().catch(undefined),
  sort: z.enum(sortValues).optional().catch(undefined),
  mode: z.enum(["any", "all"]).optional().catch(undefined),
});

export const Route = createFileRoute("/browse")({
  validateSearch: searchSchema,
  head: () => ({
    meta: [
      { title: "Browse manga & manhwa — KuroToon" },
      { name: "description", content: "Search and filter thousands of manga, manhwa and doujin by genre, status, year, popularity and rating." },
      { property: "og:title", content: "Browse manga & manhwa — KuroToon" },
      { property: "og:description", content: "Search and filter thousands of readable titles by genre, status, year and rating." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Browse,
});

function parseList(v?: string): string[] {
  if (!v) return [];
  return v.split(",").map((s) => s.trim()).filter(Boolean);
}

const PAGE = 24;

function Browse() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/browse" });

  const [q, setQ] = useState(search.q ?? "");
  const selectedGenres = useMemo(() => parseList(search.g), [search.g]);
  const excludedGenres = useMemo(() => parseList(search.x), [search.x]);
  const statuses = useMemo(() => parseList(search.status), [search.status]);
  const demos = useMemo(() => parseList(search.demo), [search.demo]);
  const sort: Sort = search.sort ?? "latest";
  const mode: "any" | "all" = search.mode ?? "any";

  const [showAllGenres, setShowAllGenres] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);

  // Cursor stack: each entry is the upstream offset a page started at.
  const [cursors, setCursors] = useState<number[]>([0]);
  const page = cursors.length - 1;
  const cursor = cursors[page] ?? 0;

  const filterKey = JSON.stringify([search.q, search.g, search.x, search.status, search.demo, search.from, search.to, search.sort, search.mode]);
  useEffect(() => { setCursors([0]); }, [filterKey]);

  useEffect(() => {
    const t = setTimeout(() => {
      if ((search.q ?? "") === q) return;
      navigate({
        search: (prev: Record<string, unknown>) => ({ ...prev, q: q || undefined }),
        replace: true,
      });
    }, 350);
    return () => clearTimeout(t);
  }, [q, search.q, navigate]);

  const { data: tagList } = useQuery({
    queryKey: ["md", "tags"],
    staleTime: 60 * 60_000,
    queryFn: listTags,
  });

  const { data, isFetching } = useQuery({
    queryKey: ["md", "browse", filterKey, cursor],
    placeholderData: keepPreviousData,
    staleTime: 2 * 60_000,
    queryFn: () =>
      listSeries({
        sort,
        q: search.q,
        genres: selectedGenres,
        excluded: excludedGenres,
        status: statuses,
        demos,
        yearFrom: search.from,
        yearTo: search.to,
        mode,
        limit: PAGE,
        cursor,
      }),
  });

  const items = data?.items ?? [];
  const hasMore = !!data?.hasMore && data.nextCursor !== null;

  const patch = (next: Record<string, unknown>) =>
    navigate({ search: (prev: Record<string, unknown>) => ({ ...prev, ...next }), replace: true });

  /** Click cycles a genre: off → include → exclude → off. */
  const cycleGenre = (g: string) => {
    if (selectedGenres.includes(g)) {
      patch({
        g: selectedGenres.filter((x) => x !== g).join(",") || undefined,
        x: [...excludedGenres, g].join(","),
      });
    } else if (excludedGenres.includes(g)) {
      patch({ x: excludedGenres.filter((x) => x !== g).join(",") || undefined });
    } else {
      patch({ g: [...selectedGenres, g].join(",") });
    }
  };

  const toggleIn = (list: string[], value: string, key: string) =>
    patch({ [key]: (list.includes(value) ? list.filter((v) => v !== value) : [...list, value]).join(",") || undefined });

  const clearAll = () =>
    patch({ g: undefined, x: undefined, status: undefined, demo: undefined, from: undefined, to: undefined });

  const genres = useMemo(
    () => (tagList ?? []).filter((t) => t.group === "genre" || t.group === "theme"),
    [tagList],
  );
  const visibleGenres = showAllGenres ? genres : genres.slice(0, 18);
  const activeCount =
    selectedGenres.length + excludedGenres.length + statuses.length + demos.length + (search.from ? 1 : 0) + (search.to ? 1 : 0);

  return (
    <div className="space-y-4">
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Search by title…"
        className="w-full rounded-xl bg-[color:var(--surface)] px-4 py-3 text-sm outline-none ring-1 ring-border focus:ring-primary"
      />

      <SourceStrip />



      <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
        {sortValues.map((s) => (
          <button
            key={s}
            onClick={() => patch({ sort: s === "latest" ? undefined : s })}
            data-burst
            disabled={!!search.q}
            title={search.q ? "Search results are ordered by relevance" : undefined}
            className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold capitalize transition disabled:opacity-40 ${sort === s && !search.q ? "bg-primary text-primary-foreground" : "bg-[color:var(--surface)] text-muted-foreground"}`}
          >{s}</button>
        ))}
        <div className="ml-auto inline-flex shrink-0 overflow-hidden rounded-full ring-1 ring-border">
          {(["any", "all"] as const).map((m) => (
            <button
              key={m}
              onClick={() => patch({ mode: m === "any" ? undefined : m })}
              data-burst
              title={m === "all" ? "Match every selected genre" : "Match any selected genre"}
              className={`px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider transition ${mode === m ? "bg-primary text-primary-foreground" : "bg-[color:var(--surface)] text-muted-foreground"}`}
            >
              {m}
            </button>
          ))}
        </div>
      </div>

      {(selectedGenres.length > 0 || excludedGenres.length > 0) && (
        <div className="flex flex-wrap items-center gap-1.5">
          {selectedGenres.map((g) => (
            <button
              key={g}
              onClick={() => cycleGenre(g)}
              data-burst
              className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2.5 py-1 text-[11px] font-semibold text-primary ring-1 ring-primary/40 hover:bg-primary/25"
            >
              {g}
              <X className="h-3 w-3" />
            </button>
          ))}
          {excludedGenres.map((g) => (
            <button
              key={g}
              onClick={() => cycleGenre(g)}
              data-burst
              className="inline-flex items-center gap-1 rounded-full bg-destructive/15 px-2.5 py-1 text-[11px] font-semibold text-destructive ring-1 ring-destructive/40 line-through hover:bg-destructive/25"
            >
              {g}
              <X className="h-3 w-3" />
            </button>
          ))}
          <button onClick={clearAll} className="text-[11px] text-muted-foreground underline-offset-4 hover:text-foreground hover:underline">
            Clear all
          </button>
        </div>
      )}

      <div className="flex flex-wrap gap-1.5">
        <button
          onClick={clearAll}
          data-burst
          className={`rounded-full px-3 py-1 text-xs transition ${activeCount === 0 ? "bg-primary/15 text-primary ring-1 ring-primary/40" : "bg-[color:var(--surface)] text-muted-foreground hover:bg-[color:var(--surface-2)]"}`}
        >
          All
        </button>
        {visibleGenres.map((t) => {
          const included = selectedGenres.includes(t.name);
          const excluded = excludedGenres.includes(t.name);
          return (
            <button
              key={t.id}
              onClick={() => cycleGenre(t.name)}
              data-burst
              title="Tap to include, tap again to exclude"
              className={`rounded-full px-3 py-1 text-xs transition ${
                included
                  ? "bg-primary/15 text-primary ring-1 ring-primary/40"
                  : excluded
                    ? "bg-destructive/15 text-destructive line-through ring-1 ring-destructive/40"
                    : "bg-[color:var(--surface)] text-muted-foreground hover:bg-[color:var(--surface-2)]"
              }`}
            >
              {t.name}
            </button>
          );
        })}
        {genres.length > 18 && (
          <button
            onClick={() => setShowAllGenres((v) => !v)}
            className="inline-flex items-center gap-1 rounded-full bg-[color:var(--surface)] px-3 py-1 text-xs text-muted-foreground ring-1 ring-border hover:bg-[color:var(--surface-2)]"
          >
            {showAllGenres ? "Show less" : `+${genres.length - 18} more`}
            <ChevronDown className={`h-3 w-3 transition-transform ${showAllGenres ? "rotate-180" : ""}`} />
          </button>
        )}
      </div>

      <button
        onClick={() => setShowAdvanced((v) => !v)}
        className="inline-flex items-center gap-1.5 rounded-full bg-[color:var(--surface)] px-3 py-1.5 text-xs font-semibold text-muted-foreground ring-1 ring-border hover:bg-[color:var(--surface-2)]"
      >
        <SlidersHorizontal className="h-3.5 w-3.5" />
        More filters
        {activeCount > 0 && <span className="rounded-full bg-primary px-1.5 text-[10px] text-primary-foreground">{activeCount}</span>}
      </button>

      {showAdvanced && (
        <div className="space-y-3 rounded-xl bg-[color:var(--surface)] p-3 ring-1 ring-border">
          <div>
            <p className="mb-1.5 text-[10px] font-bold uppercase tracking-wider text-muted-foreground">Status</p>
            <div className="flex flex-wrap gap-1.5">
              {STATUSES.map((s) => (
                <button
                  key={s}
                  onClick={() => toggleIn(statuses, s, "status")}
                  className={`rounded-full px-3 py-1 text-xs capitalize transition ${statuses.includes(s) ? "bg-primary text-primary-foreground" : "bg-[color:var(--surface-2)] text-muted-foreground"}`}
                >{s}</button>
              ))}
            </div>
          </div>
          <div>
            <p className="mb-1.5 text-[10px] font-bold uppercase tracking-wider text-muted-foreground">Demographic</p>
            <div className="flex flex-wrap gap-1.5">
              {DEMOS.map((d) => (
                <button
                  key={d}
                  onClick={() => toggleIn(demos, d, "demo")}
                  className={`rounded-full px-3 py-1 text-xs capitalize transition ${demos.includes(d) ? "bg-primary text-primary-foreground" : "bg-[color:var(--surface-2)] text-muted-foreground"}`}
                >{d}</button>
              ))}
            </div>
          </div>
          <div>
            <p className="mb-1.5 text-[10px] font-bold uppercase tracking-wider text-muted-foreground">Year</p>
            <div className="flex items-center gap-2">
              <input
                type="number"
                inputMode="numeric"
                placeholder="From"
                value={search.from ?? ""}
                onChange={(e) => patch({ from: Number(e.target.value) || undefined })}
                className="w-24 rounded-lg bg-[color:var(--surface-2)] px-2.5 py-1.5 text-xs outline-none ring-1 ring-border focus:ring-primary"
              />
              <span className="text-xs text-muted-foreground">—</span>
              <input
                type="number"
                inputMode="numeric"
                placeholder="To"
                value={search.to ?? ""}
                onChange={(e) => patch({ to: Number(e.target.value) || undefined })}
                className="w-24 rounded-lg bg-[color:var(--surface-2)] px-2.5 py-1.5 text-xs outline-none ring-1 ring-border focus:ring-primary"
              />
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        {items.length === 0 && isFetching
          ? Array.from({ length: PAGE }).map((_, i) => (
              <div key={i} className="aspect-[2/3] animate-pulse rounded-lg bg-[color:var(--surface)] ring-1 ring-border" />
            ))
          : items.map((s, i) => <MangaCard key={s.id} s={s} index={i} />)}
      </div>

      {!isFetching && items.length === 0 && (
        <p className="py-12 text-center text-sm text-muted-foreground">No readable series match your filters.</p>
      )}

      {(page > 0 || hasMore) && (
        <div className="flex items-center justify-center gap-3 pt-2">
          <button
            disabled={page === 0 || isFetching}
            onClick={() => setCursors((c) => c.slice(0, -1))}
            className="rounded-full bg-[color:var(--surface)] px-4 py-2 text-xs font-semibold ring-1 ring-border disabled:opacity-40"
          >
            ← Prev
          </button>
          <span className="text-xs text-muted-foreground">Page {page + 1}</span>
          <button
            disabled={!hasMore || isFetching}
            onClick={() => setCursors((c) => (data?.nextCursor != null ? [...c, data.nextCursor] : c))}
            className="rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground disabled:opacity-40"
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
