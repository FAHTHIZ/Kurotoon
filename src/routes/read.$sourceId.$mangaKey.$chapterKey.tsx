import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { AnimatePresence, motion } from "framer-motion";
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ArrowLeft, ChevronLeft, ChevronRight, ImageOff, Loader2, Maximize, Minimize, RefreshCcw, Settings2, X } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { bumpMission } from "@/lib/kuro";
import { chapterLabel, getProgress, markChapterRead, recordHistory, rememberSeries, setProgress, getSeen } from "@/lib/local-library";
import { useReaderSettings, useWakeLock, type ReaderMode } from "@/lib/reader-settings";
import { ProgressOrb } from "@/components/ProgressOrb";
import { ChapterComplete } from "@/components/ChapterComplete";
import { AtmospherePlayer } from "@/components/AtmospherePlayer";
import { analyzeAtmosphere } from "@/lib/atmosphere";
import { spring } from "@/lib/motion";
import { useChapters, useMangaDetails, usePages } from "@/engine/hooks";
import { decodeKey, encodeKey, seriesId } from "@/engine/ids";
import { pageList, resolvePage, type ResolvedImage } from "@/engine/runtime";
import type { ChapterRow, Page } from "@/engine/types";

export const Route = createFileRoute("/read/$sourceId/$mangaKey/$chapterKey")({
  head: () => ({ meta: [{ title: "Reading — KuroToon" }] }),
  component: Reader,
});

const MODES: Array<{ id: ReaderMode; label: string }> = [
  { id: "webtoon", label: "Webtoon" },
  { id: "vertical", label: "Vertical" },
  { id: "ltr", label: "Left to right" },
  { id: "rtl", label: "Right to left" },
];

const BG = { black: "bg-black", gray: "bg-neutral-800", white: "bg-white" } as const;

