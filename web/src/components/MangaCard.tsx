import { Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { memo, useRef, useState, useCallback } from "react";
import { MangaPreview } from "./MangaPreview";
import { Cover } from "./Cover";
import { revealUp } from "@/lib/motion";
import { getPerfProfile } from "@/lib/perf";
import { encodeKey } from "@/engine/ids";
import { STATUS_LABEL, type SeriesCard } from "@/engine/types";
import { rememberSeries } from "@/lib/local-library";

export type { SeriesCard };

const LONG_PRESS_MS = 400;

export const MangaCard = memo(function MangaCard({
  s,
  index = 0,
  badge,
}: {
  s: SeriesCard;
  index?: number;
  /** Optional corner label (e.g. source name in Universal Search). */
  badge?: string;
}) {
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
      try {
        navigator.vibrate?.(15);
      } catch {
        /* noop */
      }
      setPreviewOpen(true);
    }, LONG_PRESS_MS);
  }, []);

  const cancel = useCallback(() => clear(), []);

  const onClickCapture = (e: React.MouseEvent) => {
    if (triggered.current) {
      e.preventDefault();
      e.stopPropagation();
      triggered.current = false;
      return;
    }
    rememberSeries(s);
  };

  const status = s.status != null && s.status > 0 ? STATUS_LABEL[s.status] : null;

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
          to="/manga/$sourceId/$key"
          params={{ sourceId: s.sourceId, key: encodeKey(s.key) }}
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
            <Cover
              sourceId={s.sourceId}
              url={s.cover}
              alt={s.title}
              eager={index < eagerCount}
              sizes="(max-width: 640px) 33vw, (max-width: 768px) 25vw, 16vw"
              className="transition-transform duration-500 group-hover/card:scale-[1.07]"
            />
            <div
              className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover/card:opacity-100"
              style={{ background: "radial-gradient(90% 60% at 50% 0%, oklch(1 0 0 / 0.14), transparent 70%)" }}
            />
            <div className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-white/0 transition-[box-shadow] duration-300 group-hover/card:shadow-[inset_0_0_0_1px_var(--primary)]" />
            {badge && (
              <span className="pointer-events-none absolute left-1.5 top-1.5 max-w-[85%] truncate rounded-md bg-black/60 px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider text-white/90 backdrop-blur">
                {badge}
              </span>
            )}
            {status && (
              <div className="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/90 via-black/30 to-transparent p-2 pt-8">
                <span className="text-[10px] font-semibold text-white/90">{status}</span>
              </div>
            )}
          </div>
          <h3 className="mt-1.5 line-clamp-2 text-xs font-medium leading-snug text-foreground transition-colors group-hover/card:text-primary">
            {s.title}
          </h3>
          {s.tags[0] && <p className="mt-0.5 line-clamp-1 text-[10px] text-muted-foreground">{s.tags.slice(0, 2).join(" • ")}</p>}
        </Link>
      </motion.div>

      <MangaPreview card={previewOpen ? s : null} onClose={() => setPreviewOpen(false)} />
    </>
  );
});

export function CardSkeletons({ count = 12 }: { count?: number }) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i}>
          <div className="kt-shimmer aspect-[2/3] rounded-xl ring-1 ring-white/[0.06]" />
          <div className="kt-shimmer mt-1.5 h-3 w-3/4 rounded" />
        </div>
      ))}
    </>
  );
}
