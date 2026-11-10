import { useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import { useNavigate } from "@tanstack/react-router";
import { ArrowRight, Loader2, Repeat, Search } from "lucide-react";
import { toast } from "sonner";
import { Cover } from "./Cover";
import { Sheet } from "./LibraryShelf";
import { spring } from "@/lib/motion";
import { universalSearch, type SourceResult } from "@/engine/search";
import { chapterList } from "@/engine/runtime";
import { encodeKey } from "@/engine/ids";
import type { ChapterRow, SeriesCard } from "@/engine/types";
import { migrateSeries, titleScore, type MigrateOptions } from "@/lib/migrate";

type Hit = { item: SeriesCard; sourceName: string; score: number };

/** Mihon-style migration: search every other source, pick a match, move. */
export function MigrateSheet({
  open,
  onClose,
  from,
  fromChapters,
}: {
  open: boolean;
  onClose: () => void;
  from: SeriesCard;
  fromChapters: ChapterRow[];
}) {
  const navigate = useNavigate();
  const [query, setQuery] = useState(from.title);
  const [submitted, setSubmitted] = useState(from.title);
  const [results, setResults] = useState<SourceResult[]>([]);
  const [pick, setPick] = useState<Hit | null>(null);
  const [opts, setOpts] = useState<MigrateOptions>({
    chapters: true,
    categories: true,
    removeOld: true,
  });
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open || !submitted.trim()) return;
    const ctl = new AbortController();
    setResults([]);
    void universalSearch(
      submitted.trim(),
      (r) => setResults(r.filter((x) => x.sourceId !== from.sourceId)),
      ctl.signal,
    );
    return () => ctl.abort();
  }, [open, submitted, from.sourceId]);

  const hits = useMemo<Hit[]>(() => {
    const out: Hit[] = [];
    for (const r of results)
      for (const item of r.items.slice(0, 4))
        out.push({ item, sourceName: r.sourceName, score: titleScore(from.title, item.title) });
    return out.sort((a, b) => b.score - a.score);
  }, [results, from.title]);
  const pending = results.filter((r) => r.status === "pending").length;

  const run = async () => {
    if (!pick) return;
    setBusy(true);
    try {
      const to = pick.item;
      const toChapters = await chapterList(to.sourceId, to.key);
      const r = migrateSeries(from, to, fromChapters, toChapters, opts);
      toast.success(
        `${opts.removeOld ? "Migrated" : "Copied"} to ${pick.sourceName}${opts.chapters ? ` · ${r.readCarried} read chapter${r.readCarried === 1 ? "" : "s"} carried over` : ""}`,
      );
      onClose();
      void navigate({
        to: "/manga/$sourceId/$key",
        params: { sourceId: to.sourceId, key: encodeKey(to.key) },
      });
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Migration failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Sheet open={open} onClose={onClose} title={pick ? "Migrate" : "Migrate to another source"}>
      {pick ? (
        <div className="space-y-4">
          <div className="flex items-center gap-2">
            <Mini card={from} label="Current" />
            <ArrowRight className="h-4 w-4 shrink-0 text-primary" />
            <Mini card={pick.item} label={pick.sourceName} />
          </div>
          <div className="space-y-1">
            <Toggle
              label="Read chapters & position"
              on={opts.chapters}
              set={(v) => setOpts({ ...opts, chapters: v })}
            />
            <Toggle
              label="Categories"
              on={opts.categories}
              set={(v) => setOpts({ ...opts, categories: v })}
            />
            <Toggle
              label="Remove old entry from library"
              on={opts.removeOld}
              set={(v) => setOpts({ ...opts, removeOld: v })}
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setPick(null)}
              className="flex-1 rounded-full bg-[color:var(--surface-2)] px-4 py-2.5 text-sm font-semibold ring-1 ring-border"
            >
              Back
            </button>
            <motion.button
              whileTap={{ scale: 0.96 }}
              transition={spring.tap}
              disabled={busy}
              onClick={run}
              className="inline-flex flex-1 items-center justify-center gap-2 rounded-full bg-[image:var(--gradient-accent)] px-4 py-2.5 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
            >
              {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Repeat className="h-4 w-4" />}
              {opts.removeOld ? "Migrate" : "Copy"}
            </motion.button>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              setSubmitted(query);
            }}
            className="flex items-center gap-2 rounded-xl bg-[color:var(--surface-2)] px-3 ring-1 ring-border focus-within:ring-primary/60"
          >
            <Search className="h-4 w-4 text-muted-foreground" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              enterKeyHint="search"
              aria-label="Search other sources"
              className="w-full bg-transparent py-2.5 text-sm focus:outline-none"
            />
          </form>
          <p className="text-[11px] text-muted-foreground">
            {results.length === 0
              ? "No other sources installed. Install another source in Extensions first."
              : pending
                ? `Searching ${pending} of ${results.length} sources…`
                : `${hits.length} matches across ${results.length} sources`}
          </p>
          <ul className="space-y-1.5">
            {hits.slice(0, 30).map((h) => (
              <li key={`${h.item.sourceId}:${h.item.key}`}>
                <button
                  onClick={() => setPick(h)}
                  className="flex w-full items-center gap-3 rounded-xl p-1.5 text-left hover:bg-[color:var(--surface-2)]"
                >
                  <div className="h-14 w-10 shrink-0 overflow-hidden rounded-lg bg-[color:var(--surface-2)]">
                    <Cover sourceId={h.item.sourceId} url={h.item.cover} alt="" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold">{h.item.title}</p>
                    <p className="truncate text-[11px] text-muted-foreground">{h.sourceName}</p>
                  </div>
                  {h.score === 1 && (
                    <span className="rounded-full bg-primary/15 px-2 py-0.5 text-[10px] font-bold text-primary">
                      Exact
                    </span>
                  )}
                </button>
              </li>
            ))}
          </ul>
          {pending > 0 && (
            <Loader2 className="mx-auto h-4 w-4 animate-spin text-muted-foreground" />
          )}
        </div>
      )}
    </Sheet>
  );
}

function Mini({ card, label }: { card: SeriesCard; label: string }) {
  return (
    <div className="flex min-w-0 flex-1 items-center gap-2 rounded-xl bg-[color:var(--surface-2)] p-2 ring-1 ring-border">
      <div className="h-12 w-9 shrink-0 overflow-hidden rounded-md">
        <Cover sourceId={card.sourceId} url={card.cover} alt="" />
      </div>
      <div className="min-w-0">
        <p className="truncate text-xs font-bold">{card.title}</p>
        <p className="truncate text-[10px] text-muted-foreground">{label}</p>
      </div>
    </div>
  );
}

function Toggle({ label, on, set }: { label: string; on: boolean; set: (v: boolean) => void }) {
  return (
    <label className="flex cursor-pointer items-center justify-between rounded-xl px-1 py-2 text-sm">
      {label}
      <input
        type="checkbox"
        checked={on}
        onChange={(e) => set(e.target.checked)}
        className="h-4 w-4 accent-[color:var(--primary)]"
      />
    </label>
  );
}
