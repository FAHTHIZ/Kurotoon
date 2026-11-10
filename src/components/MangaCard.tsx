import { Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { Star } from "lucide-react";
import { memo, useRef, useState, useCallback } from "react";
import { MangaPreview } from "./MangaPreview";
import { SmartImage } from "./SmartImage";
import { proxied } from "@/lib/image";
import { revealUp } from "@/lib/motion";
import { getPerfProfile } from "@/lib/perf";
import type { SeriesCard } from "@/lib/catalog";

export type { SeriesCard };


const LONG_PRESS_MS = 400;

export const MangaCard = memo(function MangaCard({ s, index = 0 }: { s: SeriesCard; index?: number }) {
  const [previewOpen, setPreviewOpen] = useState(false);
  const { eagerCount } = getPerfProfile();
  const timer = useRef<number | null>(null);
  const triggered = useRef(false);

  const clear = () => {
    if (timer.current) {
      window.clearTimeout(timer.current);
      timer.current = null;
    }
  };

  const start = useCallback(() => {
    triggered.current = false;
    clear();
    timer.current = window.setTimeout(() => {
      triggered.current = true;
      // light haptic on mobile
      if (typeof navigator !== "undefined" && "vibrate" in navigator) {
        try { navigator.vibrate?.(15); } catch { /* noop */ }
      }
      setPreviewOpen(true);
    }, LONG_PRESS_MS);
  }, []);

  const cancel = useCallback(() => {
    clear();
  }, []);

  const onClickCapture = (e: React.MouseEvent) => {
    if (triggered.current) {
      e.preventDefault();
      e.stopPropagation();
      triggered.current = false;
    }
  };

  return (
    <>
      <motion.div
        variants={revealUp}
        initial="hidden"
        animate="show"
        transition={{ duration: 0.4, delay: Math.min(index, 14) * 0.03 }}
        whileHover={{ y: -4 }}
        whileTap={{ scale: 0.965 }}
        className="group/card will-change-transform"
        style={{ transition: "none" }}
      >
        <Link
          to="/series/$slug"
          params={{ slug: s.slug }}
          className="group block select-none"
          onClickCapture={onClickCapture}
          onContextMenu={(e) => e.preventDefault()}
          onTouchStart={start}
          onTouchEnd={cancel}
          onTouchMove={cancel}
          onTouchCancel={cancel}
          onMouseDown={start}
          onMouseUp={cancel}
          onMouseLeave={cancel}
          style={{ WebkitTouchCallout: "none" }}
        >
          <div className="kt-cv relative aspect-[2/3] overflow-hidden rounded-xl bg-[color:var(--surface)] ring-1 ring-white/[0.06] shadow-[var(--shadow-depth-2)] transition-shadow duration-300 group-hover/card:shadow-[var(--shadow-depth-3)]">
            {s.cover_url && (
              <SmartImage
                src={proxied(s.cover_url)}
                alt={s.title}
                eager={index < eagerCount}
                draggable={false}
                sizes="(max-width: 640px) 33vw, (max-width: 768px) 25vw, 16vw"
                className="h-full w-full object-cover transition-transform duration-500 group-hover/card:scale-[1.07]"
              />
            )}
            {/* interactive lighting */}
            <div className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover/card:opacity-100"
              style={{ background: "radial-gradient(90% 60% at 50% 0%, oklch(1 0 0 / 0.14), transparent 70%)" }} />
            <div className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-white/0 transition-[box-shadow] duration-300 group-hover/card:shadow-[inset_0_0_0_1px_var(--primary)]" />
            <div className="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/90 via-black/30 to-transparent p-2 pt-8">
              {s.rating ? (
                <div className="flex items-center gap-1 text-[10px] font-semibold text-white">
                  <Star className="h-3 w-3 fill-[color:var(--gold)] text-[color:var(--gold)]" />
                  {Number(s.rating).toFixed(1)}
                </div>
              ) : null}
            </div>
          </div>
          <h3 className="mt-1.5 line-clamp-2 text-xs font-medium leading-snug text-foreground transition-colors group-hover/card:text-primary">
            {s.title}
          </h3>
          {s.genres?.[0] && (
            <p className="mt-0.5 line-clamp-1 text-[10px] text-muted-foreground">{s.genres.slice(0, 2).join(" • ")}</p>
          )}
        </Link>
      </motion.div>

      <MangaPreview seriesId={previewOpen ? s.id : null} onClose={() => setPreviewOpen(false)} />
    </>
  );
});