function Reader() {
  const { sourceId, mangaKey: encManga, chapterKey: encChapter } = Route.useParams();
  const mangaKey = useMemo(() => decodeKey(encManga), [encManga]);
  const chapterKey = useMemo(() => decodeKey(encChapter), [encChapter]);
  const sid = seriesId(sourceId, mangaKey);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { user } = useAuth();

  const [settings, update] = useReaderSettings(sid);
  useWakeLock(settings.keepAwake);
  const paged = settings.mode === "ltr" || settings.mode === "rtl";

  const chapters = useChapters(sourceId, mangaKey);
  const details = useMangaDetails(sourceId, mangaKey);
  const list = chapters.data ?? [];
  const idx = list.findIndex((c) => c.key === chapterKey);
  const chapter: ChapterRow | null = idx >= 0 ? list[idx]! : null;
  // Sources list newest first: "next" in reading order is the previous index.
  const nextCh = idx > 0 ? list.slice(0, idx).reverse().find((c) => !c.locked) ?? null : null;
  const prevCh = idx >= 0 ? list.slice(idx + 1).find((c) => !c.locked) ?? null : null;

  const pagesQ = usePages(sourceId, mangaKey, chapter?.raw ?? null);
  const pages = pagesQ.data ?? [];

  const [hideUI, setHideUI] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [current, setCurrent] = useState(0);
  const [showComplete, setShowComplete] = useState(false);
  const [fs, setFs] = useState(false);
  const celebrated = useRef<string | null>(null);
  const restored = useRef<string | null>(null);
  const counted = useRef<string | null>(null);

  const series = details.data ?? (typeof window !== "undefined" ? getSeen(sid) : null);
  useEffect(() => {
    if (details.data) rememberSeries(details.data);
  }, [details.data]);

  const goChapter = useCallback(
    (c: ChapterRow | null) => {
      if (!c) return;
      navigate({ to: "/read/$sourceId/$mangaKey/$chapterKey", params: { sourceId, mangaKey: encManga, chapterKey: encodeKey(c.key) } });
    },
    [navigate, sourceId, encManga],
  );
  const goSeries = useCallback(
    () => navigate({ to: "/manga/$sourceId/$key", params: { sourceId, key: encManga } }),
    [navigate, sourceId, encManga],
  );

  // Reset per chapter; restore saved position once pages are known.
  useEffect(() => {
    setCurrent(0);
    setShowComplete(false);
    window.scrollTo({ top: 0 });
  }, [chapterKey]);

  useEffect(() => {
    if (!pages.length || restored.current === chapterKey) return;
    restored.current = chapterKey;
    const p = getProgress(sid);
    if (p && p.chapterKey === chapterKey && p.page > 1 && p.page < pages.length) {
      const target = p.page - 1;
      setCurrent(target);
      if (!paged) requestAnimationFrame(() => document.getElementById(`kt-page-${target}`)?.scrollIntoView({ block: "start" }));
    }
  }, [pages.length, chapterKey, sid, paged]);

  // Persist progress + history (debounced).
  useEffect(() => {
    if (!chapter || !pages.length) return;
    const t = window.setTimeout(() => {
      const label = chapterLabel(chapter);
      setProgress(sid, { chapterKey: chapter.key, chapterLabel: label, page: current + 1, total: pages.length, at: Date.now() });
      recordHistory({ seriesId: sid, chapterKey: chapter.key, chapterLabel: label, at: Date.now() });
    }, 500);
    return () => window.clearTimeout(t);
  }, [sid, chapter, current, pages.length]);

  // Finished: mark read, celebrate, count mission, prefetch next chapter.
  const atEnd = pages.length > 0 && current >= pages.length - 1;
  useEffect(() => {
    if (!chapter || !pages.length) return;
    if (current >= pages.length - 3 && nextCh) {
      void qc.prefetchQuery({
        queryKey: ["src", sourceId, "pages", mangaKey, nextCh.key],
        queryFn: () => pageList(sourceId, mangaKey, nextCh.raw),
        staleTime: 8 * 60_000,
      });
    }
    if (atEnd && celebrated.current !== chapter.key) {
      celebrated.current = chapter.key;
      markChapterRead(sid, chapter.key);
      setShowComplete(true);
      if (user && counted.current !== chapter.key) {
        counted.current = chapter.key;
        bumpMission(user.id, "read_5_chapters").catch(() => {});
      }
    }
  }, [atEnd, current, chapter, pages.length, nextCh, qc, sourceId, mangaKey, sid, user]);

  // Paged navigation
  const step = useCallback(
    (dir: 1 | -1) => {
      if (!pages.length) return;
      const n = current + dir;
      if (n < 0) return goChapter(prevCh);
      if (n >= pages.length) return nextCh ? goChapter(nextCh) : setShowComplete(true);
      setCurrent(n);
      try {
        navigator.vibrate?.(4);
      } catch {
        /* noop */
      }
    },
    [current, pages.length, goChapter, prevCh, nextCh],
  );

  useEffect(() => {
    const fn = (e: KeyboardEvent) => {
      if (!paged) return;
      const forward = settings.mode === "rtl" ? "ArrowLeft" : "ArrowRight";
      const back = settings.mode === "rtl" ? "ArrowRight" : "ArrowLeft";
      if (e.key === forward || e.key === " ") {
        e.preventDefault();
        step(1);
      } else if (e.key === back) {
        e.preventDefault();
        step(-1);
      }
    };
    window.addEventListener("keydown", fn);
    return () => window.removeEventListener("keydown", fn);
  }, [paged, settings.mode, step]);

  const toggleFs = async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen?.();
        setFs(true);
      } else {
        await document.exitFullscreen?.();
        setFs(false);
      }
    } catch {
      /* unsupported */
    }
  };

  const onVisible = useCallback((i: number) => setCurrent((c) => (paged ? c : i)), [paged]);

  const loading = chapters.isLoading || (chapter && pagesQ.isLoading);
  const failed = chapters.error || pagesQ.error || (!chapters.isLoading && !chapter);
  const bg = BG[settings.background];

  if (loading) {
    return (
      <div className="grid min-h-[100dvh] place-items-center bg-black text-white/60">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
          <p className="text-xs">{chapter ? `Loading ${chapterLabel(chapter)}…` : "Opening chapter…"}</p>
        </div>
      </div>
    );
  }

  if (failed || pages.length === 0) {
    const err = (pagesQ.error ?? chapters.error) as Error | null;
    return (
      <div className="grid min-h-[100dvh] place-items-center bg-black px-6 text-center text-white">
        <div>
          <p className="text-sm text-white/70">
            {chapter?.locked ? "This chapter is locked on the source." : err ? `The source failed: ${err.message}` : "No pages in this chapter."}
          </p>
          <div className="mt-4 flex justify-center gap-2">
            <button onClick={() => void pagesQ.refetch()} className="rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground">
              Retry
            </button>
            <button onClick={goSeries} className="rounded-full bg-white/10 px-4 py-2 text-xs font-semibold">
              Chapters
            </button>
          </div>
        </div>
      </div>
    );
  }

  const title = series?.title ?? "";
  const label = chapter ? chapterLabel(chapter) : "";
  const progress = pages.length ? (current + 1) / pages.length : 0;

  return (
    <div className={`min-h-[100dvh] ${bg} text-white`}>
      <AnimatePresence>
        {!hideUI && (
          <motion.header
            initial={{ y: -60, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: -60, opacity: 0 }}
            transition={spring.surface}
            className="fixed inset-x-0 top-0 z-40 bg-black/80 pt-[env(safe-area-inset-top)] backdrop-blur"
          >
            <div className="mx-auto flex max-w-3xl items-center gap-2 px-3 py-2.5">
              <button onClick={goSeries} aria-label="Back to series" className="grid h-9 w-9 place-items-center rounded-full bg-white/5 hover:bg-white/10">
                <ArrowLeft className="h-4 w-4" />
              </button>
              <div className="min-w-0 flex-1">
                <p className="truncate text-xs text-white/60">{title}</p>
                <p className="truncate text-sm font-semibold">
                  {label}
                  {chapter?.number != null && chapter.title ? ` — ${chapter.title}` : ""}
                </p>
              </div>
              <button onClick={() => setShowSettings(true)} aria-label="Reader settings" className="grid h-9 w-9 place-items-center rounded-full bg-white/5 hover:bg-white/10">
                <Settings2 className="h-4 w-4" />
              </button>
              <button onClick={toggleFs} aria-label="Fullscreen" className="grid h-9 w-9 place-items-center rounded-full bg-white/5 hover:bg-white/10">
                {fs ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}
              </button>
            </div>
          </motion.header>
        )}
      </AnimatePresence>

      {paged ? (
        <PagedView
          sourceId={sourceId}
          pages={pages}
          index={current}
          rtl={settings.mode === "rtl"}
          fit={settings.fit}
          tapZones={settings.tapZones}
          onStep={step}
          onToggleUI={() => setHideUI((h) => !h)}
        />
      ) : (
        <div
          className="mx-auto max-w-3xl"
          style={{ paddingLeft: `${settings.sidePadding}%`, paddingRight: `${settings.sidePadding}%` }}
          onClick={() => setHideUI((h) => !h)}
        >
          {pages.map((p, i) => (
            <div key={i} id={`kt-page-${i}`} className={settings.mode === "vertical" ? "mb-2" : ""}>
              <PageImage sourceId={sourceId} page={p} index={i} eager={i < 3} onVisible={onVisible} />
            </div>
          ))}
          <div className="mx-auto flex max-w-md gap-2 px-4 pb-32 pt-8" onClick={(e) => e.stopPropagation()}>
            {prevCh && (
              <button onClick={() => goChapter(prevCh)} className="flex-1 rounded-full bg-white/10 py-3 text-sm font-semibold hover:bg-white/20">
                ← {chapterLabel(prevCh)}
              </button>
            )}
            {nextCh ? (
              <button
                onClick={() => goChapter(nextCh)}
                className="flex-1 rounded-full bg-primary py-3 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)]"
              >
                {chapterLabel(nextCh)} →
              </button>
            ) : (
              <button onClick={goSeries} className="flex-1 rounded-full bg-white/10 py-3 text-sm font-semibold">
                Back to series
              </button>
            )}
          </div>
        </div>
      )}

      <AnimatePresence>
        {!hideUI && (
          <motion.footer
            initial={{ y: 80, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: 80, opacity: 0 }}
            transition={spring.surface}
            className="fixed inset-x-0 bottom-0 z-40 bg-black/80 pb-[env(safe-area-inset-bottom)] backdrop-blur"
          >
            <div className="mx-auto flex max-w-3xl items-center gap-2 px-3 py-2.5">
              <button
                disabled={!prevCh}
                onClick={() => goChapter(prevCh)}
                aria-label="Previous chapter"
                className="grid h-9 w-9 place-items-center rounded-full bg-white/5 disabled:opacity-30"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <input
                type="range"
                min={0}
                max={Math.max(0, pages.length - 1)}
                value={current}
                aria-label="Page"
                onChange={(e) => {
                  const n = Number(e.target.value);
                  setCurrent(n);
                  if (!paged) document.getElementById(`kt-page-${n}`)?.scrollIntoView({ block: "start" });
                }}
                className="flex-1 accent-[color:var(--primary)]"
                style={settings.mode === "rtl" ? { direction: "rtl" } : undefined}
              />
              <span className="w-14 text-center text-xs tabular-nums text-white/70">
                {current + 1}/{pages.length}
              </span>
              <button
                disabled={!nextCh}
                onClick={() => goChapter(nextCh)}
                aria-label="Next chapter"
                className="grid h-9 w-9 place-items-center rounded-full bg-white/5 disabled:opacity-30"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </motion.footer>
        )}
      </AnimatePresence>

      {!hideUI && (
        <div className="fixed right-4 top-20 z-50">
          <AtmospherePlayer profile={analyzeAtmosphere({ title, genres: series?.tags, chapterTitle: chapter?.title ?? null })} />
        </div>
      )}

      <ProgressOrb progress={progress} label={`${current + 1}/${pages.length}`} dimmed={hideUI} onClick={() => setHideUI((h) => !h)} />

      <ChapterComplete
        open={showComplete}
        chapterNumber={chapter?.number != null ? String(chapter.number) : label}
        seriesTitle={title}
        onNext={nextCh ? () => goChapter(nextCh) : null}
        onChapters={goSeries}
        onDismiss={() => setShowComplete(false)}
        onRestart={() => {
          setShowComplete(false);
          setCurrent(0);
          window.scrollTo({ top: 0, behavior: "smooth" });
        }}
      />

      <AnimatePresence>
        {showSettings && <SettingsSheet settings={settings} update={update} onClose={() => setShowSettings(false)} />}
      </AnimatePresence>

    </div>
  );
}

