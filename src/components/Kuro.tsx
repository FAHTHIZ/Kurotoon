import { motion } from "framer-motion";
import { spring } from "@/lib/motion";

export type KuroMood = "idle" | "happy" | "reading" | "sleepy" | "hyped";

const eyes: Record<KuroMood, string> = {
  idle: "M 34 46 q 6 -7 12 0 M 66 46 q 6 -7 12 0",
  happy: "M 34 48 q 6 -10 12 0 M 66 48 q 6 -10 12 0",
  reading: "M 34 47 h 12 M 66 47 h 12",
  sleepy: "M 34 49 q 6 4 12 0 M 66 49 q 6 4 12 0",
  hyped: "M 34 44 q 6 -12 12 0 M 66 44 q 6 -12 12 0",
};

/**
 * Kuro — the KuroToon mascot. Pure SVG so it costs nothing to ship and
 * can be tinted by brand tokens anywhere in the app.
 */
export function Kuro({
  mood = "idle",
  size = 96,
  className = "",
}: {
  mood?: KuroMood;
  size?: number;
  className?: string;
}) {
  return (
    <motion.svg
      viewBox="0 0 112 112"
      width={size}
      height={size}
      className={className}
      initial={{ scale: 0.9, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={spring.soft}
      aria-label="Kuro, the KuroToon mascot"
      role="img"
    >
      <defs>
        <radialGradient id="kuro-body" cx="35%" cy="28%">
          <stop offset="0%" stopColor="oklch(0.34 0.05 300)" />
          <stop offset="60%" stopColor="oklch(0.19 0.03 290)" />
          <stop offset="100%" stopColor="oklch(0.12 0.02 280)" />
        </radialGradient>
        <linearGradient id="kuro-aura" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="var(--brand-crimson)" />
          <stop offset="50%" stopColor="var(--brand-galaxy)" />
          <stop offset="100%" stopColor="var(--brand-aurora)" />
        </linearGradient>
      </defs>

      <motion.circle
        cx="56"
        cy="58"
        r="46"
        fill="url(#kuro-aura)"
        opacity={0.22}
        animate={{ scale: mood === "hyped" ? [1, 1.08, 1] : [1, 1.03, 1] }}
        transition={{ duration: mood === "hyped" ? 1.2 : 3.4, repeat: Infinity, ease: "easeInOut" }}
      />

      <motion.g
        animate={{ y: mood === "sleepy" ? [0, 1.5, 0] : [0, -3, 0] }}
        transition={{ duration: mood === "sleepy" ? 4 : 2.6, repeat: Infinity, ease: "easeInOut" }}
      >
        {/* ears */}
        <path d="M 24 30 L 34 8 L 46 26 Z" fill="url(#kuro-body)" />
        <path d="M 88 30 L 78 8 L 66 26 Z" fill="url(#kuro-body)" />
        {/* head / body */}
        <circle cx="56" cy="56" r="38" fill="url(#kuro-body)" />
        <circle cx="56" cy="56" r="38" fill="none" stroke="url(#kuro-aura)" strokeWidth="1.5" opacity="0.7" />
        {/* eyes */}
        <path d={eyes[mood]} stroke="var(--brand-aurora)" strokeWidth="4" strokeLinecap="round" fill="none" />
        {/* mouth */}
        <path
          d={mood === "happy" || mood === "hyped" ? "M 48 68 q 8 8 16 0" : "M 50 68 q 6 3 12 0"}
          stroke="var(--brand-crimson)"
          strokeWidth="3"
          strokeLinecap="round"
          fill="none"
        />
        {/* crest */}
        <circle cx="56" cy="30" r="4" fill="var(--brand-gold)" opacity="0.9" />
      </motion.g>
    </motion.svg>
  );
}

/** Small inline Kuro used in empty states and toasts. */
export function KuroTip({ children, mood = "happy" }: { children: React.ReactNode; mood?: KuroMood }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl bg-[color:var(--surface)] p-3 ring-1 ring-border">
      <Kuro mood={mood} size={44} />
      <p className="text-xs text-muted-foreground">{children}</p>
    </div>
  );
}
