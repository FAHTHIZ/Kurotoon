// KuroToon brand foundation — reusable, not page-specific.
// Themes: discovery, exploration, creators, community, freedom.

export const APP_NAME = "KuroToon";
export const APP_DESCRIPTION =
  "An extension-based manga, manhwa and webtoon reader that keeps everything on your device.";

/** Core palette, mirrored by CSS tokens in styles.css. */
export const brandColors = {
  midnight: "var(--brand-midnight)",
  crimson: "var(--brand-crimson)",
  galaxy: "var(--brand-galaxy)",
  aurora: "var(--brand-aurora)",
  gold: "var(--brand-gold)",
} as const;

export type BrandColor = keyof typeof brandColors;

/** Typography scale tokens (class strings, so pages opt in without redesign). */
export const type = {
  display: "text-2xl font-black tracking-tight",
  title: "text-lg font-bold tracking-tight",
  section: "text-base font-semibold",
  body: "text-sm",
  meta: "text-xs text-muted-foreground",
  micro: "text-[10px] uppercase tracking-widest text-muted-foreground",
} as const;

/** Atmospheres used by discovery, library analytics and the reader. */
export const ATMOSPHERES = [
  "epic",
  "dark",
  "wholesome",
  "psychological",
  "mystery",
  "cozy",
  "emotional",
  "action",
  "adventure",
  "romantic",
  "tragic",
] as const;
export type Atmosphere = (typeof ATMOSPHERES)[number];

export const RARITIES = ["common", "rare", "epic", "legendary", "mythic"] as const;
export type Rarity = (typeof RARITIES)[number];

export const rarityRing: Record<Rarity, string> = {
  common: "ring-white/15",
  rare: "ring-[color:var(--brand-aurora)]/50",
  epic: "ring-[color:var(--brand-galaxy)]/55",
  legendary: "ring-[color:var(--brand-gold)]/60",
  mythic: "ring-[color:var(--brand-crimson)]/70",
};
