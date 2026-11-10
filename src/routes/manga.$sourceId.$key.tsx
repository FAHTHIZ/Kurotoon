import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import { ArrowDownUp, ArrowLeft, Bookmark, BookmarkCheck, CheckCheck, ExternalLink, Heart, Lock, Play, RefreshCcw, Tags, Download } from "lucide-react";
import { toast } from "sonner";
import { Cover } from "@/components/Cover";
import { KuroTip } from "@/components/Kuro";
import { spring } from "@/lib/motion";
import {
  chapterLabel,
  getProgress,
  getReadChapters,
  getSeen,
  isBookmarked,
  isFavourite,
  markChapterRead,
  rememberSeries,
  toggleBookmark,
  toggleFavourite,
} from "@/lib/local-library";
import { useChapters, useEngineReady, useMangaDetails } from "@/engine/hooks";
import { decodeKey, encodeKey, seriesId } from "@/engine/ids";
import { STATUS_LABEL, type ChapterRow, type SeriesCard } from "@/engine/types";
import { seedKnownChapters } from "@/lib/updates";
import { getCategories } from "@/lib/categories";
import { enqueueDownloads } from "@/lib/downloads";
import { DownloadButton } from "@/components/DownloadButton";
import { CategoryPicker, Sheet } from "@/components/LibraryShelf";

export const Route = createFileRoute("/manga/$sourceId/$key")({
  head: () => ({
    meta: [
      { title: "Series — KuroToon" },
      { name: "description", content: "Series details and chapters." },
    ],
  }),
  component: MangaPage,
});

function useLocalTick() {
  const [t, setT] = useState(0);
  useEffect(() => {
    const on = () => setT((x) => x + 1);
    window.addEventListener("kt-local-change", on);
    return () => window.removeEventListener("kt-local-change", on);
  }, []);
  return t;
}

