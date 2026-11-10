import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Bookmark, BookmarkCheck, Star, Play, Radio } from "lucide-react";
import { toast } from "sonner";
import { useEffect, useState } from "react";
import { proxied } from "@/lib/image";
import { getSeries } from "@/lib/catalog";
import { getProgress, isBookmarked, toggleBookmark } from "@/lib/local-library";

export const Route = createFileRoute("/series/$slug")({
  head: ({ params }) => ({
    meta: [
      { title: "Series — KuroToon" },
      { name: "description", content: "Chapter list, details and instant reading for this series on KuroToon." },
      { property: "og:title", content: "Series — KuroToon" },
      { property: "og:description", content: "Chapter list, details and instant reading, streamed live." },
      { property: "og:type", content: "article" },
      { name: "twitter:card", content: "summary_large_image" },
      { name: "series-id", content: params.slug },
    ],
  }),
  component: SeriesPage,
});

function SeriesPage() {
  const { slug } = Route.useParams();

  const { data, isLoading, error } = useQuery({
    queryKey: ["md", "series", slug],
    queryFn: () => getSeries(slug),
    staleTime: 3 * 60_000,
  });

  const series = data?.series;
  const chapters = data?.chapters;

  const [saved, setSaved] = useState(false);
  useEffect(() => { setSaved(isBookmarked(slug)); }, [slug]);

  const progress = typeof window !== "undefined" ? getProgress(slug) : null;

  if (isLoading) {
    return <div className="py-20 text-center text-sm text-muted-foreground">Loading series…</div>;
  }
  if (error || !series) {
    return (
      <div className="py-20 text-center">
        <p className="text-sm text-muted-foreground">Couldn’t load this series right now.</p>
        <Link to="/browse" className="mt-4 inline-block rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground">Back to browse</Link>
      </div>
    );
  }

  const startChapter =
    (progress && chapters?.find((c) => c.id === progress.chapterId)) ??
    chapters?.[chapters.length - 1];

  const onBookmark = () => {
    const nowSaved = toggleBookmark({
      id: series.id,
      slug: series.slug,
      title: series.title,
      cover_url: series.cover_url,
      rating: series.rating,
      genres: series.genres,
    });
    setSaved(nowSaved);
    toast.success(nowSaved ? "Added to library" : "Removed from library");
  };

  return (
    <article className="space-y-6">
      <div className="relative -mx-3 overflow-hidden md:-mx-6 md:rounded-2xl">
        <div className="absolute inset-0 -z-10">
          <img src={proxied(series.cover_url)} alt="" referrerPolicy="no-referrer" className="h-full w-full object-cover opacity-25 blur-2xl" />
          <div className="absolute inset-0 bg-gradient-to-b from-transparent to-background" />
        </div>
        <div className="flex gap-4 p-4 md:p-6">
          <img src={proxied(series.cover_url)} alt={series.title} referrerPolicy="no-referrer" className="h-44 w-32 shrink-0 rounded-xl object-cover ring-1 ring-border md:h-64 md:w-44" />
          <div className="flex flex-col justify-end">
            <h1 className="text-xl font-extrabold leading-tight md:text-3xl">{series.title}</h1>
            <p className="mt-1 text-xs text-muted-foreground">
              {series.author ?? "Unknown author"}
              {series.artist && series.artist !== series.author ? ` • art ${series.artist}` : ""}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-3 text-xs">
              {series.rating ? (
                <span className="inline-flex items-center gap-1 font-semibold"><Star className="h-3.5 w-3.5 fill-primary text-primary" />{series.rating.toFixed(2)}</span>
              ) : null}
              <span className="rounded-full bg-[color:var(--surface)] px-2 py-0.5 capitalize">{series.status}</span>
              {series.year ? <span className="text-muted-foreground">{series.year}</span> : null}
              <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-emerald-400 ring-1 ring-emerald-500/30">
                <Radio className="h-3 w-3" /> Live · {chapters?.length ?? 0} ch
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {startChapter && (
          <Link
            to="/read/$chapterId"
            params={{ chapterId: startChapter.id }}
            className="inline-flex items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)]"
          >
            <Play className="h-4 w-4" />
            {progress ? `Continue Ch. ${startChapter.number}` : `Start Ch. ${startChapter.number}`}
          </Link>
        )}
        <button
          onClick={onBookmark}
          className="inline-flex items-center gap-2 rounded-full bg-[color:var(--surface)] px-4 py-2 text-sm font-semibold ring-1 ring-border hover:bg-[color:var(--surface-2)]"
        >
          {saved ? <BookmarkCheck className="h-4 w-4 text-primary" /> : <Bookmark className="h-4 w-4" />}
          {saved ? "In Library" : "Bookmark"}
        </button>
      </div>

      <div className="flex flex-wrap gap-1.5">
        {series.genres.map((g) => (
          <span key={g} className="rounded-full bg-[color:var(--surface)] px-2.5 py-0.5 text-[11px] text-muted-foreground ring-1 ring-border">{g}</span>
        ))}
      </div>

      {series.description && <p className="whitespace-pre-line text-sm leading-relaxed text-muted-foreground">{series.description}</p>}

      <section>
        <h2 className="mb-2 text-sm font-bold uppercase tracking-wider text-muted-foreground">Chapters</h2>
        {chapters && chapters.length > 0 ? (
          <ul className="overflow-hidden rounded-xl ring-1 ring-border">
            {chapters.map((c) => (
              <li key={c.id}>
                <Link
                  to="/read/$chapterId"
                  params={{ chapterId: c.id }}
                  className="flex items-center justify-between gap-3 border-b border-border bg-[color:var(--surface)] px-4 py-3 text-sm last:border-b-0 hover:bg-[color:var(--surface-2)]"
                >
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold">Chapter {c.number}</p>
                    {(c.title || c.group) && (
                      <p className="truncate text-xs text-muted-foreground">
                        {c.title}{c.title && c.group ? " • " : ""}{c.group}
                      </p>
                    )}
                  </div>
                  <span className="shrink-0 text-[11px] text-muted-foreground">{c.pages} pages</span>
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className="rounded-xl bg-[color:var(--surface)] p-6 text-center text-sm text-muted-foreground ring-1 ring-border">
            No English chapters available for this title yet.
          </p>
        )}
      </section>
    </article>
  );
}
