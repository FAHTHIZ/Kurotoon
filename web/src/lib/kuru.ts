// Kuru — the cosmic companion of KuroToon.
// Extracted, piece by piece, from the official Kuru character reference sheet.
// Data only: every surface (rewards, shop, collection, loaders, empty states)
// consumes these tokens so the mascot stays consistent everywhere.

import type { Rarity } from "./brand";

/** Tagline + identity copy from the reference sheet. */
export const KURU = {
  name: "Kuru",
  role: "The Cosmic Companion of KuroToon",
  blurb:
    "Kuru is more than a mascot — it's the spirit of exploration, storytelling, and community. Always with you in every chapter of your journey.",
  motto: "Every season. Every story. Always with Kuru.",
  traits: ["Curious", "Playful", "Helpful", "Mischievous"] as const,
} as const;

/** Official colour palette (hex values straight from the sheet). */
export const KURU_PALETTE = {
  void: "#0B0B12",
  ink: "#13131C",
  shadow: "#1F1B2E",
  nebula: "#2E1F4F",
  galaxy: "#7A3BFF",
  crimson: "#FF4D6D",
  gold: "#FFB347",
  moon: "#F2F2F2",
} as const;
export type KuruPaletteKey = keyof typeof KURU_PALETTE;

/** Art direction rules — used when generating or reviewing Kuru artwork. */
export const KURU_STYLE_GUIDE = [
  "Chibi anime style",
  "Big expressive eyes",
  "Soft shading with strong rim light",
  "Cosmic purple glow & particles",
  "Cute, playful, and emotive",
  "High contrast for small sizes",
  "Consistent silhouette",
] as const;

/** Standard proportions from the height sheet. */
export const KURU_PROPORTIONS = { heads: 3.2 } as const;

/** Signature look. */
export const KURU_ACCESSORIES = [
  { id: "k_coin", label: "K Coin", note: "Kuro Points made physical" },
  { id: "creator_tablet", label: "Creator Tablet", note: "Creator mode prop" },
  { id: "hood_up", label: "Hood Up", note: "Focus / night reading" },
  { id: "cape_flow", label: "Cape Flow", note: "Motion accent" },
  { id: "galaxy_aura", label: "Galaxy Aura", note: "Level-up effect" },
  { id: "paw_gloves", label: "Paw Gloves", note: "Default outfit detail" },
] as const;

/** Emotion set used for reactions, toasts, loaders and empty states. */
export const KURU_MOODS = [
  "happy",
  "laughing",
  "love",
  "shock",
  "angry",
  "cry",
  "sleepy",
  "reading",
  "excited",
  "thinking",
  "salute",
  "bye",
  "clap",
  "hug",
  "cheer",
  "wow",
  "oops",
  "thanks",
  "support",
  "creator",
  "levelup",
  "galaxy",
  "rich",
  "nom",
] as const;
export type KuruMood = (typeof KURU_MOODS)[number];

/** Contextual mood picker so surfaces don't hardcode emotions. */
export const KURU_MOOD_FOR = {
  loading: "thinking",
  empty: "sleepy",
  error: "oops",
  success: "cheer",
  reward: "rich",
  levelup: "levelup",
  reading: "reading",
  creator: "creator",
  welcome: "salute",
  farewell: "bye",
  thanks: "thanks",
} satisfies Record<string, KuruMood>;

export type KuruVariant = {
  id: string;
  label: string;
  window: string;
  rarity: Rarity;
  /** 1-12 month, or null for always-on / event-driven. */
  month: number | null;
  accent: KuruPaletteKey;
};

/** Limited seasonal & event editions — they return every year. */
export const KURU_SEASONAL: KuruVariant[] = [
  { id: "new_year", label: "New Year Kuru", window: "January", month: 1, rarity: "epic", accent: "galaxy" },
  { id: "valentine", label: "Valentine Kuru", window: "February", month: 2, rarity: "epic", accent: "crimson" },
  { id: "spring", label: "Spring Kuru", window: "March", month: 3, rarity: "rare", accent: "crimson" },
  { id: "summer", label: "Summer Kuru", window: "June – August", month: 7, rarity: "rare", accent: "gold" },
  { id: "halloween", label: "Halloween Kuru", window: "October", month: 10, rarity: "epic", accent: "gold" },
  { id: "winter", label: "Winter Kuru", window: "December", month: 12, rarity: "epic", accent: "moon" },
  { id: "anniversary", label: "Anniversary Kuru", window: "Annual event", month: null, rarity: "legendary", accent: "gold" },
];

/** Hidden legendary easter eggs. */
export const KURU_LEGENDARY: KuruVariant[] = [
  { id: "architect", label: "Architect Kuru", window: "The original Architect", month: null, rarity: "legendary", accent: "galaxy" },
  { id: "builders", label: "Builder Kuros", window: "Many Kuros, one mission", month: null, rarity: "mythic", accent: "nebula" },
  { id: "all_for_kuru", label: "All For Kuru", window: "Together, we build the future", month: null, rarity: "mythic", accent: "crimson" },
];

export const KURU_VARIANTS: KuruVariant[] = [...KURU_SEASONAL, ...KURU_LEGENDARY];

/** Which seasonal Kuru is currently in season (null outside all windows). */
export function activeSeasonalKuru(now = new Date()): KuruVariant | null {
  const m = now.getUTCMonth() + 1;
  if (m >= 6 && m <= 8) return KURU_SEASONAL.find((v) => v.id === "summer") ?? null;
  return KURU_SEASONAL.find((v) => v.month === m) ?? null;
}

/** Avatar frames offered as profile icons. */
export const KURU_AVATAR_FRAMES = [
  { id: "starfield", label: "Starfield", rarity: "common" },
  { id: "nebula", label: "Nebula", rarity: "rare" },
  { id: "coin", label: "Coin Holder", rarity: "epic" },
  { id: "shadow", label: "Shadow", rarity: "rare" },
  { id: "pumpkin", label: "Pumpkin", rarity: "legendary" },
] as const;
