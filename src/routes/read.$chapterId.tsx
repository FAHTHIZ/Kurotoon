import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/lib/auth";
import { useEffect, useRef, useState, useCallback } from "react";
import { ChevronLeft, ChevronRight, ArrowLeft, List, Maximize, Minimize, Download, CheckCircle2 } from "lucide-react";
import { downloadChapter, isChapterDownloaded, removeChapter, pwaEnabled } from "@/lib/pwa";
import { bumpMission } from "@/lib/kuro";
import { toast } from "sonner";
import { proxied } from "@/lib/image";
import { getChapter } from "@/lib/catalog";
import { setProgress, recordHistory } from "@/lib/local-library";
import { ProgressOrb } from "@/components/ProgressOrb";
import { ChapterComplete } from "@/components/ChapterComplete";
import { AtmospherePlayer } from "@/components/AtmospherePlayer";
import { analyzeAtmosphere } from "@/lib/atmosphere";

export const Route = createFileRoute("/read/$chapterId")({
  component: Reader,
});

type Mode = "vertical" | "paged";

function Reader() {
  const { chapterId } = Route.useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>("vertical");
  const [pageIdx, setPageIdx] = useState(0);
  const [hideUI, setHideUI] = useState(false);
  const [fs, setFs] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const [scrollPct, setScrollPct] = useState(0);
  const [celebrated, setCelebrated] = useState<string | null>(null);
  const [showComplete, setShowComplete] = useState(false);

  // Vertical reading progress drives the orb.
  useEffect(() => {
    const onScroll = () => {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      setScrollPct(max > 0 ? Math.min(1, window.scrollY / max) : 0);
    };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [chapterId]);

  // One live request: metadata + fresh page tokens + sibling chapters.
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["md", "chapter", chapterId],
    queryFn: () => getChapter(chapterId),
    // Upstream page tokens expire ~15 min after issue.
    staleTime: 10 * 60_000,
    gcTime: 15 * 60_000,
  });

  const chapter = data?.chapter;
  const pages = data?.pages;
  const siblings = data?.siblings;

  useEffect(() => { setPageIdx(0); }, [chapterId]);

  // Self-heal expired image tokens by re-requesting the live payload.
  const refreshingRef = useRef(false);
  const refreshedAtRef = useRef(0);
  const refreshPages = useCallback(async () => {
    if (refreshingRef.current) return;
    if (Date.now() - refreshedAtRef.current < 5000) return;
    refreshingRef.current = true;
    try {
      refreshedAtRef.current = Date.now();
      await refetch();
    } finally {
      refreshingRef.current = false;
    }
  }, [refetch]);

  const handleImgError = useCallback((e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget;
    if (img.dataset["retried"] === "1") return;
    img.dataset["retried"] = "1";
    void refreshPages();
  }, [refreshPages]);

  const idx = siblings?.findIndex((c) => c.id === chapterId) ?? -1;
  const prevCh = idx > 0 ? siblings?.[idx - 1] : null;
  const nextCh = siblings && idx >= 0 && idx < siblings.length - 1 ? siblings[idx + 1] : null;

  // Progress is device-local (nothing about the catalogue is stored server-side).
  const countedRef = useRef<string | null>(null);
  useEffect(() => {
    if (!chapter?.series_id) return;
    const t = setTimeout(() => {
      setProgress(chapter.series_id!, {
        chapterId: chapter.id,
        chapterNumber: chapter.number ?? "",
        page: pageIdx + 1,
        at: Date.now(),
      });
      recordHistory({
        seriesId: chapter.series_id!,
        chapterId: chapter.id,
        chapterNumber: chapter.number ?? "",
        at: Date.now(),
      });
    }, 600);
    const atEnd = mode === "paged" ? !!pages && pageIdx >= pages.length - 1 : scrollPct > 0.985;
    if (atEnd && celebrated !== chapter.id) {
      setCelebrated(chapter.id);
      setShowComplete(true);
    }
    if (user && pages && pageIdx >= pages.length - 1 && countedRef.current !== chapter.id) {
      countedRef.current = chapter.id;
      bumpMission(user.id, "read_5_chapters").catch(() => {});
    }
    return () => clearTimeout(t);
  }, [user, chapter, pageIdx, pages, mode, scrollPct, celebrated]);

  const goNext = useCallback(() => {
    if (!pages) return;
    if (pageIdx < pages.length - 1) setPageIdx((p) => p + 1);
    else if (nextCh) navigate({ to: "/read/$chapterId", params: { chapterId: nextCh.id } });
  }, [pages, pageIdx, nextCh, navigate]);

  const goPrev = useCallback(() => {
    if (pageIdx > 0) setPageIdx((p) => p - 1);
    else if (prevCh) navigate({ to: "/read/$chapterId", params: { chapterId: prevCh.id } });
  }, [pageIdx, prevCh, navigate]);

  useEffect(() => {
    if (mode !== "paged") return;
    const fn = (e: KeyboardEvent) => {
      if (e.key === "ArrowRight" || e.key === " ") { e.preventDefault(); goNext(); }
      if (e.key === "ArrowLeft") { e.preventDefault(); goPrev(); }
    };
    window.addEventListener("keydown", fn);
    return () => window.removeEventListener("keydown", fn);
  }, [mode, goNext, goPrev]);

  const toggleFs = async () => {
    if (!document.fullscreenElement) { await containerRef.current?.requestFullscreen?.(); setFs(true); }
    else { await document.exitFullscreen?.(); setFs(false); }
  };

  const [downloaded, setDownloaded] = useState(false);
  const [dlProgress, setDlProgress] = useState<{ done: number; total: number } | null>(null);
  useEffect(() => { isChapterDownloaded(chapterId).then(setDownloaded); }, [chapterId]);
  useEffect(() => {
    const onP = (e: Event) => {
      const d = (e as CustomEvent).detail;
      if (d.chapterId === chapterId) setDlProgress({ done: d.done, total: d.total });
    };
    window.addEventListener("kt-download-progress", onP);
    return () => window.removeEventListener("kt-download-progress", onP);
  }, [chapterId]);

  const handleDownload = async () => {
    if (!pages) return;
    if (!pwaEnabled()) {
      toast.warning("Offline downloads only work on the published app, not in the editor preview.");
      return;
    }
    setDlProgress({ done: 0, total: pages.length });
    try {
      await downloadChapter(chapterId, pages.map((p) => proxied(p.image_url)));
      setDownloaded(true);
      toast.success("Chapter saved for offline reading");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Download failed");
    } finally {
      setTimeout(() => setDlProgress(null), 1200);
    }
  };

  const handleRemove = async () => {
    await removeChapter(chapterId);
    setDownloaded(false);
    toast.success("Removed from offline storage");
  };

  if (isLoading) {
    return <div className="grid min-h-screen place-items-center bg-background text-muted-foreground">Loading reader…</div>;
  }

  if (error || !chapter || !pages || pages.length === 0) {
    return (
      <div className="grid min-h-screen place-items-center bg-background px-6 text-center">
        <div>
          <p className="text-sm text-muted-foreground">This chapter couldn’t be loaded from the source right now.</p>
          <div className="mt-4 flex justify-center gap-2">
            <button onClick={() => void refetch()} className="rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground">Retry</button>
            <Link to="/browse" className="rounded-full bg-[color:var(--surface)] px-4 py-2 text-xs font-semibold ring-1 ring-border">Browse</Link>
          </div>
        </div>
      </div>
    );
  }

  const seriesLink = chapter.series_id;

  return (
    <div ref={containerRef} className="min-h-screen bg-black text-white">
      <header className={`fixed inset-x-0 top-0 z-40 transition-transform ${hideUI ? "-translate-y-full" : ""} bg-black/80 backdrop-blur`}>
        <div className="mx-auto flex max-w-3xl items-center gap-2 px-3 py-2.5">
          {seriesLink ? (
            <Link to="/series/$slug" params={{ slug: seriesLink }} className="grid h-9 w-9 place-items-center rounded-full bg-white/5 hover:bg-white/10">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          ) : (
            <Link to="/browse" className="grid h-9 w-9 place-items-center rounded-full bg-white/5 hover:bg-white/10">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          )}
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs text-white/60">{chapter.series_title}</p>
            <p className="truncate text-sm font-semibold">Chapter {chapter.number}{chapter.title ? ` — ${chapter.title}` : ""}</p>
          </div>
          <button onClick={() => setMode((m) => (m === "vertical" ? "paged" : "vertical"))} className="rounded-full bg-white/5 px-3 py-1.5 text-[11px] font-semibold hover:bg-white/10">
            {mode === "vertical" ? "Vertical" : "Paged"}
          </button>
          {downloaded ? (
            <button onClick={handleRemove} title="Remove offline copy" className="grid h-9 w-9 place-items-center rounded-full bg-primary/20 text-primary hover:bg-primary/30">
              <CheckCircle2 className="h-4 w-4" />
            </button>
          ) : (
            <button onClick={handleDownload} disabled={!!dlProgress} title="Download for offline" className="grid h-9 w-9 place-items-center rounded-full bg-white/5 hover:bg-white/10 disabled:opacity-50">
              <Download className="h-4 w-4" />
            </button>
          )}
          <button onClick={toggleFs} className="grid h-9 w-9 place-items-center rounded-full bg-white/5 hover:bg-white/10">
            {fs ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}
          </button>
        </div>
        {dlProgress && (
          <div className="h-0.5 bg-white/10">
            <div className="h-full bg-primary transition-all" style={{ width: `${(dlProgress.done / dlProgress.total) * 100}%` }} />
          </div>
        )}
      </header>

      {mode === "vertical" ? (
        <div className="mx-auto max-w-3xl pb-32 pt-14" onClick={() => setHideUI((h) => !h)}>
          {pages.map((p, i) => (
            <img
              key={p.page_number}
              src={proxied(p.image_url)}
              alt={`Page ${p.page_number}`}
              loading={i < 2 ? "eager" : "lazy"}
              fetchPriority={i < 2 ? "high" : "auto"}
              decoding="async"
              referrerPolicy="no-referrer"
              onError={handleImgError}
              className="kt-cv-page mx-auto block w-full select-none"
            />

          ))}
          <div className="mx-auto mt-8 flex max-w-md gap-2 px-4">
            {prevCh && (
              <Link to="/read/$chapterId" params={{ chapterId: prevCh.id }} className="flex-1 rounded-full bg-white/10 py-3 text-center text-sm font-semibold hover:bg-white/20">
                ← Prev
              </Link>
            )}
            {nextCh ? (
              <Link to="/read/$chapterId" params={{ chapterId: nextCh.id }} className="flex-1 rounded-full bg-primary py-3 text-center text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)]">
                Next →
              </Link>
            ) : seriesLink ? (
              <Link to="/series/$slug" params={{ slug: seriesLink }} className="flex-1 rounded-full bg-white/10 py-3 text-center text-sm font-semibold">
                Back to series
              </Link>
            ) : null}
          </div>
        </div>
      ) : (
        <div className="relative grid min-h-screen place-items-center pt-14" onClick={() => setHideUI((h) => !h)}>
          <img
            src={proxied(pages[pageIdx]?.image_url)}
            alt={`Page ${pageIdx + 1}`}
            referrerPolicy="no-referrer"
            onError={handleImgError}
            className="max-h-[100dvh] max-w-full select-none object-contain"
          />
          {pages[pageIdx + 1] && <link rel="preload" as="image" href={proxied(pages[pageIdx + 1]!.image_url)} />}
          <button onClick={(e) => { e.stopPropagation(); goPrev(); }} className="absolute inset-y-0 left-0 w-1/3" aria-label="Previous" />
          <button onClick={(e) => { e.stopPropagation(); goNext(); }} className="absolute inset-y-0 right-0 w-1/3" aria-label="Next" />
        </div>
      )}

      <footer className={`fixed inset-x-0 bottom-0 z-40 transition-transform ${hideUI ? "translate-y-full" : ""} bg-black/80 backdrop-blur pb-[env(safe-area-inset-bottom)]`}>
        <div className="mx-auto flex max-w-3xl items-center gap-2 px-3 py-2.5">
          <button disabled={!prevCh} onClick={() => prevCh && navigate({ to: "/read/$chapterId", params: { chapterId: prevCh.id } })} className="grid h-9 w-9 place-items-center rounded-full bg-white/5 disabled:opacity-30">
            <ChevronLeft className="h-4 w-4" />
          </button>
          <div className="flex-1 text-center text-xs text-white/60">
            {mode === "paged" ? `${pageIdx + 1} / ${pages.length}` : `${pages.length} pages`}
          </div>
          {seriesLink && (
            <Link to="/series/$slug" params={{ slug: seriesLink }} className="grid h-9 w-9 place-items-center rounded-full bg-white/5">
              <List className="h-4 w-4" />
            </Link>
          )}
          <button disabled={!nextCh} onClick={() => nextCh && navigate({ to: "/read/$chapterId", params: { chapterId: nextCh.id } })} className="grid h-9 w-9 place-items-center rounded-full bg-white/5 disabled:opacity-30">
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
        {mode === "paged" && (
          <div className="h-1 bg-white/10">
            <div className="h-full bg-primary transition-all" style={{ width: `${((pageIdx + 1) / pages.length) * 100}%` }} />
          </div>
        )}
      </footer>

      <div className="fixed right-4 top-16 z-50">
        <AtmospherePlayer
          profile={analyzeAtmosphere({
            title: chapter.series_title ?? "",
            chapterTitle: chapter.title ?? null,
          })}
        />
      </div>

      <ProgressOrb
        progress={mode === "paged" ? (pageIdx + 1) / pages.length : scrollPct}
        label={mode === "paged" ? `${pageIdx + 1}/${pages.length}` : `${Math.round(scrollPct * 100)}%`}
        dimmed={hideUI}
        onClick={() => setHideUI((h) => !h)}
      />

      <ChapterComplete
        open={showComplete}
        chapterNumber={chapter.number ?? ""}
        seriesTitle={chapter.series_title}
        seriesSlug={seriesLink ?? null}
        nextChapterId={nextCh?.id ?? null}
        onDismiss={() => setShowComplete(false)}
        onRestart={() => {
          setShowComplete(false);
          setPageIdx(0);
          window.scrollTo({ top: 0, behavior: "smooth" });
        }}
      />
    </div>
  );
}