function MangaPage() {
  const { sourceId, key: encoded } = Route.useParams();
  const key = useMemo(() => decodeKey(encoded), [encoded]);
  const id = seriesId(sourceId, key);
  const engine = useEngineReady();
  const meta = engine.installed.find((s) => s.id === sourceId);
  const details = useMangaDetails(sourceId, key);
  const chapters = useChapters(sourceId, key);
  const tick = useLocalTick();
  const [desc, setDesc] = useState(false);
  const [asc, setAsc] = useState(false);

  const seen = useMemo(() => (typeof window === "undefined" ? null : getSeen(id)), [id]);
  const card: SeriesCard | null = details.data ?? seen;

  useEffect(() => {
    if (details.data) rememberSeries(details.data);
  }, [details.data]);

  const [saved, setSaved] = useState(false);
  const [pickCats, setPickCats] = useState(false);
  const [fav, setFav] = useState(false);
  useEffect(() => {
    setSaved(isBookmarked(id));
    setFav(isFavourite(id));
  }, [id, tick]);

  const progress = useMemo(() => (typeof window === "undefined" ? null : getProgress(id)), [id, tick]);
  const readSet = useMemo(() => new Set(typeof window === "undefined" ? [] : getReadChapters(id)), [id, tick]);

  const list = chapters.data ?? [];
  const ordered = asc ? [...list].reverse() : list;
  const resume = (progress && list.find((c) => c.key === progress.chapterKey)) ?? null;
  // Sources list newest first (Aidoku/Mihon convention): first unread from the end.
  const firstUnread = [...list].reverse().find((c) => !readSet.has(c.key) && !c.locked) ?? null;
  const start = resume ?? firstUnread ?? list[list.length - 1] ?? null;

  const readParams = (c: ChapterRow) => ({ sourceId, mangaKey: encoded, chapterKey: encodeKey(c.key) });

  if (engine.ready && !meta) {
    return (
      <div className="space-y-3">
        <KuroTip mood="sleepy">The source for this series isn't installed on this device.</KuroTip>
        <Link to="/extensions" className="inline-block rounded-full bg-primary px-4 py-2 text-xs font-bold text-primary-foreground">
          Open Extensions
        </Link>
      </div>
    );
  }

  return (
    <article className="space-y-5">
      <Sheet open={pickCats} onClose={() => setPickCats(false)} title="Categories">
        <CategoryPicker seriesId={id} onClose={() => setPickCats(false)} />
      </Sheet>
      <div className="relative -mx-3 overflow-hidden md:-mx-6 md:rounded-3xl">
        <div className="absolute inset-0 -z-10">
          {card && (
            <div className="absolute inset-0 scale-110 opacity-30 blur-2xl">
              <Cover sourceId={sourceId} url={card.cover} alt="" />
            </div>
          )}
          <div className="absolute inset-0 bg-gradient-to-b from-transparent to-background" />
        </div>
        <div className="px-4 pt-2 md:px-6">
          <button
            onClick={() => history.back()}
            aria-label="Back"
            className="grid h-9 w-9 place-items-center rounded-full bg-black/30 ring-1 ring-white/10 backdrop-blur"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
        </div>
        <div className="flex gap-4 p-4 md:p-6">
          <motion.div
            initial={{ opacity: 0, y: 10, rotate: -1 }}
            animate={{ opacity: 1, y: 0, rotate: 0 }}
            transition={spring.surface}
            className="relative h-44 w-32 shrink-0 overflow-hidden rounded-xl ring-1 ring-border shadow-[var(--shadow-depth-3)] md:h-64 md:w-44"
          >
            {card ? <Cover sourceId={sourceId} url={card.cover} alt={card.title} eager /> : <span className="kt-shimmer absolute inset-0" />}
          </motion.div>
          <div className="flex min-w-0 flex-col justify-end">
            {card ? (
              <h1 className="text-xl font-extrabold leading-tight md:text-3xl">{card.title}</h1>
            ) : (
              <div className="kt-shimmer h-6 w-48 rounded" />
            )}
            {card && card.authors.length > 0 && <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{card.authors.join(", ")}</p>}
            <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px]">
              {card?.status != null && card.status > 0 && (
                <span className="rounded-full bg-[color:var(--surface)] px-2 py-0.5">{STATUS_LABEL[card.status]}</span>
              )}
              {meta && (
                <Link to="/source/$sourceId" params={{ sourceId }} className="rounded-full bg-primary/15 px-2 py-0.5 font-semibold text-primary ring-1 ring-primary/25">
                  {meta.name}
                </Link>
              )}
              {card && card.contentRating > 0 && (
                <span className="rounded-full bg-[color:var(--brand-crimson,#e11d48)]/15 px-2 py-0.5 font-bold text-rose-300">
                  {card.contentRating === 2 ? "18+" : "16+"}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {start && (
          <Link to="/read/$sourceId/$mangaKey/$chapterKey" params={readParams(start)}>
            <motion.span
              whileTap={{ scale: 0.95 }}
              transition={spring.tap}
              className="inline-flex items-center gap-2 rounded-full bg-[image:var(--gradient-accent)] px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
            >
              <Play className="h-4 w-4" />
              {resume ? `Continue ${chapterLabel(resume)}` : `Start ${chapterLabel(start)}`}
            </motion.span>
          </Link>
        )}
        <motion.button
          whileTap={{ scale: 0.95 }}
          transition={spring.tap}
          disabled={!card}
          onClick={() => {
            if (!card) return;
            const now = toggleBookmark(card);
            setSaved(now);
            if (now && chapters.data) seedKnownChapters(id, chapters.data.map((c) => c.key));
            if (now && getCategories().length) setPickCats(true);
            toast.success(now ? "Added to library" : "Removed from library");
          }}
          className="inline-flex items-center gap-2 rounded-full bg-[color:var(--surface)] px-4 py-2.5 text-sm font-semibold ring-1 ring-border"
        >
          {saved ? <BookmarkCheck className="h-4 w-4 text-primary" /> : <Bookmark className="h-4 w-4" />}
          {saved ? "In library" : "Add to library"}
        </motion.button>
        {saved && (
          <motion.button
            whileTap={{ scale: 0.9 }}
            transition={spring.tap}
            onClick={() => setPickCats(true)}
            aria-label="Set categories"
            className="grid h-10 w-10 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border"
          >
            <Tags className="h-4 w-4" />
          </motion.button>
        )}
        <motion.button
          whileTap={{ scale: 0.85 }}
          transition={spring.tap}
          disabled={!card}
          aria-label={fav ? "Remove favourite" : "Favourite"}
          onClick={() => card && setFav(toggleFavourite(card))}
          className="grid h-10 w-10 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border"
        >
          <Heart className={`h-4 w-4 ${fav ? "fill-rose-400 text-rose-400" : ""}`} />
        </motion.button>
        {details.data?.url && (
          <a
            href={details.data.url}
            target="_blank"
            rel="noreferrer noopener"
            aria-label="Open on website"
            className="grid h-10 w-10 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border"
          >
            <ExternalLink className="h-4 w-4" />
          </a>
        )}
      </div>

      {card && card.tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {card.tags.map((g) => (
            <span key={g} className="rounded-full bg-[color:var(--surface)] px-2.5 py-0.5 text-[11px] text-muted-foreground ring-1 ring-border">
              {g}
            </span>
          ))}
        </div>
      )}

      {details.isLoading ? (
        <div className="space-y-1.5">
          <div className="kt-shimmer h-3 rounded" />
          <div className="kt-shimmer h-3 rounded" />
          <div className="kt-shimmer h-3 w-2/3 rounded" />
        </div>
      ) : details.data?.description ? (
        <button onClick={() => setDesc((d) => !d)} className="block w-full text-left">
          <p className={`whitespace-pre-line text-sm leading-relaxed text-muted-foreground ${desc ? "" : "line-clamp-4"}`}>
            {details.data.description}
          </p>
        </button>
      ) : details.error ? (
        <p className="text-xs text-destructive">Details failed: {details.error instanceof Error ? details.error.message : "error"}</p>
      ) : null}

      <section>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
            {chapters.isLoading ? "Chapters" : `${list.length} chapters`}
          </h2>
          <div className="flex gap-1">
            <button
              onClick={() => {
                if (!card) return;
                const unread = [...list].reverse().filter((c) => !readSet.has(c.key) && !c.locked).slice(0, 5);
                const n = enqueueDownloads(card, unread);
                toast.success(n ? `Downloading ${n} chapter${n === 1 ? "" : "s"}` : "Next unread chapters are already downloaded");
              }}
              disabled={!card || !list.length}
              aria-label="Download next 5 unread"
              title="Download next 5 unread"
              className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border disabled:opacity-40"
            >
              <Download className="h-3.5 w-3.5" />
            </button>
            <button
              onClick={() => setAsc((a) => !a)}
              aria-label="Reverse order"
              className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border"
            >
              <ArrowDownUp className="h-3.5 w-3.5" />
            </button>
            <button
              onClick={() => void chapters.refetch()}
              aria-label="Refresh chapters"
              className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border"
            >
              <RefreshCcw className={`h-3.5 w-3.5 ${chapters.isFetching ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>
        {chapters.isLoading ? (
          <div className="space-y-1.5">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="kt-shimmer h-12 rounded-xl" />
            ))}
          </div>
        ) : chapters.error ? (
          <div className="kt-card rounded-xl p-4 text-center text-xs text-muted-foreground">
            Chapters failed to load: {chapters.error instanceof Error ? chapters.error.message : "error"}
          </div>
        ) : list.length === 0 ? (
          <p className="kt-card rounded-xl p-6 text-center text-sm text-muted-foreground">No chapters available from this source.</p>
        ) : (
          <ul className="overflow-hidden rounded-xl ring-1 ring-border">
            {ordered.map((c) => {
              const isRead = readSet.has(c.key);
              const isCurrent = progress?.chapterKey === c.key;
              return (
                <li key={c.key} className="flex items-center border-b border-border bg-[color:var(--surface)] last:border-b-0">
                  <Link
                    to="/read/$sourceId/$mangaKey/$chapterKey"
                    params={readParams(c)}
                    disabled={c.locked}
                    className={`flex min-w-0 flex-1 items-center gap-3 px-4 py-3 text-sm hover:bg-[color:var(--surface-2)] ${isRead ? "opacity-50" : ""}`}
                  >
                    <div className="min-w-0 flex-1">
                      <p className="flex items-center gap-1.5 font-semibold">
                        {c.locked && <Lock className="h-3.5 w-3.5 text-amber-400" />}
                        {chapterLabel(c)}
                        {c.number != null && c.title ? <span className="truncate font-normal text-muted-foreground">— {c.title}</span> : null}
                        {isCurrent && <span className="rounded-full bg-primary/15 px-1.5 text-[10px] font-bold text-primary">p.{progress!.page}</span>}
                      </p>
                      <p className="truncate text-[11px] text-muted-foreground">
                        {[c.scanlator, c.date ? new Date(c.date * (c.date < 1e12 ? 1000 : 1)).toLocaleDateString() : null, c.lang?.toUpperCase()]
                          .filter(Boolean)
                          .join(" · ")}
                      </p>
                    </div>
                  </Link>
                  {card && !c.locked && <DownloadButton series={card} chapter={c} />}
                  <button
                    onClick={() => markChapterRead(id, c.key, !isRead)}
                    aria-label={isRead ? "Mark unread" : "Mark read"}
                    className={`grid h-12 w-11 shrink-0 place-items-center ${isRead ? "text-primary" : "text-muted-foreground/50"}`}
                  >
                    <CheckCheck className="h-4 w-4" />
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </article>
  );
}
