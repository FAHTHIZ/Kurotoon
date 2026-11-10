import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { useState } from "react";
import { Boxes, ChevronRight, Globe2, Search } from "lucide-react";
import { KuroTip } from "@/components/Kuro";
import { SourceIcon } from "@/components/Cover";
import { spring, revealUp, staggerContainer } from "@/lib/motion";
import { useEngineReady } from "@/engine/hooks";
import { activeSources, healthLabel, updatesAvailable } from "@/engine/store";

export const Route = createFileRoute("/browse")({
  head: () => ({
    meta: [
      { title: "Browse sources — KuroToon" },
      { name: "description", content: "Browse your installed sources, or search all of them at once with Universal Search." },
      { property: "og:title", content: "Browse sources — KuroToon" },
      { property: "og:description", content: "Your installed sources and Universal Search." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Browse,
});

const DOT: Record<ReturnType<typeof healthLabel>, string> = {
  unknown: "bg-white/20",
  healthy: "bg-emerald-400",
  degraded: "bg-amber-400",
  failing: "bg-destructive",
};

function Browse() {
  const engine = useEngineReady();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const sources = activeSources(engine);
  const updates = updatesAvailable(engine).length;

  const byLang = new Map<string, typeof sources>();
  for (const s of sources) {
    const l = s.languages.length === 1 ? s.languages[0]! : "multi";
    byLang.set(l, [...(byLang.get(l) ?? []), s]);
  }
  const groups = [...byLang.entries()].sort(([a], [b]) => (a === "multi" ? -1 : b === "multi" ? 1 : a.localeCompare(b)));

  return (
    <div className="space-y-5">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (q.trim()) navigate({ to: "/search", search: { q: q.trim() } });
        }}
        className="kt-card relative overflow-hidden rounded-3xl p-4"
      >
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <p className="relative flex items-center gap-2 text-sm font-black">
          <Globe2 className="h-4 w-4 text-primary" /> Universal Search
        </p>
        <p className="relative mt-0.5 text-[11px] text-muted-foreground">One search across every installed source.</p>
        <label className="relative mt-3 flex items-center gap-2 rounded-full bg-[color:var(--surface-2)] px-3 py-2.5 ring-1 ring-border focus-within:ring-primary/60">
          <Search className="h-4 w-4 text-muted-foreground" />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            enterKeyHint="search"
            placeholder="Search every source…"
            className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
          />
        </label>
      </form>

      <div className="flex items-center justify-between">
        <h1 className="text-lg font-bold tracking-tight">Sources</h1>
        <Link to="/extensions" className="inline-flex items-center gap-1 text-xs font-semibold text-muted-foreground hover:text-primary">
          <Boxes className="h-3.5 w-3.5" /> Extensions
          {updates > 0 && <span className="rounded-full bg-primary px-1.5 text-[10px] font-bold text-primary-foreground">{updates}</span>}
        </Link>
      </div>

      {!engine.ready ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="kt-shimmer h-14 rounded-2xl" />
          ))}
        </div>
      ) : sources.length === 0 ? (
        <div className="space-y-3">
          <KuroTip mood="idle">
            {engine.installed.length
              ? "Your installed sources are disabled or hidden by content filters."
              : "No sources yet. Add an extension repository and install a source to start reading."}
          </KuroTip>
          <Link
            to="/extensions"
            className="inline-block rounded-full bg-[image:var(--gradient-accent)] px-5 py-2 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
          >
            Open Extensions
          </Link>
        </div>
      ) : (
        groups.map(([lang, list]) => (
          <section key={lang}>
            <h2 className="mb-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">{lang === "multi" ? "Multi-language" : lang}</h2>
            <motion.ul variants={staggerContainer(0.03)} initial="hidden" animate="show" className="space-y-2">
              {list.map((s) => (
                <motion.li key={s.id} variants={revealUp}>
                  <Link to="/source/$sourceId" params={{ sourceId: s.id }}>
                    <motion.span
                      whileTap={{ scale: 0.98 }}
                      transition={spring.tap}
                      className="kt-card flex items-center gap-3 rounded-2xl p-3"
                    >
                      <SourceIcon name={s.name} url={s.iconUrl} />
                      <span className="min-w-0 flex-1">
                        <span className="flex items-center gap-1.5 truncate text-sm font-bold">
                          {s.name}
                          <span className={`h-1.5 w-1.5 rounded-full ${DOT[healthLabel(engine.health[s.id])]}`} />
                        </span>
                        <span className="block truncate text-[11px] text-muted-foreground">{s.baseUrl ? new URL(s.baseUrl).hostname : s.id}</span>
                      </span>
                      <ChevronRight className="h-4 w-4 text-muted-foreground" />
                    </motion.span>
                  </Link>
                </motion.li>
              ))}
            </motion.ul>
          </section>
        ))
      )}
    </div>
  );
}
