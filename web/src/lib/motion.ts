// KuroToon unified motion language.
// One place for springs, easings and reveal variants so every surface
// (buttons, cards, nav, search, pages) shares the same physics.
import type { Transition, Variants } from "framer-motion";

export const spring = {
  /** Quick, snappy — taps and toggles. */
  tap: { type: "spring", stiffness: 620, damping: 30, mass: 0.6 } as Transition,
  /** Default interactive spring — hovers, chips, icons. */
  soft: { type: "spring", stiffness: 380, damping: 28, mass: 0.8 } as Transition,
  /** Layout / indicator movement — bottom nav pill, sliders. */
  glide: { type: "spring", stiffness: 300, damping: 26, mass: 0.9 } as Transition,
  /** Larger surfaces — sheets, overlays, hero. */
  surface: { type: "spring", stiffness: 240, damping: 26, mass: 1 } as Transition,
} as const;

export const ease = {
  out: [0.22, 0.9, 0.24, 1] as const,
  inOut: [0.65, 0, 0.35, 1] as const,
};

/** Staggered container for grids and lists. */
export const staggerContainer = (stagger = 0.035, delay = 0): Variants => ({
  hidden: {},
  show: { transition: { staggerChildren: stagger, delayChildren: delay } },
});

/** Child reveal used with staggerContainer. */
export const revealUp: Variants = {
  hidden: { opacity: 0, y: 14, scale: 0.985 },
  show: { opacity: 1, y: 0, scale: 1, transition: { duration: 0.42, ease: ease.out } },
};

export const fadeIn: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { duration: 0.3, ease: ease.out } },
};

/** Page-level transition for route content. */
export const pageTransition: Variants = {
  hidden: { opacity: 0, y: 10 },
  show: { opacity: 1, y: 0, transition: { duration: 0.35, ease: ease.out } },
  exit: { opacity: 0, y: -6, transition: { duration: 0.18, ease: ease.inOut } },
};

/** Shared press physics for anything that should feel physical. */
export const press = {
  whileTap: { scale: 0.955 },
  whileHover: { scale: 1.02 },
  transition: spring.tap,
} as const;
