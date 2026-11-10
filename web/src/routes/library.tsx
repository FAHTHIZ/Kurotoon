import { createFileRoute, Link } from "@tanstack/react-router";
import { MangaCard } from "@/components/MangaCard";
import { LibraryShelf } from "@/components/LibraryShelf";
import { Bookmark, History, BookOpen, Heart, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  getBookmarks,
  getContinueReading,
  getFavourites,
  getHistory,
  clearHistory,
  type ProgressEntry,
  type HistoryEntry,
  idOf,
} from "@/lib/local-library";
import type { SeriesCard } from "@/engine/types";
import { encodeKey } from "@/engine/ids";
import { Cover } from "@/components/Cover";
import { KuroTip } from "@/components/Kuro";
import { revealUp, staggerContainer, spring } from "@/lib/motion";

export const Route = createFileRoute("/library")({
  head: () => ({
    meta: [
      { title: "My library — KuroToon" },
      { name: "description", content: "Your reading hub: continue reading, bookmarks, favourites and full history — saved on this device." },
      { property: "og:title", content: "My library — KuroToon" },
      { property: "og:description", content: "Continue reading, bookmarks, favourites and history in one place." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Library,
});

type Tab = "bookmarks" | "reading" | "favourites" | "history";

const TABS: Array<{ id: Tab; label: string; icon: typeof Bookmark }> = [
  { id: "bookmarks", label: "Library", icon: Bookmark },
  { id: "reading", label: "Reading", icon: BookOpen },
  { id: "favourites", label: "Favourites", icon: Heart },
  { id: "history", label: "History", icon: History },
];

type State = {
  reading: Array<{ series: SeriesCard; progress: ProgressEntry }>;
  bookmarks: SeriesCard[];
  favourites: SeriesCard[];
  history: Array<HistoryEntry & { series: SeriesCard | null }>;
};

const EMPTY: State = { reading: [], bookmarks: [], favourites: [], history: [] };

function Library() {
  const [tab, setTab] = useState<Tab>("bookmarks");
  const [state, setState] = useState<State>(EMPTY);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const sync = () => {
      setState({
        reading: getContinueReading(),
        bookmarks: getBookmarks(),
        favourites: getFavourites(),
        history: getHistory(),
      });
      setReady(true);
    };
    sync();
    window.addEventListener("kt-local-change", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("kt-local-change", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  const counts: Record<Tab, number> = {
    reading: state.reading.length,
    bookmarks: state.bookmarks.length,
    favourites: state.favourites.length,
    history: state.history.length,
  };

  return (
    <div className="space-y-5">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <h1 className="relative text-xl font-black tracking-tight">My Library</h1>
        <p className="relative mt-1 text-xs text-muted-foreground">
          Everything you read is kept on this device — instant, private, and always yours.
        </p>
      </header>

      <div className="flex gap-1.5 overflow-x-auto rounded-full bg-[color:var(--surface)] p-1 ring-1 ring-border">
        {TABS.map(({ id, label, icon: Icon }) => (
          <motion.button
            key={id}
            whileTap={{ scale: 0.95 }}
            transition={spring.tap}
            onClick={() => setTab(id)}
            className={`inline-flex flex-1 items-center justify-center gap-1.5 whitespace-nowrap rounded-full px-3 py-2 text-[11px] font-bold transition ${
              tab === id ? "bg-primary text-primary-foreground shadow-[var(--shadow-glow)]" : "text-muted-foreground"
            }`}
          >
            <Icon className="h-3.5 w-3.5" /> {label}
            {counts[id] > 0 && <span className="tabular-nums opacity-70">{counts[id]}</span>}
          </motion.button>
        ))}
      </div>

      {!ready ? (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="kt-shimmer aspect-[2/3] rounded-xl ring-1 ring-white/[0.06]" />
          ))}
        </div>
      ) : tab === "reading" ? (
        state.reading.length === 0 ? (
          <Empty text="Nothing in progress yet. Open any chapter and Kuro will keep your place." />
        ) : (
          <motion.div variants={staggerContainer(0.04)} initial="hidden" animate="show" className="space-y-2.5">
            {state.reading.map(({ series, progress }) => (
              <motion.div key={idOf(series)} variants={revealUp}>
                <Link
                  to="/read/$sourceId/$mangaKey/$chapterKey"
                  params={{ sourceId: series.sourceId, mangaKey: encodeKey(series.key), chapterKey: encodeKey(progress.chapterKey) }}
                  className="kt-card flex items-center gap-3 rounded-2xl p-3"
                >
                  <div className="relative h-16 w-11 shrink-0 overflow-hidden rounded-lg bg-[color:var(--surface-2)]">
                    <Cover sourceId={series.sourceId} url={series.cover} alt="" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-bold">{series.title}</p>
                    <p className="mt-0.5 text-[11px] text-muted-foreground">
                      {progress.chapterLabel} · page {progress.page}/{progress.total}
                    </p>
                  </div>
                  <span className="rounded-full bg-primary/15 px-3 py-1 text-[11px] font-bold text-primary ring-1 ring-primary/25">
                    Resume
                  </span>
                </Link>
              </motion.div>
            ))}
          </motion.div>
        )
      ) : tab === "bookmarks" ? (
        state.bookmarks.length === 0 ? (
          <Empty text="Your library is empty. Tap Add to library on any series to save it here." />
        ) : (
          <LibraryShelf items={state.bookmarks} />
        )
      ) : tab === "favourites" ? (
        state.favourites.length === 0 ? (
          <Empty text="No favourites yet. Heart a series to pin your all-time picks." />
        ) : (
          <Grid items={state.favourites} />
        )
      ) : state.history.length === 0 ? (
        <Empty text="Your reading history is empty — go find something great." />
      ) : (
        <div className="space-y-2.5">
          <button
            onClick={clearHistory}
            className="inline-flex items-center gap-1.5 rounded-full bg-[color:var(--surface-2)] px-3 py-1.5 text-[11px] font-bold text-muted-foreground ring-1 ring-border"
          >
            <Trash2 className="h-3.5 w-3.5" /> Clear history
          </button>
          {state.history.map((h) => (
            <Link
              key={`${h.seriesId}-${h.chapterKey}-${h.at}`}
              to="/read/$sourceId/$mangaKey/$chapterKey"
              params={{
                sourceId: h.seriesId.split("::")[0]!,
                mangaKey: encodeKey(h.seriesId.slice(h.seriesId.indexOf("::") + 2)),
                chapterKey: encodeKey(h.chapterKey),
              }}
              className="kt-card flex items-center gap-3 rounded-xl p-3"
            >
              <div className="relative h-12 w-9 shrink-0 overflow-hidden rounded-md bg-[color:var(--surface-2)]">
                {h.series && <Cover sourceId={h.series.sourceId} url={h.series.cover} alt="" />}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold">{h.series?.title ?? "Unknown series"}</p>
                <p className="text-[11px] text-muted-foreground">
                  {h.chapterLabel} · {new Date(h.at).toLocaleDateString()}
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

function Grid({ items }: { items: SeriesCard[] }) {
  return (
    <motion.div
      variants={staggerContainer(0.03)}
      initial="hidden"
      animate="show"
      className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6"
    >
      {items.map((s, i) => (
        <MangaCard key={idOf(s)} s={s} index={i} />
      ))}
    </motion.div>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <div className="space-y-3">
      <KuroTip mood="sleepy">{text}</KuroTip>
      <Link
        to="/browse"
        className="inline-block rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-glow)]"
      >
        Browse sources
      </Link>
    </div>
  );
}