/* --------------------------------- pages --------------------------------- */

type Loaded = { kind: "img"; src: string; fallback: string | null } | { kind: "text"; text: string };

/**
 * Resolved pages are shared between the visible page and its preloads, so a
 * page is fetched (and, for scrambled sources, decoded) once. Blob URLs are
 * revoked on eviction.
 */
const PAGE_CACHE_MAX = 30;
const pageCache = new Map<string, Promise<ResolvedImage>>();

function resolveCached(sourceId: string, page: Page): Promise<ResolvedImage> {
  const k = `${sourceId}|${page.url ?? ""}|${page.base64?.length ?? 0}|${page.text?.slice(0, 32) ?? ""}`;
  const hit = pageCache.get(k);
  if (hit) {
    pageCache.delete(k);
    pageCache.set(k, hit);
    return hit;
  }
  const p = resolvePage(sourceId, page);
  p.catch(() => pageCache.delete(k));
  pageCache.set(k, p);
  while (pageCache.size > PAGE_CACHE_MAX) {
    const [oldKey, oldVal] = pageCache.entries().next().value as [string, Promise<ResolvedImage>];
    pageCache.delete(oldKey);
    void oldVal.then((r) => r.kind === "blob" && window.setTimeout(r.revoke, 5000)).catch(() => {});
  }
  return p;
}

