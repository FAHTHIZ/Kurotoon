import { motion } from "framer-motion";
import { spring } from "@/lib/motion";

/**
 * Floating reading-progress orb. Ring fills with progress, the core pulses
 * softly, and tapping it toggles immersion mode.
 */
export function ProgressOrb({
  progress,
  label,
  onClick,
  dimmed,
}: {
  progress: number; // 0..1
  label: string;
  onClick: () => void;
  dimmed?: boolean;
}) {
  const p = Math.max(0, Math.min(1, progress));
  const R = 22;
  const C = 2 * Math.PI * R;
  const complete = p >= 0.999;

  return (
    <motion.button
      onClick={onClick}
      initial={{ opacity: 0, scale: 0.6 }}
      animate={{ opacity: dimmed ? 0.45 : 1, scale: 1 }}
      whileHover={{ scale: 1.08 }}
      whileTap={{ scale: 0.9 }}
      transition={spring.soft}
      aria-label="Toggle immersion mode"
      className="fixed bottom-20 right-4 z-50 grid h-14 w-14 place-items-center rounded-full bg-black/55 backdrop-blur-xl ring-1 ring-white/10 shadow-[var(--shadow-depth-3)] md:bottom-6"
    >
      <svg viewBox="0 0 52 52" className="absolute inset-0 h-full w-full -rotate-90">
        <circle cx="26" cy="26" r={R} fill="none" stroke="oklch(1 0 0 / 0.14)" strokeWidth="3" />
        <motion.circle
          cx="26"
          cy="26"
          r={R}
          fill="none"
          stroke={complete ? "var(--gold)" : "var(--primary)"}
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={C}
          animate={{ strokeDashoffset: C * (1 - p) }}
          transition={spring.glide}
          style={{ filter: `drop-shadow(0 0 6px ${complete ? "var(--gold)" : "var(--primary)"})` }}
        />
      </svg>
      <motion.span
        animate={{ opacity: [0.55, 1, 0.55], scale: [1, 1.12, 1] }}
        transition={{ duration: 3.2, repeat: Infinity, ease: "easeInOut" }}
        className="absolute h-9 w-9 rounded-full"
        style={{
          background: complete
            ? "radial-gradient(circle, oklch(0.83 0.14 88 / 0.4), transparent 70%)"
            : "radial-gradient(circle, oklch(0.66 0.22 25 / 0.35), transparent 70%)",
        }}
      />
      <span className="relative text-[10px] font-bold tabular-nums text-white">{label}</span>
    </motion.button>
  );
}
