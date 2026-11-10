import { Link } from "@tanstack/react-router";
import { motion, AnimatePresence } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { useRewards } from "@/lib/rewards";

export function KuroCoin({ className = "" }: { className?: string }) {
  return (
    <motion.span
      aria-hidden
      className={`inline-grid h-5 w-5 place-items-center rounded-full text-[11px] font-black text-black ${className}`}
      style={{
        background: "radial-gradient(circle at 30% 30%, #ffe680, #f5b800 60%, #b07700 100%)",
        boxShadow: "0 0 10px rgba(245,184,0,0.55), inset 0 0 4px rgba(0,0,0,0.25)",
      }}
      animate={{ rotateY: [0, 360] }}
      transition={{ duration: 4, repeat: Infinity, ease: "linear" }}
    >
      K
    </motion.span>
  );
}

export function KuroBalance() {
  const { points: credits } = useRewards();
  const [display, setDisplay] = useState(credits);
  const prev = useRef(credits);

  useEffect(() => {
    if (credits === prev.current) return;
    const start = prev.current;
    const diff = credits - start;
    const t0 = performance.now();
    const dur = 700;
    let raf = 0;
    const tick = (t: number) => {
      const p = Math.min(1, (t - t0) / dur);
      const eased = 1 - Math.pow(1 - p, 3);
      setDisplay(Math.round(start + diff * eased));
      if (p < 1) raf = requestAnimationFrame(tick);
      else prev.current = credits;
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [credits]);

  const gained = credits > prev.current;

  return (
    <Link
      to="/rewards"
      className="relative inline-flex items-center gap-1.5 rounded-full bg-[color:var(--surface)] px-2.5 py-1 text-xs font-bold ring-1 ring-border hover:bg-[color:var(--surface-2)]"
    >
      <KuroCoin />
      <span className="tabular-nums">{display.toLocaleString()}</span>
      <AnimatePresence>
        {gained && (
          <motion.span
            initial={{ y: 0, opacity: 0 }}
            animate={{ y: -18, opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6 }}
            className="pointer-events-none absolute -top-1 right-1 text-[10px] font-black text-amber-400"
          >
            +{credits - prev.current}
          </motion.span>
        )}
      </AnimatePresence>
    </Link>
  );
}
