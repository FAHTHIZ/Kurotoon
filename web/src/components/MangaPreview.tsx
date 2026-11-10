import { AnimatePresence, motion } from "framer-motion";
import { Link } from "@tanstack/react-router";
import { Bookmark, BookmarkCheck, Play, X } from "lucide-react";
import { toast } from "sonner";
import { useEffect, useState } from "react";
import { Cover } from "./Cover";
import { useChapters, useMangaDetails } from "@/engine/hooks";
import { encodeKey } from "@/engine/ids";
import { STATUS_LABEL, type SeriesCard } from "@/engine/types";
import { chapterLabel, getProgress, idOf, isBookmarked, toggleBookmark } from "@/lib/local-library";

type Props = { card: SeriesCard | null; onClose: () => void };

export function MangaPreview({ card, onClose }: Props) {
  const open = !!card;
  useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  return <AnimatePresence>{card && <PreviewSheet card={card} onClose={onClose} />}</AnimatePresence>;
}

function PreviewSheet({ card, onClose }: { card: SeriesCard; onClose: () => void }) {
  const details = useMangaDetails(card.sourceId, card.key);
  const chapters = useChapters(card.sourceId, card.key);
  const series = { ...card, ...(details.data ?? {}) };
  const id = idOf(card);
  const progress = getProgress(id);
  const list = chapters.data ?? [];
  const target = (progress && list.find((c) => c.key === progress.chapterKey)) ?? list[list.length - 1];

  const [saved, setSaved] = useState(false);
  useEffect(() => setSaved(isBookmarked(id)), [id]);

  const onBookmark = () => {
    const now = toggleBookmark(series);
    setSaved(now);
    toast.success(now ? "Added to library" : "Removed from library");
  };

  const mangaParams = { sourceId: card.sourceId, key: encodeKey(card.key) };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.18 }}
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      onClick={onClose}
      onContextMenu={(e) => e.preventDefault()}
      style={{ backdropFilter: "blur(14px)", WebkitBackdropFilter: "blur(14px)", background: "rgba(0,0,0,0.55)" }}
    >
      <motion.div
        initial={{ scale: 0.92, opacity: 0, y: 16 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.94, opacity: 0, y: 8 }}
        transition={{ type: "spring", stiffness: 320, damping: 28 }}
        className="relative w-full max-w-sm overflow-hidden rounded-2xl bg-[color:var(--surface)] shadow-2xl ring-1 ring-border"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          aria-label="Close preview"
          className="absolute right-2 top-2 z-10 rounded-full bg-black/50 p-1.5 text-white/90 backdrop-blur"
        >
          <X className="h-4 w-4" />
        </button>

        <div className="relative h-44 w-full overflow-hidden">
          <div className="absolute inset-0 scale-110 opacity-60 blur-md">
            <Cover sourceId={card.sourceId} url={series.cover} alt="" />
          </div>
          <div className="absolute inset-0 bg-gradient-to-t from-[color:var(--surface)] via-[color:var(--surface)]/40 to-transparent" />
          <div className="absolute inset-x-0 bottom-0 flex items-end gap-3 p-3">
            <div className="relative h-32 shrink-0 overflow-hidden rounded-lg ring-1 ring-border" style={{ width: "5.5rem" }}>
              <Cover sourceId={card.sourceId} url={series.cover} alt={series.title} eager />
            </div>
            <div className="min-w-0 flex-1 pb-1">
              <h2 className="line-clamp-2 text-base font-extrabold leading-tight">{series.title}</h2>
              <p className="mt-0.5 line-clamp-1 text-[11px] text-muted-foreground">{series.authors.join(", ")}</p>
              <div className="mt-1.5 flex items-center gap-2 text-[11px]">
                {series.status != null && series.status > 0 && (
                  <span className="rounded-full bg-black/40 px-2 py-0.5 text-muted-foreground">{STATUS_LABEL[series.status]}</span>
                )}
                <span className="text-muted-foreground">{chapters.isLoading ? "…" : `${list.length} ch`}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-3 p-4 pt-3">
          {series.tags.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {series.tags.slice(0, 6).map((g) => (
                <span key={g} className="rounded-full bg-black/30 px-2 py-0.5 text-[10px] text-muted-foreground ring-1 ring-border">
                  {g}
                </span>
              ))}
            </div>
          )}
          {details.isLoading ? (
            <div className="space-y-1.5">
              <div className="kt-shimmer h-3 rounded" />
              <div className="kt-shimmer h-3 w-4/5 rounded" />
            </div>
          ) : (
            details.data?.description && (
              <p className="line-clamp-4 text-xs leading-relaxed text-muted-foreground">{details.data.description}</p>
            )
          )}

          <div className="flex items-center gap-2 pt-1">
            {target ? (
              <Link
                to="/read/$sourceId/$mangaKey/$chapterKey"
                params={{ sourceId: card.sourceId, mangaKey: encodeKey(card.key), chapterKey: encodeKey(target.key) }}
                onClick={onClose}
                className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-full bg-primary px-3 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
              >
                <Play className="h-3.5 w-3.5" /> {progress ? "Continue" : "Read"} {chapterLabel(target)}
              </Link>
            ) : (
              <Link
                to="/manga/$sourceId/$key"
                params={mangaParams}
                onClick={onClose}
                className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-full bg-primary px-3 py-2 text-xs font-bold text-primary-foreground"
              >
                Open
              </Link>
            )}
            <button
              onClick={onBookmark}
              aria-label={saved ? "Remove from library" : "Add to library"}
              className="inline-flex h-9 items-center justify-center gap-1.5 rounded-full bg-black/30 px-3 text-xs font-semibold ring-1 ring-border"
            >
              {saved ? <BookmarkCheck className="h-4 w-4 text-primary" /> : <Bookmark className="h-4 w-4" />}
            </button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
