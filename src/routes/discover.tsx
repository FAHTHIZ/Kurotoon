import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { motion } from "framer-motion";
import { Flame, Star, Clock, Gem, Shuffle, ChevronRight } from "lucide-react";
import { listSeries, type ListParams, type SeriesCard } from "@/lib/catalog";
import { MangaCard } from "@/components/MangaCard";
import { Kuro } from "@/components/Kuro";
import { revealUp, staggerContainer, spring } from "@/lib/motion";

export const Route = createFileRoute("/discover")({
  head: () => ({
    meta: [
      { title: "Discover — curated manga rails | KuroToon" },
      {
        name: "description",
        content: "Popular, trending, newly added and hidden gems — curated rails streamed live from the catalogue.",
      },
      { property: "og:title", content: "Discover — curated manga rails | KuroToon" },
      { property: "og:description", content: "Popular, trending, new and hidden gem manga rails on KuroToon." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Discover,
});

type Rail = {
  id: string;
  title: string;
  hint: string;
  icon: typeof Flame;
  params: ListParams;
};

const RAILS: Rail[] = [
  { id: "popular", title: "Most popular", hint: "What everyone is reading right now", icon: Flame, params: { sort: "popular", limit: 12 } },
  { id: "trending", title: "Trending updates", hint: "Series with fresh chapters", icon: Clock, params: { sort: "latest", limit: 12 } },
  { id: "top", title: "Top rated", hint: "The highest scored titles on the catalogue", icon: Star, params: { sort: "rating", limit: 12 } },
  { id: "gems", title: "Hidden gems", hint: "Completed series that deserve more readers", icon: Gem, params: { sort: "rating", status: ["completed"], limit: 12 } },
];

const COLLECTIONS: Array<{ label: string; genres: string[] }> = [
  { label: "Dark & psychological", genres: ["psychological"] },
  { label: "Epic action", genres: ["action"] },
  { label: "Cozy slice of life", genres: ["slice of life"] },
  { label: "Slow-burn romance", genres: ["romance"] },
  { label: "Mystery & thriller", genres: ["mystery"] },
  { label: "Fantasy worlds", genres: ["fantasy"] },
];

function RailRow({ rail }: { rail: Rail }) {
  const q = useQuery({
    queryKey: ["rail", rail.id],
    queryFn: () => listSeries(rail.params),
    staleTime: 5 * 60_000,
  });
  const Icon = rail.icon;

  return (
    <section className="mt-8">
      <div className="mb-3 flex items-end justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-bold tracking-tight">
            <span className="grid h-7 w-7 place-items-center rounded-lg bg-primary/12 ring-1 ring-primary/20">
              <Icon className="h-4 w-4 text-primary" />
            </span>
            {rail.title}
          </h2>
          <p className="mt-0.5 text-[11px] text-muted-foreground">{rail.hint}</p>
        </div>
        <Link to="/browse" className="group inline-flex items-center gap-0.5 text-xs font-medium text-muted-foreground hover:text-primary">
          See all <ChevronRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
        </Link>
      </div>
      <motion.div
        variants={staggerContainer(0.03)}
        initial="hidden"
        animate="show"
        className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6"
      >
        {q.isLoading
          ? Array.from({ length: 6 }).map((_, i) => <div key={i} className="kt-shimmer aspect-[2/3] rounded-xl ring-1 ring-white/[0.06]" />)
          : q.data?.items.map((s, i) => <MangaCard key={s.id} s={s} index={i} />)}
      </motion.div>
    </section>
  );
}

function Discover() {
  const [collection, setCollection] = useState(COLLECTIONS[0]!.label);
  const [surprise, setSurprise] = useState<SeriesCard[]>([]);

  const active = COLLECTIONS.find((c) => c.label === collection)!;
  const collectionQuery = useQuery({
    queryKey: ["collection", active.label],
    queryFn: () => listSeries({ sort: "popular", genres: active.genres, limit: 12 }),
    staleTime: 5 * 60_000,
  });

  const shuffle = async () => {
    const res = await listSeries({ sort: "popular", limit: 60 });
    setSurprise([...res.items].sort(() => Math.random() - 0.5).slice(0, 6));
  };

  return (
    <div>
      <section className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative flex items-center gap-4">
          <Kuro mood="reading" size={78} />
          <div>
            <h1 className="text-xl font-black tracking-tight">Discover</h1>
            <p className="mt-1 text-xs text-muted-foreground">
              Hand-built rails and themed collections — nothing is stored, every shelf is streamed live.
            </p>
          </div>
        </div>
        <motion.button
          whileTap={{ scale: 0.96 }}
          transition={spring.tap}
          onClick={shuffle}
          className="relative mt-4 inline-flex items-center gap-1.5 rounded-full bg-[image:var(--gradient-accent)] px-4 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
        >
          <Shuffle className="h-3.5 w-3.5" /> Surprise me
        </motion.button>
      </section>

      {surprise.length > 0 && (
        <section className="mt-6">
          <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">Kuro's random picks</h2>
          <motion.div
            variants={staggerContainer(0.04)}
            initial="hidden"
            animate="show"
            className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6"
          >
            {surprise.map((s, i) => (
              <motion.div key={s.id} variants={revealUp}>
                <MangaCard s={s} index={i} />
              </motion.div>
            ))}
          </motion.div>
        </section>
      )}

      {RAILS.map((r) => (
        <RailRow key={r.id} rail={r} />
      ))}

      <section className="mt-10">
        <h2 className="mb-3 text-lg font-bold tracking-tight">Featured collections</h2>
        <div className="mb-3 flex flex-wrap gap-2">
          {COLLECTIONS.map((c) => (
            <motion.button
              key={c.label}
              whileTap={{ scale: 0.94 }}
              transition={spring.tap}
              onClick={() => setCollection(c.label)}
              className={`rounded-full px-3 py-1.5 text-[11px] font-bold ring-1 transition ${
                collection === c.label
                  ? "bg-primary text-primary-foreground ring-primary/40 shadow-[var(--shadow-glow)]"
                  : "bg-[color:var(--surface-2)] text-muted-foreground ring-border"
              }`}
            >
              {c.label}
            </motion.button>
          ))}
        </div>
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {collectionQuery.isLoading
            ? Array.from({ length: 6 }).map((_, i) => <div key={i} className="kt-shimmer aspect-[2/3] rounded-xl ring-1 ring-white/[0.06]" />)
            : collectionQuery.data?.items.map((s, i) => <MangaCard key={s.id} s={s} index={i} />)}
        </div>
      </section>
    </div>
  );
}