function usePageImage(sourceId: string, page: Page, active: boolean) {
  const [state, setState] = useState<Loaded | "loading" | "error">("loading");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (!active) return;
    let cancelled = false;
    setState("loading");
    resolveCached(sourceId, page)
      .then((r) => {
        if (cancelled) return;
        setState(r.kind === "text" ? { kind: "text", text: r.text } : { kind: "img", src: r.src, fallback: r.fallback });
      })
      .catch(() => !cancelled && setState("error"));
    return () => {
      cancelled = true;
    };
  }, [sourceId, page, active, attempt]);
  const retry = () => setAttempt((a) => a + 1);
  const fallback = () =>
    setState((s) => (typeof s === "object" && s.kind === "img" && s.fallback ? { kind: "img", src: s.fallback, fallback: null } : "error"));
  return { state, retry, fallback };
}

const PageImage = memo(function PageImage({
  sourceId,
  page,
  index,
  eager,
  onVisible,
}: {
  sourceId: string;
  page: Page;
  index: number;
  eager: boolean;
  onVisible: (i: number) => void;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [near, setNear] = useState(eager);
  const [loaded, setLoaded] = useState(false);
  const { state, retry, fallback } = usePageImage(sourceId, page, near);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (e.isIntersecting) setNear(true);
        }
      },
      { rootMargin: "2500px 0px" },
    );
    const seen = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) onVisible(index);
      },
      { rootMargin: "-45% 0px -45% 0px" },
    );
    io.observe(el);
    seen.observe(el);
    return () => {
      io.disconnect();
      seen.disconnect();
    };
  }, [index, onVisible]);

  return (
    <div ref={ref} className="relative" style={loaded ? undefined : { minHeight: "70vh" }}>
      {typeof state === "object" && state.kind === "text" ? (
        <p className="whitespace-pre-line p-6 text-sm leading-relaxed text-white/90">{state.text}</p>
      ) : typeof state === "object" ? (
        <img
          src={state.src}
          alt={`Page ${index + 1}`}
          decoding="async"
          loading={eager ? "eager" : "lazy"}
          fetchPriority={eager ? "high" : "auto"}
          referrerPolicy="no-referrer"
          draggable={false}
          onLoad={() => setLoaded(true)}
          onError={fallback}
          className="kt-cv-page mx-auto block w-full select-none"
        />
      ) : null}
      {!loaded && state !== "error" && !(typeof state === "object" && state.kind === "text") && (
        <div className="absolute inset-0 grid place-items-center">
          <span className="text-xs font-bold tabular-nums text-white/30">{index + 1}</span>
        </div>
      )}
      {state === "error" && (
        <div className="absolute inset-0 grid place-items-center">
          <button
            onClick={(e) => {
              e.stopPropagation();
              retry();
            }}
            className="flex flex-col items-center gap-2 text-white/60"
          >
            <ImageOff className="h-6 w-6" />
            <span className="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-1.5 text-xs font-semibold">
              <RefreshCcw className="h-3.5 w-3.5" /> Retry page {index + 1}
            </span>
          </button>
        </div>
      )}
    </div>
  );
});

