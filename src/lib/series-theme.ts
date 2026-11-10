// Extracts a dynamic accent/glow/atmosphere triad from a series cover.
// Runs entirely in the browser on the same-origin proxied image, so the
// canvas stays readable and nothing is stored anywhere.
import { proxied } from "@/lib/image";

export type SeriesTheme = { accent: string; glow: string; atmos: string };

const cache = new Map<string, SeriesTheme>();

function toHsl(r: number, g: number, b: number) {
  const rn = r / 255, gn = g / 255, bn = b / 255;
  const max = Math.max(rn, gn, bn), min = Math.min(rn, gn, bn);
  const l = (max + min) / 2;
  const d = max - min;
  if (d === 0) return { h: 0, s: 0, l };
  const s = d / (1 - Math.abs(2 * l - 1));
  let h: number;
  if (max === rn) h = ((gn - bn) / d) % 6;
  else if (max === gn) h = (bn - rn) / d + 2;
  else h = (rn - gn) / d + 4;
  h = (h * 60 + 360) % 360;
  return { h, s, l };
}

export async function extractSeriesTheme(coverUrl: string | null | undefined): Promise<SeriesTheme | null> {
  if (!coverUrl || typeof window === "undefined") return null;
  const key = coverUrl;
  const hit = cache.get(key);
  if (hit) return hit;

  const src = proxied(coverUrl);
  const img = new Image();
  img.decoding = "async";
  img.src = src;
  try {
    await img.decode();
  } catch {
    return null;
  }

  const size = 32;
  const canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext("2d", { willReadFrequently: true });
  if (!ctx) return null;
  ctx.drawImage(img, 0, 0, size, size);

  let data: Uint8ClampedArray;
  try {
    data = ctx.getImageData(0, 0, size, size).data;
  } catch {
    return null; // tainted canvas — fall back to brand theme
  }

  // Weight pixels by colourfulness so muted backgrounds don't win.
  const buckets = new Map<number, { w: number; h: number; s: number; l: number }>();
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i]!, g = data[i + 1]!, b = data[i + 2]!, a = data[i + 3]!;
    if (a < 128) continue;
    const { h, s, l } = toHsl(r, g, b);
    if (l < 0.12 || l > 0.94) continue;
    const weight = s * s * (1 - Math.abs(l - 0.55));
    if (weight <= 0.002) continue;
    const bucket = Math.round(h / 15);
    const prev = buckets.get(bucket) ?? { w: 0, h: 0, s: 0, l: 0 };
    buckets.set(bucket, { w: prev.w + weight, h: prev.h + h * weight, s: prev.s + s * weight, l: prev.l + l * weight });
  }

  let best: { w: number; h: number; s: number; l: number } | null = null;
  for (const b of buckets.values()) if (!best || b.w > best.w) best = b;
  if (!best || best.w === 0) return null;

  const hue = Math.round(best.h / best.w);
  const sat = Math.min(0.9, Math.max(0.35, best.s / best.w));
  const theme: SeriesTheme = {
    accent: `hsl(${hue} ${Math.round(sat * 88)}% 62%)`,
    glow: `hsl(${hue} ${Math.round(sat * 90)}% 55% / 0.45)`,
    atmos: `hsl(${hue} ${Math.round(sat * 60)}% 12%)`,
  };
  cache.set(key, theme);
  return theme;
}

/** Inline style object that scopes the dynamic theme to a subtree. */
export function themeVars(theme: SeriesTheme | null): React.CSSProperties {
  if (!theme) return {};
  return {
    ["--series-accent" as string]: theme.accent,
    ["--series-glow" as string]: theme.glow,
    ["--series-atmos" as string]: theme.atmos,
  } as React.CSSProperties;
}
