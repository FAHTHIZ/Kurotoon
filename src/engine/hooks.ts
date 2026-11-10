import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { browseListing, chapterList, mangaDetails, pageList, popular, search } from "./runtime";
import { bootEngine, useEngine } from "./store";
import type { Chapter, Listing } from "./types";

const isClient = typeof window !== "undefined";

/** Boots persisted engine state on mount and returns it reactively. */
export function useEngineReady() {
  const s = useEngine();
  useEffect(() => {
    void bootEngine();
  }, []);
  return s;
}

export function usePopular(sourceId: string | null, enabled = true) {
  return useQuery({
    queryKey: ["src", sourceId, "popular"],
    queryFn: () => popular(sourceId!, 1),
    enabled: isClient && !!sourceId && enabled,
    staleTime: 10 * 60_000,
    gcTime: 30 * 60_000,
    retry: 1,
  });
}

export function useSourceFeed(sourceId: string, mode: { listing: Listing } | { query: string }) {
  const key = "listing" in mode ? ["listing", mode.listing.id] : ["search", mode.query];
  return useInfiniteQuery({
    queryKey: ["src", sourceId, ...key],
    initialPageParam: 1,
    queryFn: ({ pageParam }) =>
      "listing" in mode ? browseListing(sourceId, mode.listing, pageParam) : search(sourceId, mode.query || null, pageParam),
    getNextPageParam: (last, all) => (last.hasNextPage && last.items.length > 0 ? all.length + 1 : undefined),
    enabled: isClient,
    staleTime: 5 * 60_000,
    retry: 1,
  });
}

export function useMangaDetails(sourceId: string, key: string) {
  return useQuery({
    queryKey: ["src", sourceId, "manga", key],
    queryFn: () => mangaDetails(sourceId, key),
    enabled: isClient,
    staleTime: 15 * 60_000,
    retry: 1,
  });
}

export function useChapters(sourceId: string, key: string) {
  return useQuery({
    queryKey: ["src", sourceId, "chapters", key],
    queryFn: () => chapterList(sourceId, key),
    enabled: isClient,
    staleTime: 5 * 60_000,
    gcTime: 60 * 60_000,
    retry: 1,
  });
}

export function usePages(sourceId: string, mangaKey: string, chapter: Chapter | null) {
  return useQuery({
    queryKey: ["src", sourceId, "pages", mangaKey, chapter?.key],
    queryFn: () => pageList(sourceId, mangaKey, chapter!),
    enabled: isClient && !!chapter,
    // Signed page URLs often expire; keep them briefly.
    staleTime: 8 * 60_000,
    gcTime: 12 * 60_000,
    retry: 1,
  });
}

/** True once mounted on the client (avoids SSR/CSR mismatch for device-only data). */
export function useMounted() {
  const [m, setM] = useState(false);
  useEffect(() => setM(true), []);
  return m;
}