function PagedImage({ sourceId, page, fit, hidden }: { sourceId: string; page: Page; fit: "screen" | "width"; hidden?: boolean }) {
  const { state, retry, fallback } = usePageImage(sourceId, page, true);
  if (hidden) {
    return typeof state === "object" && state.kind === "img" ? <img src={state.src} alt="" className="hidden" decoding="async" /> : null;
  }
  if (state === "loading") return <Loader2 className="h-6 w-6 animate-spin text-white/40" />;
  if (state === "error")
    return (
      <button onClick={retry} className="inline-flex items-center gap-1 rounded-full bg-white/10 px-3 py-1.5 text-xs font-semibold">
        <RefreshCcw className="h-3.5 w-3.5" /> Retry
      </button>
    );
  if (state.kind === "text") return <p className="max-w-prose whitespace-pre-line p-6 text-sm">{state.text}</p>;
  return (
    <img
      src={state.src}
      alt=""
      decoding="async"
      referrerPolicy="no-referrer"
      draggable={false}
      onError={fallback}
      className={`select-none ${fit === "screen" ? "max-h-[100dvh] max-w-full object-contain" : "w-full"}`}
    />
  );
}

function PagedView({
  sourceId,
  pages,
  index,
  rtl,
  fit,
  tapZones,
  onStep,
  onToggleUI,
}: {
  sourceId: string;
  pages: Page[];
  index: number;
  rtl: boolean;
  fit: "screen" | "width";
  tapZones: boolean;
  onStep: (d: 1 | -1) => void;
  onToggleUI: () => void;
}) {
  const touch = useRef<{ x: number; y: number } | null>(null);
  const page = pages[index];
  const onTap = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!tapZones) return onToggleUI();
    const x = e.clientX / window.innerWidth;
    if (x < 0.33) onStep(rtl ? 1 : -1);
    else if (x > 0.67) onStep(rtl ? -1 : 1);
    else onToggleUI();
  };
  return (
    <div
      className={`relative grid min-h-[100dvh] place-items-center ${fit === "width" ? "overflow-y-auto" : "overflow-hidden"}`}
      onClick={onTap}
      onTouchStart={(e) => (touch.current = { x: e.touches[0]!.clientX, y: e.touches[0]!.clientY })}
      onTouchEnd={(e) => {
        const t = touch.current;
        touch.current = null;
        if (!t) return;
        const dx = e.changedTouches[0]!.clientX - t.x;
        const dy = e.changedTouches[0]!.clientY - t.y;
        if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) {
          e.preventDefault();
          onStep(dx < 0 !== rtl ? 1 : -1);
        }
      }}
    >
      <AnimatePresence mode="popLayout" initial={false}>
        <motion.div
          key={index}
          initial={{ opacity: 0, x: rtl ? -24 : 24 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18 }}
          className="grid w-full place-items-center"
        >
          {page && <PagedImage sourceId={sourceId} page={page} fit={fit} />}
        </motion.div>
      </AnimatePresence>
      {/* Preload the next two pages */}
      {[1, 2].map((d) => pages[index + d] && <PagedImage key={index + d} sourceId={sourceId} page={pages[index + d]!} fit={fit} hidden />)}
    </div>
  );
}

