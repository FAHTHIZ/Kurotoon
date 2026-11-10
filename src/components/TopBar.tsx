import { Link, useNavigate } from "@tanstack/react-router";
import { Search, User as UserIcon, Loader2, TrendingUp, Clock, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { spring, ease } from "@/lib/motion";
import { getRecentSearches, pushRecentSearch, clearRecentSearches, searchLocal, rememberSeries } from "@/lib/local-library";
import { encodeKey } from "@/engine/ids";
import type { SeriesCard } from "@/engine/types";
import { KuroBalance } from "./KuroBalance";

const TRENDING = ["Solo Leveling", "One Piece", "Jujutsu Kaisen", "Chainsaw Man", "Omniscient Reader", "Berserk"];

export function TopBar() {
  const [q, setQ] = useState("");
  const [debounced, setDebounced] = useState("");
  const [open, setOpen] = useState(false);
  const [focused, setFocused] = useState(false);
  const [recent, setRecent] = useState<string[]>([]);
  const navigate = useNavigate();
  const boxRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(q.trim()), 220);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => { setRecent(getRecentSearches()); }, []);

  useEffect(() => {
    const onDown = (e: MouseEvent) => {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, []);

  // Instant typeahead over the on-device library; Enter runs Universal Search.
  const hits = debounced.length >= 2 ? searchLocal(debounced) : [];

  const go = (s: SeriesCard) => {
    setOpen(false);
    rememberSeries(s);
    setQ("");
    navigate({ to: "/manga/$sourceId/$key", params: { sourceId: s.sourceId, key: encodeKey(s.key) } });
  };

  const runSearch = (term: string) => {
    if (!term.trim()) return;
    pushRecentSearch(term);
    setRecent(getRecentSearches());
    setOpen(false);
    setQ(term);
    navigate({ to: "/search", search: { q: term.trim() } });
  };

  const showTypeahead = open && debounced.length >= 2;
  const showDiscovery = open && debounced.length < 2;

  return (
    <header className="sticky top-0 z-30 border-b border-white/[0.06] bg-background/70 backdrop-blur-2xl">
      <div className="pointer-events-none absolute inset-x-0 -bottom-16 h-16 bg-[image:var(--gradient-ambient)] opacity-60" />
      <div className="mx-auto flex max-w-6xl items-center gap-3 px-4 py-3">
        <Link to="/" className="flex items-center gap-2">
          <motion.div
            whileHover={{ rotate: -6, scale: 1.06 }}
            whileTap={{ scale: 0.92 }}
            transition={spring.tap}
            className="grid h-8 w-8 place-items-center rounded-lg bg-[image:var(--gradient-accent)] font-black text-primary-foreground shadow-[var(--shadow-glow)]"
          >
            K
          </motion.div>
          <span className="text-base font-bold tracking-tight">KuroToon</span>
        </Link>

        <div ref={boxRef} className="relative ml-auto flex-1 max-w-md">
          <motion.form
            onSubmit={(e) => { e.preventDefault(); runSearch(q); }}
            animate={focused ? { scale: 1.015 } : { scale: 1 }}
            transition={spring.soft}
            className={`flex items-center gap-2 rounded-full bg-[color:var(--surface)] px-3 py-1.5 ring-1 transition-shadow duration-300 ${
              focused ? "ring-primary/60 shadow-[var(--shadow-glow)]" : "ring-border"
            }`}
          >
            <motion.span animate={focused ? { scale: 1.1, color: "var(--primary)" } : { scale: 1 }} transition={spring.soft}>
              <Search className="h-4 w-4 text-muted-foreground" />
            </motion.span>
            <input
              value={q}
              onChange={(e) => { setQ(e.target.value); setOpen(true); }}
              onFocus={() => { setOpen(true); setFocused(true); }}
              onBlur={() => setFocused(false)}
              placeholder="Search every source…"
              enterKeyHint="search"
              className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            />
            <AnimatePresence>
              {q && (
                <motion.button
                  type="button"
                  initial={{ opacity: 0, scale: 0.6 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.6 }}
                  transition={spring.tap}
                  onClick={() => { setQ(""); setDebounced(""); }}
                  className="grid h-5 w-5 place-items-center rounded-full bg-white/10 text-muted-foreground"
                  aria-label="Clear search"
                >
                  <X className="h-3 w-3" />
                </motion.button>
              )}
            </AnimatePresence>
            
          </motion.form>

          <AnimatePresence>
            {(showTypeahead || showDiscovery) && (
              <motion.div
                initial={{ opacity: 0, y: -6, scale: 0.98 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -6, scale: 0.98 }}
                transition={{ duration: 0.2, ease: ease.out }}
                className="absolute left-0 right-0 top-full z-40 mt-2 origin-top overflow-hidden rounded-2xl border border-white/[0.08] bg-background/95 shadow-[var(--shadow-depth-3)] backdrop-blur-2xl"
              >
                {showTypeahead ? (
                  <>
                      {hits.map((h, i) => (
                        <motion.button
                          key={`${h.sourceId}:${h.key}`}
                          initial={{ opacity: 0, x: -8 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: i * 0.03, duration: 0.2, ease: ease.out }}
                          whileTap={{ scale: 0.98 }}
                          onClick={() => go(h)}
                          className="flex w-full items-center gap-3 px-3 py-2 text-left hover:bg-[color:var(--surface)]"
                        >
                          {h.cover ? (
                            <img src={h.cover} alt="" loading="lazy" referrerPolicy="no-referrer" className="h-12 w-8 rounded object-cover shadow-[var(--shadow-depth-1)]" />
                          ) : (
                            <div className="h-12 w-8 rounded bg-[color:var(--surface-2)]" />
                          )}
                          <span className="min-w-0 flex-1">
                            <span className="block truncate text-sm font-semibold">{h.title}</span>
                            <span className="block truncate text-[11px] text-muted-foreground">In your library · {h.sourceId}</span>
                          </span>
                        </motion.button>
                      ))}
                      <button
                        onClick={() => runSearch(debounced)}
                        className="w-full border-t border-border px-3 py-2 text-xs font-semibold text-primary hover:bg-[color:var(--surface)]"
                      >
                        Search every source for “{debounced}”
                      </button>
                    </>
                ) : (
                  <div className="p-3">
                    {recent.length > 0 && (
                      <div className="mb-3">
                        <div className="mb-1.5 flex items-center justify-between">
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                            <Clock className="h-3 w-3" /> Recent
                          </span>
                          <button
                            onClick={() => { clearRecentSearches(); setRecent([]); }}
                            className="text-[10px] font-semibold text-muted-foreground hover:text-primary"
                          >
                            Clear
                          </button>
                        </div>
                        <div className="flex flex-wrap gap-1.5">
                          {recent.map((r) => (
                            <motion.button
                              key={r}
                              whileTap={{ scale: 0.94 }}
                              transition={spring.tap}
                              onClick={() => runSearch(r)}
                              className="max-w-full truncate rounded-full bg-[color:var(--surface)] px-2.5 py-1 text-[11px] ring-1 ring-border hover:ring-primary/50"
                            >
                              {r}
                            </motion.button>
                          ))}
                        </div>
                      </div>
                    )}
                    <div className="mb-1.5 inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                      <TrendingUp className="h-3 w-3 text-[color:var(--gold)]" /> Trending now
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {TRENDING.map((t, i) => (
                        <motion.button
                          key={t}
                          initial={{ opacity: 0, y: 4 }}
                          animate={{ opacity: 1, y: 0 }}
                          transition={{ delay: 0.02 * i, duration: 0.2 }}
                          whileTap={{ scale: 0.94 }}
                          onClick={() => runSearch(t)}
                          className="rounded-full bg-[color:var(--surface)] px-2.5 py-1 text-[11px] ring-1 ring-border hover:ring-primary/50"
                        >
                          {t}
                        </motion.button>
                      ))}
                    </div>
                  </div>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <KuroBalance />
        <Link to="/profile" className="grid h-9 w-9 place-items-center rounded-full bg-[color:var(--surface)] shadow-[var(--shadow-depth-1)] hover:bg-[color:var(--surface-2)]">
          <UserIcon className="h-4 w-4" />
        </Link>
      </div>
    </header>
  );
}
