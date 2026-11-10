import { AnimatePresence, motion } from "framer-motion";
import { useQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { Bookmark, BookmarkCheck, Play, Star, X } from "lucide-react";
import { toast } from "sonner";
import { useEffect, useState } from "react";
import { proxied } from "@/lib/image";
import { getSeries } from "@/lib/catalog";
import { isBookmarked, toggleBookmark } from "@/lib/local-library";

type Props = {
  seriesId: string | null;
  onClose: () => void;
};

export function MangaPreview({ seriesId, onClose }: Props) {
  const open = !!seriesId;

  useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = prev; };
  }, [open]);

  const { data } = useQuery({
    enabled: open,
    queryKey: ["md", "series", seriesId],
    queryFn: () => getSeries(seriesId!),
    staleTime: 3 * 60_000,
  });

  const series = data?.series;
  const chapters = data?.chapters;
  const firstChapter = chapters?.[chapters.length - 1];

  const [saved, setSaved] = useState(false);
  useEffect(() => { if (seriesId) setSaved(isBookmarked(seriesId)); }, [seriesId]);

  const onBookmark = () => {
    if (!series) return;
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
    <AnimatePresence>
      {open && (
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

            {series ? (
              <>
                <div className="relative h-44 w-full overflow-hidden">
                  <img src={proxied(series.cover_url)} alt="" referrerPolicy="no-referrer" className="h-full w-full scale-110 object-cover opacity-60 blur-md" />
                  <div className="absolute inset-0 bg-gradient-to-t from-[color:var(--surface)] via-[color:var(--surface)]/40 to-transparent" />
                  <div className="absolute inset-x-0 bottom-0 flex items-end gap-3 p-3">
                    <img src={proxied(series.cover_url)} alt={series.title} referrerPolicy="no-referrer" className="h-32 shrink-0 rounded-lg object-cover ring-1 ring-border" style={{ width: "5.5rem" }} />
                    <div className="min-w-0 flex-1 pb-1">
                      <h2 className="line-clamp-2 text-base font-extrabold leading-tight">{series.title}</h2>
                      <p className="mt-0.5 line-clamp-1 text-[11px] text-muted-foreground">{series.author}</p>
                      <div className="mt-1.5 flex items-center gap-2 text-[11px]">
                        {series.rating ? (
                          <span className="inline-flex items-center gap-1 font-semibold">
                            <Star className="h-3 w-3 fill-primary text-primary" />
                            {series.rating.toFixed(2)}
                          </span>
                        ) : null}
                        <span className="rounded-full bg-black/40 px-2 py-0.5 capitalize text-muted-foreground">{series.status}</span>
                        <span className="text-muted-foreground">{chapters?.length ?? 0} ch</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 p-4 pt-3">
                  <div className="flex flex-wrap gap-1">
                    {series.genres.slice(0, 6).map((g) => (
                      <span key={g} className="rounded-full bg-black/30 px-2 py-0.5 text-[10px] text-muted-foreground ring-1 ring-border">{g}</span>
                    ))}
                  </div>

                  {series.description && (
                    <p className="line-clamp-4 text-xs leading-relaxed text-muted-foreground">{series.description}</p>
                  )}

                  <div className="flex items-center gap-2 pt-1">
                    {firstChapter ? (
                      <Link
                        to="/read/$chapterId"
                        params={{ chapterId: firstChapter.id }}
                        onClick={onClose}
                        className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-full bg-primary px-3 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
                      >
                        <Play className="h-3.5 w-3.5" /> Read Ch. {firstChapter.number}
                      </Link>
                    ) : (
                      <Link
                        to="/series/$slug"
                        params={{ slug: series.slug }}
                        onClick={onClose}
                        className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-full bg-primary px-3 py-2 text-xs font-bold text-primary-foreground"
                      >
                        Open
                      </Link>
                    )}
                    <button
                      onClick={onBookmark}
                      aria-label="Bookmark"
                      className="inline-flex h-9 items-center justify-center gap-1.5 rounded-full bg-black/30 px-3 text-xs font-semibold ring-1 ring-border"
                    >
                      {saved ? <BookmarkCheck className="h-4 w-4 text-primary" /> : <Bookmark className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex h-72 items-center justify-center text-xs text-muted-foreground">Loading…</div>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
