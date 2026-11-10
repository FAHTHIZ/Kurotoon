// KuroToon performance primitives.
// Device-aware tuning + a tiny in-memory image cache so re-renders and
// re-mounts never re-trigger a network fetch or a fade-in flash.

type NavigatorLike = Navigator & {
  deviceMemory?: number;
  hardwareConcurrency?: number;
  connection?: { effectiveType?: string; saveData?: boolean };
};

let cachedProfile: PerfProfile | null = null;

export type PerfProfile = {
  /** Constrained device or network: shrink batches, drop decorative work. */
  lowMemory: boolean;
  /** User asked for less motion. */
  reducedMotion: boolean;
  /** How many items to render eagerly in a list before deferring. */
  eagerCount: number;
  /** How many reader pages to preload ahead. */
  preloadAhead: number;
};

export function getPerfProfile(): PerfProfile {
  if (cachedProfile) return cachedProfile;
  if (typeof window === "undefined") {
    return { lowMemory: false, reducedMotion: false, eagerCount: 6, preloadAhead: 2 };
  }
  const nav = navigator as NavigatorLike;
  const mem = nav.deviceMemory ?? 8;
  const cores = nav.hardwareConcurrency ?? 8;
  const conn = nav.connection;
  const slowNet = conn?.saveData === true || /(^|-)(2g|slow-2g)$/.test(conn?.effectiveType ?? "");
  const lowMemory = mem <= 4 || cores <= 4 || slowNet;
  const reducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;

  cachedProfile = {
    lowMemory,
    reducedMotion,
    eagerCount: lowMemory ? 3 : 6,
    preloadAhead: lowMemory ? 1 : 3,
  };
  return cachedProfile;
}

/** URLs already decoded by the browser in this session. */
const decoded = new Set<string>();

export function isImageCached(src: string): boolean {
  return decoded.has(src);
}

export function markImageCached(src: string) {
  if (decoded.size > 600) decoded.clear();
  decoded.add(src);
}

/** Warm the HTTP cache for an image without blocking render. */
export function prefetchImage(src: string | null | undefined) {
  if (!src || typeof window === "undefined" || decoded.has(src)) return;
  const img = new Image();
  img.decoding = "async";
  img.referrerPolicy = "no-referrer";
  img.onload = () => markImageCached(src);
  img.src = src;
}

export function prefetchImages(sources: Array<string | null | undefined>) {
  const { lowMemory } = getPerfProfile();
  const limit = lowMemory ? 2 : 6;
  sources.slice(0, limit).forEach(prefetchImage);
}
