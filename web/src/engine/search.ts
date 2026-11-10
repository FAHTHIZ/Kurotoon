/**
 * Universal search — Mihon's "global search": one query fans out to every
 * active source in parallel (bounded concurrency), results stream in per
 * source as they arrive, and slow or failing sources never block the rest.
 */
import { activeSources, getEngineState, healthLabel } from "./store";
import { search } from "./runtime";
import type { SeriesCard } from "./types";

export type SourceResult = {
  sourceId: string;
  sourceName: string;
  status: "pending" | "done" | "error";
  items: SeriesCard[];
  error: string | null;
  ms: number | null;
};

const CONCURRENCY = 3;

export function universalSearch(
  query: string,
  onUpdate: (results: SourceResult[]) => void,
  signal?: AbortSignal,
): Promise<SourceResult[]> {
  const state = getEngineState();
  // Healthy sources first so the screen fills quickly.
  const order = { healthy: 0, unknown: 1, degraded: 2, failing: 3 } as const;
  const sources = activeSources(state).sort(
    (a, b) => order[healthLabel(state.health[a.id])] - order[healthLabel(state.health[b.id])],
  );
  const results: SourceResult[] = sources.map((s) => ({
    sourceId: s.id,
    sourceName: s.name,
    status: "pending",
    items: [],
    error: null,
    ms: null,
  }));
  onUpdate([...results]);
  let next = 0;
  const worker = async () => {
    while (next < sources.length && !signal?.aborted) {
      const i = next++;
      const s = sources[i]!;
      const t0 = performance.now();
      try {
        const r = await search(s.id, query, 1);
        const prefs = getEngineState().prefs;
        results[i] = {
          ...results[i]!,
          status: "done",
          items: r.items.filter((m) => (m.contentRating === 2 ? prefs.showAdult : m.contentRating === 1 ? prefs.showSuggestive : true)),
          ms: Math.round(performance.now() - t0),
        };
      } catch (e) {
        results[i] = { ...results[i]!, status: "error", error: e instanceof Error ? e.message : String(e), ms: Math.round(performance.now() - t0) };
      }
      if (!signal?.aborted) onUpdate([...results]);
    }
  };
  return Promise.all(Array.from({ length: Math.min(CONCURRENCY, sources.length) }, worker)).then(() => results);
}
