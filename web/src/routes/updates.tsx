import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { BellRing, Loader2, RefreshCcw, Trash2, AlertTriangle } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Cover } from "@/components/Cover";
import { KuroTip } from "@/components/Kuro";
import { encodeKey } from "@/engine/ids";
import { useEngineReady } from "@/engine/hooks";
import { getReadChapters } from "@/lib/local-library";
import { checkLibraryUpdates, clearUpdates, lastUpdateRun, useUpdates, type UpdateEntry } from "@/lib/updates";
import { revealUp, spring, staggerContainer } from "@/lib/motion";

export const Route = createFileRoute("/updates")({
  head: () => ({
    meta: [
      { title: "Updates — KuroToon" },
      { name: "description", content: "New chapters from the series in your library, checked on your device." },
      { property: "og:title", content: "Updates — KuroToon" },
      { property: "og:description", content: "New chapters from your library." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Updates,
});

function dayLabel(ts: number) {
  const d = new Date(ts);
  const today = new Date();
  const y = new Date(Date.now() - 86_400_000);
  if (d.toDateString() === today.toDateString()) return "Today";
  if (d.toDateString() === y.toDateString()) return "Yesterday";
  return d.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
}

function ago(ts: number | null) {
  if (!ts) return "never";
  const m = Math.round((Date.now() - ts) / 60_000);
  if (m < 1) return "just now";
  if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60);
  return h < 24 ? `${h} h ago` : `${Math.round(h / 24)} d ago`;
}

function Updates() {
  const ready = useEngineReady();
  const { feed, status } = useUpdates();
  const [last, setLast] = useState<number | null>(null);
  const [, force] = useState(0);

  useEffect(() => {
    setLast(lastUpdateRun());
    const on = () => force((n) => n + 1);
    window.addEventListener("kt-local-change", on);
    return () => window.removeEventListener("kt-local-change", on);
  }, [status.lastRunAt]);

  const groups = useMemo(() => {
    const out: Array<{ day: string; items: UpdateEntry[] }> = [];
    for (const e of feed) {
      const day = dayLabel(e.foundAt);
      const g = out[out.length - 1];
      if (g && g.day === day) g.items.push(e);
      else out.push({ day, items: [e] });
    }
    return out;
  }, [feed]);

  const run = async () => {
    const n = await checkLibraryUpdates();
    toast.success(n ? `${n} new chapter${n === 1 ? "" : "s"}` : "No new chapters", { description: "Your library is up to date." });
  };

  const pct = status.total ? Math.round((status.done / status.total) * 100) : 0;

  return (
    <div className="space-y-5">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative flex items-center justify-between gap-3">
          <div className="min-w-0">
            <h1 className="flex items-center gap-2 text-xl font-black tracking-tight">
              <BellRing className="h-5 w-5 text-primary" /> Updates
            </h1>
            <p className="mt-1 text-xs text-muted-foreground">Last checked {ago(last)}</p>
          </div>
          <motion.button
            whileTap={{ scale: 0.94 }}
            transition={spring.tap}
            disabled={!ready || status.running}
            onClick={run}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
          >
            {status.running ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <RefreshCcw className="h-3.5 w-3.5" />}
            {status.running ? `${status.done}/${status.total}` : "Check now"}
          </motion.button>
        </div>
        {status.running && (
          <div className="relative mt-3 h-1.5 overflow-hidden rounded-full bg-[color:var(--surface-2)]">
            <motion.div animate={{ width: `${pct}%` }} className="h-full bg-primary" />
          </div>
        )}
      </header>

      {status.errors.length > 0 && !status.running && (
        <div className="kt-card rounded-2xl p-3 text-xs">
          <p className="flex items-center gap-1.5 font-bold text-amber-400">
            <AlertTriangle className="h-3.5 w-3.5" /> {status.errors.length} series could not be checked
          </p>
          <ul className="mt-1 space-y-0.5 text-muted-foreground">
            {status.errors.slice(0, 5).map((e) => (
              <li key={e.seriesId} className="truncate">{e.title}: {e.error}</li>
            ))}
          </ul>
        </div>
      )}

      {feed.length === 0 ? (
        <KuroTip mood="sleepy">
          No new chapters yet. Add series to your library, then tap Check now — KuroToon compares every chapter list with what
          it saw last time.
        </KuroTip>
      ) : (
        <>
          {groups.map((g) => (
            <section key={g.day}>
              <h2 className="mb-2 text-xs font-bold uppercase tracking-widest text-muted-foreground">{g.day}</h2>
              <motion.div variants={staggerContainer(0.03)} initial="hidden" animate="show" className="space-y-2">
                {g.items.map((e) => {
                  const read = getReadChapters(e.seriesId).includes(e.chapterKey);
                  return (
                    <motion.div key={`${e.seriesId}|${e.chapterKey}`} variants={revealUp}>
                      <Link
                        to="/read/$sourceId/$mangaKey/$chapterKey"
                        params={{ sourceId: e.series.sourceId, mangaKey: encodeKey(e.series.key), chapterKey: encodeKey(e.chapterKey) }}
                        className={`kt-card flex items-center gap-3 rounded-2xl p-2.5 ${read ? "opacity-60" : ""}`}
                      >
                        <div className="relative h-14 w-10 shrink-0 overflow-hidden rounded-lg bg-[color:var(--surface-2)]">
                          <Cover sourceId={e.series.sourceId} url={e.series.cover} alt="" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-bold">{e.series.title}</p>
                          <p className="truncate text-[11px] text-muted-foreground">
                            {e.label}
                            {e.title && e.label !== e.title ? ` · ${e.title}` : ""}
                          </p>
                        </div>
                        {!read && <span className="h-2 w-2 shrink-0 rounded-full bg-primary shadow-[var(--shadow-glow)]" aria-label="Unread" />}
                      </Link>
                    </motion.div>
                  );
                })}
              </motion.div>
            </section>
          ))}
          <button onClick={clearUpdates} className="mx-auto flex items-center gap-1.5 text-[11px] text-muted-foreground hover:text-foreground">
            <Trash2 className="h-3 w-3" /> Clear updates
          </button>
        </>
      )}
    </div>
  );
}
