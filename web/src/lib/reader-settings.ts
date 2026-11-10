// Reader preferences (Mihon-style), persisted on the device.
import { useCallback, useEffect, useState } from "react";

export type ReaderMode = "webtoon" | "vertical" | "ltr" | "rtl";

export type ReaderSettings = {
  mode: ReaderMode;
  /** Webtoon side padding as % of width (Mihon "side padding"). */
  sidePadding: number;
  /** Keep the screen awake while reading (Wake Lock API). */
  keepAwake: boolean;
  /** Tap zones in paged modes. */
  tapZones: boolean;
  /** Fit in paged modes. */
  fit: "screen" | "width";
  background: "black" | "gray" | "white";
};

export const READER_DEFAULTS: ReaderSettings = {
  mode: "webtoon",
  sidePadding: 0,
  keepAwake: true,
  tapZones: true,
  fit: "screen",
  background: "black",
};

const KEY = "kt.reader.v1";
const perSeriesKey = (id: string) => `kt.reader.mode.${id}`;

function load(): ReaderSettings {
  if (typeof window === "undefined") return READER_DEFAULTS;
  try {
    return { ...READER_DEFAULTS, ...(JSON.parse(localStorage.getItem(KEY) ?? "{}") as Partial<ReaderSettings>) };
  } catch {
    return READER_DEFAULTS;
  }
}

/** Global settings plus a per-series reading-mode override (like Mihon). */
export function useReaderSettings(seriesId: string) {
  const [settings, setSettings] = useState<ReaderSettings>(READER_DEFAULTS);
  useEffect(() => {
    const base = load();
    let mode = base.mode;
    try {
      const m = localStorage.getItem(perSeriesKey(seriesId)) as ReaderMode | null;
      if (m && ["webtoon", "vertical", "ltr", "rtl"].includes(m)) mode = m;
    } catch {
      /* ignore */
    }
    setSettings({ ...base, mode });
  }, [seriesId]);

  const update = useCallback(
    (patch: Partial<ReaderSettings>) => {
      setSettings((prev) => {
        const next = { ...prev, ...patch };
        try {
          const { mode, ...rest } = next;
          localStorage.setItem(KEY, JSON.stringify({ ...load(), ...rest, ...(patch.mode ? { mode } : {}) }));
          if (patch.mode) localStorage.setItem(perSeriesKey(seriesId), patch.mode);
        } catch {
          /* ignore */
        }
        return next;
      });
    },
    [seriesId],
  );

  return [settings, update] as const;
}

/** Screen wake lock while reading; re-acquired when the tab becomes visible. */
export function useWakeLock(on: boolean) {
  useEffect(() => {
    if (!on || typeof navigator === "undefined" || !("wakeLock" in navigator)) return;
    let lock: { release: () => Promise<void> } | null = null;
    let disposed = false;
    const acquire = async () => {
      try {
        lock = await (navigator as Navigator & { wakeLock: { request: (t: "screen") => Promise<{ release: () => Promise<void> }> } }).wakeLock.request("screen");
        if (disposed) void lock.release();
      } catch {
        /* denied / unsupported */
      }
    };
    const onVis = () => document.visibilityState === "visible" && void acquire();
    void acquire();
    document.addEventListener("visibilitychange", onVis);
    return () => {
      disposed = true;
      document.removeEventListener("visibilitychange", onVis);
      void lock?.release().catch(() => {});
    };
  }, [on]);
}
