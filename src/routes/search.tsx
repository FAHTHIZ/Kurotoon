import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { useEffect, useState } from "react";
import { AlertTriangle, ChevronRight, Globe2, Loader2, Search } from "lucide-react";
import { z } from "zod";
import { MangaCard } from "@/components/MangaCard";
import { KuroTip } from "@/components/Kuro";
import { revealUp } from "@/lib/motion";
import { pushRecentSearch } from "@/lib/local-library";
import { useEngineReady } from "@/engine/hooks";
import { activeSources } from "@/engine/store";
import { universalSearch, type SourceResult } from "@/engine/search";

export const Route = createFileRoute("/search")({
  validateSearch: z.object({ q: z.string().optional().catch(undefined) }),
  head: () => ({
    meta: [
      { title: "Universal Search — KuroToon" },
      { name: "description", content: "Search every installed source at once." },
    ],
  }),
  component: SearchPage,
});

function SearchPage() {
  const { q } = Route.useSearch();
  const navigate = Route.useNavigate();
  const engine = useEngineReady();
  const [input, setInput] = useState(q ?? "");
  const [results, setResults] = useState<SourceResult[]>([]);
  const [running, setRunning] = useState(false);
  const sourceCount = activeSources(engine).length;

  useEffect(() => setInput(q ?? ""), [q]);

  useEffect(() => {
    const term = q?.trim();
    if (!engine.ready || !term) {
      setResults([]);
      return;
    }
    pushRecentSearch(term);
    const ctrl = new AbortController();
    setRunning(true);
    universalSearch(term, setResults, ctrl.signal).finally(() => {
      if (!ctrl.signal.aborted) setRunning(false);
    });
    return () => ctrl.abort();
    // re-run when the active source set changes
  }, [q, engine.ready, sourceCount]);

  const done = results.filter((r) => r.status !== "pending").length;
  const withHits = results.filter((r) => r.items.length > 0);
  const empty = results.filter((r) => r.status === "done" && r.items.length === 0);
  const failed = results.filter((r) => r.status === "error");
  const pending = results.filter((r) => r.status === "pending");

  return (
    <div className="space-y-5">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          navigate({ search: { q: input.trim() || undefined }, replace: true });
        }}
        className="flex items-center gap-2 rounded-full bg-[color:var(--surface)] px-3 py-2.5 ring-1 ring-border focus-within:ring-primary/60"
      >
        <Globe2 className="h-4 w-4 text-primary" />
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          enterKeyHint="search"
          autoFocus={!q}
          placeholder="Search every source…"
          className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
        {running ? <Loader2 className="h-4 w-4 animate-spin text-primary" /> : <Search className="h-4 w-4 text-muted-foreground" />}
      </form>

      {engine.ready && sourceCount === 0 ? (
        <div className="space-y-3">
          <KuroTip mood="idle">Universal Search needs at least one installed source.</KuroTip>
          <Link to="/extensions" className="inline-block rounded-full bg-primary px-4 py-2 text-xs font-bold text-primary-foreground">
            Open Extensions
          </Link>
        </div>
      ) : !q ? (
        <KuroTip mood="reading">Type a title — every installed source is searched in parallel.</KuroTip>
      ) : (
        <>
          {results.length > 0 && (
            <div className="h-1 overflow-hidden rounded-full bg-white/10">
              <motion.div
                className="h-full rounded-full bg-[image:var(--gradient-accent)]"
                animate={{ width: `${(done / results.length) * 100}%` }}
                transition={{ duration: 0.3 }}
              />
            </div>
          )}

          {withHits.map((r) => (
            <motion.section key={r.sourceId} variants={revealUp} initial="hidden" animate="show">
              <Link
                to="/source/$sourceId"
                params={{ sourceId: r.sourceId }}
                search={{ q }}
                className="mb-2 flex items-center justify-between"
              >
                <h2 className="text-sm font-bold">{r.sourceName}</h2>
                <span className="inline-flex items-center gap-0.5 text-[11px] text-muted-foreground">
                  {r.items.length}
                  {r.ms != null && ` · ${(r.ms / 1000).toFixed(1)}s`}
                  <ChevronRight className="h-3.5 w-3.5" />
                </span>
              </Link>
              <div className="no-scrollbar -mx-3 flex gap-3 overflow-x-auto px-3 pb-1">
                {r.items.slice(0, 20).map((s, i) => (
                  <div key={s.key} className="w-28 shrink-0">
                    <MangaCard s={s} index={i} />
                  </div>
                ))}
              </div>
            </motion.section>
          ))}

          {pending.length > 0 && (
            <p className="flex items-center gap-2 text-[11px] text-muted-foreground">
              <Loader2 className="h-3.5 w-3.5 animate-spin" /> Searching {pending.map((p) => p.sourceName).join(", ")}
            </p>
          )}

          {!running && withHits.length === 0 && <KuroTip mood="sleepy">No source found “{q}”.</KuroTip>}

          {(empty.length > 0 || failed.length > 0) && (
            <div className="space-y-1 text-[11px] text-muted-foreground">
              {empty.length > 0 && <p>No results: {empty.map((r) => r.sourceName).join(", ")}</p>}
              {failed.map((r) => (
                <p key={r.sourceId} className="flex items-start gap-1">
                  <AlertTriangle className="mt-px h-3 w-3 shrink-0 text-amber-400" /> {r.sourceName}: {r.error}
                </p>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
