import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { ArrowDownToLine, Loader2, RotateCcw, Trash2, X, HardDrive } from "lucide-react";
import { useMemo } from "react";
import { Cover } from "@/components/Cover";
import { KuroTip } from "@/components/Kuro";
import { encodeKey } from "@/engine/ids";
import {
  cancelDownload,
  clearFailed,
  deleteDownload,
  deleteSeriesDownloads,
  formatBytes,
  retryFailed,
  useDownloads,
  type DownloadMeta,
} from "@/lib/downloads";
import { revealUp, staggerContainer } from "@/lib/motion";

export const Route = createFileRoute("/downloads")({
  head: () => ({
    meta: [
      { title: "Downloads — KuroToon" },
      { name: "description", content: "Download queue and chapters saved for offline reading on this device." },
      { property: "og:title", content: "Downloads — KuroToon" },
      { property: "og:description", content: "Chapters saved for offline reading." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Downloads,
});

function Downloads() {
  const dl = useDownloads();
  const groups = useMemo(() => {
    const m = new Map<string, DownloadMeta[]>();
    for (const d of Object.values(dl.downloaded)) m.set(d.seriesId, [...(m.get(d.seriesId) ?? []), d]);
    return [...m.values()].map((list) => list.sort((a, b) => (a.chapter.number ?? 0) - (b.chapter.number ?? 0)));
  }, [dl.downloaded]);
  const total = Object.values(dl.downloaded).reduce((n, d) => n + d.bytes, 0);
  const failed = dl.queue.filter((q) => q.status === "error").length;

  return (
    <div className="space-y-5">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <h1 className="relative flex items-center gap-2 text-xl font-black tracking-tight">
          <ArrowDownToLine className="h-5 w-5 text-primary" /> Downloads
        </h1>
        <p className="relative mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
          <HardDrive className="h-3.5 w-3.5" /> {plural(Object.keys(dl.downloaded).length, "chapter")} · {formatBytes(total)} on this device
        </p>
      </header>

      {dl.queue.length > 0 && (
        <section className="space-y-2">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Queue ({dl.queue.length})</h2>
            {failed > 0 && (
              <div className="flex gap-1.5">
                <button onClick={retryFailed} className="inline-flex items-center gap-1 rounded-full bg-[color:var(--surface-2)] px-2.5 py-1 text-[11px] font-bold ring-1 ring-border">
                  <RotateCcw className="h-3 w-3" /> Retry
                </button>
                <button onClick={clearFailed} className="rounded-full bg-[color:var(--surface-2)] px-2.5 py-1 text-[11px] font-bold ring-1 ring-border">Clear failed</button>
              </div>
            )}
          </div>
          {dl.queue.map((q) => {
            const pct = q.total ? Math.round((q.done / q.total) * 100) : 0;
            return (
              <div key={q.id} className="kt-card flex items-center gap-3 rounded-2xl p-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-bold">{q.series.title}</p>
                  <p className={`truncate text-[11px] ${q.status === "error" ? "text-amber-400" : "text-muted-foreground"}`}>
                    {q.label} · {q.status === "downloading" ? `${q.done}/${q.total || "?"}` : q.status === "error" ? q.error : "Waiting"}
                  </p>
                  {q.status === "downloading" && (
                    <div className="mt-1.5 h-1 overflow-hidden rounded-full bg-[color:var(--surface-2)]">
                      <motion.div animate={{ width: `${pct}%` }} className="h-full bg-primary" />
                    </div>
                  )}
                </div>
                {q.status === "downloading" ? (
                  <Loader2 className="h-4 w-4 animate-spin text-primary" />
                ) : (
                  <button onClick={() => cancelDownload(q.id)} aria-label="Remove from queue" className="grid h-8 w-8 place-items-center rounded-full hover:bg-[color:var(--surface-2)]">
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
            );
          })}
        </section>
      )}

      {dl.loaded && groups.length === 0 && dl.queue.length === 0 ? (
        <KuroTip mood="sleepy">Nothing downloaded yet. Tap the download arrow next to any chapter to read it offline.</KuroTip>
      ) : (
        <motion.div variants={staggerContainer(0.04)} initial="hidden" animate="show" className="space-y-3">
          {groups.map((list) => {
            const s = list[0]!.series;
            const bytes = list.reduce((n, d) => n + d.bytes, 0);
            return (
              <motion.section key={list[0]!.seriesId} variants={revealUp} className="kt-card rounded-2xl p-3">
                <div className="flex items-center gap-3">
                  <Link to="/manga/$sourceId/$key" params={{ sourceId: s.sourceId, key: encodeKey(s.key) }}
                    className="relative h-14 w-10 shrink-0 overflow-hidden rounded-lg bg-[color:var(--surface-2)]">
                    <Cover sourceId={s.sourceId} url={s.cover} alt="" />
                  </Link>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-bold">{s.title}</p>
                    <p className="text-[11px] text-muted-foreground">{plural(list.length, "chapter")} · {formatBytes(bytes)}</p>
                  </div>
                  <button onClick={() => void deleteSeriesDownloads(list[0]!.seriesId)} aria-label="Delete all downloads for this series"
                    className="grid h-8 w-8 place-items-center rounded-full text-muted-foreground hover:bg-[color:var(--surface-2)] hover:text-foreground">
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                <ul className="mt-2 flex flex-wrap gap-1.5">
                  {list.map((d) => (
                    <li key={d.chapter.key} className="flex items-center overflow-hidden rounded-full bg-[color:var(--surface-2)] ring-1 ring-border">
                      <Link to="/read/$sourceId/$mangaKey/$chapterKey"
                        params={{ sourceId: s.sourceId, mangaKey: encodeKey(s.key), chapterKey: encodeKey(d.chapter.key) }}
                        className="px-2.5 py-1 text-[11px] font-bold">
                        {d.label}
                      </Link>
                      <button onClick={() => void deleteDownload(d.seriesId, d.chapter.key)} aria-label={`Delete ${d.label}`} className="pr-2 text-muted-foreground hover:text-foreground">
                        <X className="h-3 w-3" />
                      </button>
                    </li>
                  ))}
                </ul>
              </motion.section>
            );
          })}
        </motion.div>
      )}
    </div>
  );
}

function plural(n: number, word: string): string {
  return `${n} ${word}${n === 1 ? "" : "s"}`;
}