/* -------------------------------- settings ------------------------------- */

function SettingsSheet({
  settings,
  update,
  onClose,
}: {
  settings: ReturnType<typeof useReaderSettings>[0];
  update: ReturnType<typeof useReaderSettings>[1];
  onClose: () => void;
}) {
  const paged = settings.mode === "ltr" || settings.mode === "rtl";
  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-[70] bg-black/60" onClick={onClose}>
      <motion.div
        initial={{ y: "100%" }}
        animate={{ y: 0 }}
        exit={{ y: "100%" }}
        transition={spring.surface}
        onClick={(e) => e.stopPropagation()}
        className="absolute inset-x-0 bottom-0 mx-auto max-w-lg space-y-4 rounded-t-3xl bg-[color:var(--surface)] p-5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] text-foreground ring-1 ring-white/10"
      >
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-black">Reader settings</h2>
          <button onClick={onClose} aria-label="Close" className="grid h-8 w-8 place-items-center rounded-full bg-white/5">
            <X className="h-4 w-4" />
          </button>
        </div>
        <Group label="Reading mode (saved for this series)">
          {MODES.map((m) => (
            <Chip key={m.id} on={settings.mode === m.id} onClick={() => update({ mode: m.id })}>
              {m.label}
            </Chip>
          ))}
        </Group>
        {paged ? (
          <>
            <Group label="Fit">
              <Chip on={settings.fit === "screen"} onClick={() => update({ fit: "screen" })}>
                Screen
              </Chip>
              <Chip on={settings.fit === "width"} onClick={() => update({ fit: "width" })}>
                Width
              </Chip>
            </Group>
            <Group label="Tap zones">
              <Chip on={settings.tapZones} onClick={() => update({ tapZones: true })}>
                On
              </Chip>
              <Chip on={!settings.tapZones} onClick={() => update({ tapZones: false })}>
                Off
              </Chip>
            </Group>
          </>
        ) : (
          <Group label={`Side padding · ${settings.sidePadding}%`}>
            <input
              type="range"
              min={0}
              max={25}
              step={5}
              value={settings.sidePadding}
              onChange={(e) => update({ sidePadding: Number(e.target.value) })}
              className="w-full accent-[color:var(--primary)]"
            />
          </Group>
        )}
        <Group label="Background">
          {(["black", "gray", "white"] as const).map((b) => (
            <Chip key={b} on={settings.background === b} onClick={() => update({ background: b })}>
              {b[0]!.toUpperCase() + b.slice(1)}
            </Chip>
          ))}
        </Group>
        <Group label="Keep screen on">
          <Chip on={settings.keepAwake} onClick={() => update({ keepAwake: true })}>
            On
          </Chip>
          <Chip on={!settings.keepAwake} onClick={() => update({ keepAwake: false })}>
            Off
          </Chip>
        </Group>
      </motion.div>
    </motion.div>
  );
}

function Group({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="mb-1.5 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">{label}</p>
      <div className="flex flex-wrap gap-1.5">{children}</div>
    </div>
  );
}

function Chip({ on, onClick, children }: { on: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <motion.button
      whileTap={{ scale: 0.94 }}
      transition={spring.tap}
      onClick={onClick}
      className={`rounded-full px-3 py-1.5 text-xs font-semibold ring-1 ${
        on ? "bg-primary text-primary-foreground ring-primary" : "bg-[color:var(--surface-2)] text-muted-foreground ring-border"
      }`}
    >
      {children}
    </motion.button>
  );
}
