import { AnimatePresence, motion } from "framer-motion";
import { Check, ChevronRight, List, RotateCcw } from "lucide-react";
import { spring, ease } from "@/lib/motion";

type Props = {
  open: boolean;
  chapterNumber: string;
  seriesTitle?: string | null;
  /** Go to the next chapter (absent when caught up). */
  onNext?: (() => void) | null;
  /** Back to the chapter list. */
  onChapters?: (() => void) | null;
  onDismiss: () => void;
  onRestart: () => void;
};

const RAYS = 10;

/** Celebratory moment shown when the reader finishes a chapter. */
export function ChapterComplete({
  open,
  chapterNumber,
  seriesTitle,
  onNext,
  onChapters,
  onDismiss,
  onRestart,
}: Props) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.25, ease: ease.out }}
          className="fixed inset-0 z-[60] grid place-items-center bg-black/80 px-6 backdrop-blur-xl"
          onClick={onDismiss}
        >
          <motion.div
            initial={{ opacity: 0, y: 24, scale: 0.94 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 16, scale: 0.96 }}
            transition={spring.surface}
            onClick={(e) => e.stopPropagation()}
            className="relative w-full max-w-sm overflow-hidden rounded-3xl bg-[color:var(--surface)] p-6 text-center ring-1 ring-white/10 shadow-[var(--shadow-depth-3)]"
          >
            <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-80" />

            {/* seal + rays */}
            <div className="relative mx-auto mb-4 grid h-20 w-20 place-items-center">
              {Array.from({ length: RAYS }).map((_, i) => (
                <motion.span
                  key={i}
                  initial={{ opacity: 0, scaleY: 0.2 }}
                  animate={{ opacity: [0, 1, 0], scaleY: [0.2, 1, 0.4] }}
                  transition={{ duration: 0.9, delay: 0.15 + i * 0.02, ease: ease.out }}
                  className="absolute h-10 w-[2px] origin-bottom rounded-full"
                  style={{
                    background: "linear-gradient(to top, transparent, var(--gold))",
                    transform: `rotate(${(360 / RAYS) * i}deg) translateY(-26px)`,
                  }}
                />
              ))}
              <motion.span
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: [0, 1.5, 1.9], opacity: [0.7, 0.3, 0] }}
                transition={{ duration: 1, ease: ease.out }}
                className="absolute h-16 w-16 rounded-full"
                style={{ background: "radial-gradient(circle, oklch(0.83 0.14 88 / 0.5), transparent 70%)" }}
              />
              <motion.span
                initial={{ scale: 0.3, rotate: -25, opacity: 0 }}
                animate={{ scale: 1, rotate: 0, opacity: 1 }}
                transition={{ ...spring.tap, delay: 0.08 }}
                className="relative grid h-14 w-14 place-items-center rounded-full bg-[image:var(--gradient-accent)] shadow-[var(--shadow-glow-lg)]"
              >
                <Check className="h-7 w-7 text-primary-foreground" strokeWidth={3} />
              </motion.span>
            </div>

            <motion.p
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.18, duration: 0.3, ease: ease.out }}
              className="text-[10px] font-bold uppercase tracking-[0.2em] text-[color:var(--gold)]"
            >
              Chapter complete
            </motion.p>
            <motion.h2
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.24, duration: 0.32, ease: ease.out }}
              className="mt-1 text-xl font-extrabold"
            >
              Chapter {chapterNumber}
            </motion.h2>
            {seriesTitle && (
              <motion.p
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.3, duration: 0.3 }}
                className="mt-0.5 line-clamp-1 text-xs text-muted-foreground"
              >
                {seriesTitle}
              </motion.p>
            )}

            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.36, duration: 0.32, ease: ease.out }}
              className="mt-5 space-y-2"
            >
              {onNext ? (
                <button onClick={() => { onDismiss(); onNext(); }} className="block w-full">
                  <motion.span
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.96 }}
                    transition={spring.tap}
                    className="flex w-full items-center justify-center gap-1.5 rounded-full bg-[image:var(--gradient-accent)] py-3 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
                  >
                    Next chapter <ChevronRight className="h-4 w-4" />
                  </motion.span>
                </button>
              ) : (
                <p className="rounded-full bg-white/5 py-3 text-xs font-semibold text-muted-foreground">
                  You’re all caught up
                </p>
              )}
              <div className="flex gap-2">
                <motion.button
                  whileTap={{ scale: 0.95 }}
                  transition={spring.tap}
                  onClick={onRestart}
                  className="flex flex-1 items-center justify-center gap-1.5 rounded-full bg-white/5 py-2.5 text-xs font-semibold ring-1 ring-white/10"
                >
                  <RotateCcw className="h-3.5 w-3.5" /> Re-read
                </motion.button>
                {onChapters && (
                  <button onClick={() => { onDismiss(); onChapters(); }} className="flex-1">
                    <motion.span
                      whileTap={{ scale: 0.95 }}
                      transition={spring.tap}
                      className="flex w-full items-center justify-center gap-1.5 rounded-full bg-white/5 py-2.5 text-xs font-semibold ring-1 ring-white/10"
                    >
                      <List className="h-3.5 w-3.5" /> Chapters
                    </motion.span>
                  </button>
                )}
              </div>
            </motion.div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
