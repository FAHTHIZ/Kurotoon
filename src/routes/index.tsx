import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { MangaCard } from "@/components/MangaCard";
import { motion } from "framer-motion";
import { Flame, Clock, Sparkles, Star, History, ChevronRight } from "lucide-react";
import { proxied } from "@/lib/image";
import { revealUp, staggerContainer, spring, ease } from "@/lib/motion";
import { getContinueReading } from "@/lib/local-library";
import type { ProgressEntry } from "@/lib/local-library";
import { listSeries, type SeriesCard } from "@/lib/catalog";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "KuroToon — Latest manga, manhwa & doujin" },
      { name: "description", content: "Read the latest manga, manhwa and doujin updates in real time on KuroToon." },
      { property: "og:title", content: "KuroToon — Latest manga, manhwa & doujin" },
      { property: "og:description", content: "Real-time manga library: trending, latest updates and top rated, streamed on demand." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Home,
});

function Row({ icon: Icon, title, items, loading }: { icon: typeof Flame; title: string; items?: SeriesCard[]; loading: boolean }) {
  return (
    <section className="mt-8">
      <div className="mb-3 flex items-end justify-between">
        <h2 className="flex items-center gap-2 text-lg font-bold tracking-tight">
          <span className="grid h-7 w-7 place-items-center rounded-lg bg-primary/12 ring-1 ring-primary/20">
            <Icon className="h-4 w-4 text-primary" />
          </span>
          {title}
        </h2>
        <Link to="/browse" className="group inline-flex items-center gap-0.5 text-xs font-medium text-muted-foreground hover:text-primary">
          See all
          <ChevronRight className="h-3.5 w-3.5 transition-transform duration-300 group-hover:translate-x-0.5" />
        </Link>
      </div>
      <motion.div
        variants={staggerContainer(0.03)}
        initial="hidden"
        animate="show"
        className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6"
      >
        {loading
          ? Array.from({ length: 12 }).map((_, i) => (
              <div key={i} className="kt-shimmer aspect-[2/3] rounded-xl ring-1 ring-white/[0.06]" />
            ))
          : items?.map((s, i) => <MangaCard key={s.id} s={s} index={i} />)}
      </motion.div>
    </section>
  );
}

function ContinueReading() {
  const [rows, setRows] = useState<Array<{ series: SeriesCard; progress: ProgressEntry }>>([]);

  useEffect(() => {
    const sync = () => setRows(getContinueReading());
    sync();
    window.addEventListener("kt-local-change", sync);
    return () => window.removeEventListener("kt-local-change", sync);
  }, []);

  if (rows.length === 0) return null;

  return (
    <section className="mt-8">
      <h2 className="mb-3 flex items-center gap-2 text-lg font-bold tracking-tight">
        <span className="grid h-7 w-7 place-items-center rounded-lg bg-[color:var(--sapphire)]/15 ring-1 ring-[color:var(--sapphire)]/25">
          <History className="h-4 w-4 text-[color:var(--sapphire)]" />
        </span>
        Continue reading
      </h2>
      <motion.div
        variants={staggerContainer(0.04)}
        initial="hidden"
        animate="show"
        className="no-scrollbar -mx-4 flex gap-3 overflow-x-auto px-4 pb-1"
      >
        {rows.map(({ series, progress }) => (
          <motion.div key={series.id} variants={revealUp} className="w-[240px] shrink-0">
            <Link
              to="/read/$chapterId"
              params={{ chapterId: progress.chapterId }}
              className="group flex gap-3 rounded-2xl bg-[color:var(--surface)] p-2.5 ring-1 ring-white/[0.06] shadow-[var(--shadow-depth-2)] transition-shadow hover:shadow-[var(--shadow-depth-3)]"
            >
              <img
                src={proxied(series.cover_url)}
                alt=""
                referrerPolicy="no-referrer"
                className="h-20 w-14 shrink-0 rounded-lg object-cover"
              />
              <div className="flex min-w-0 flex-col justify-center">
                <p className="line-clamp-2 text-xs font-semibold group-hover:text-primary">{series.title}</p>
                <p className="mt-1 text-[11px] text-muted-foreground">Ch. {progress.chapterNumber}</p>
                <div className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-white/10">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${Math.min(100, Math.max(6, progress.page))}%` }}
                    transition={{ duration: 0.7, ease: ease.out }}
                    className="h-full rounded-full bg-[image:var(--gradient-accent)]"
                  />
                </div>
              </div>
            </Link>
          </motion.div>
        ))}
      </motion.div>
    </section>
  );
}

function Home() {
  const popular = useQuery({
    queryKey: ["md", "list", "popular"],
    queryFn: () => listSeries({ sort: "popular", limit: 18 }),
    staleTime: 5 * 60_000,
  });
  const latest = useQuery({
    queryKey: ["md", "list", "latest"],
    queryFn: () => listSeries({ sort: "latest", limit: 24 }),
    staleTime: 2 * 60_000,
  });
  const top = useQuery({
    queryKey: ["md", "list", "rating"],
    queryFn: () => listSeries({ sort: "rating", limit: 12 }),
    staleTime: 10 * 60_000,
  });

  const hero = popular.data?.items?.[0];

  return (
    <div>
      {hero && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={spring.surface}
          className="relative overflow-hidden rounded-3xl ring-1 ring-white/[0.07] shadow-[var(--shadow-depth-3)]"
          style={{ background: "var(--gradient-hero)" }}
        >
          <div className="absolute inset-0 -z-10">
            <motion.img
              src={proxied(hero.cover_url)}
              alt=""
              referrerPolicy="no-referrer"
              initial={{ scale: 1.12, opacity: 0 }}
              animate={{ scale: 1, opacity: 0.32 }}
              transition={{ duration: 1.4, ease: ease.out }}
              className="h-full w-full object-cover blur-2xl"
            />
          </div>
          <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-70" />
          <motion.div variants={staggerContainer(0.06, 0.1)} initial="hidden" animate="show" className="flex gap-4 p-4 md:p-7">
            <motion.div variants={revealUp} className="shrink-0">
              <Link to="/series/$slug" params={{ slug: hero.slug }} className="block">
                <motion.img
                  src={proxied(hero.cover_url)}
                  alt={hero.title}
                  referrerPolicy="no-referrer"
                  whileHover={{ y: -5, rotate: -1 }}
                  whileTap={{ scale: 0.97 }}
                  transition={spring.soft}
                  className="h-36 w-24 rounded-xl object-cover ring-1 ring-white/10 shadow-[var(--shadow-depth-3)] md:h-52 md:w-36"
                />
              </Link>
            </motion.div>
            <div className="flex min-w-0 flex-col justify-end">
              <motion.span variants={revealUp} className="inline-flex w-fit items-center gap-1 rounded-full bg-primary/15 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-primary ring-1 ring-primary/25">
                <Sparkles className="h-3 w-3" /> Trending
              </motion.span>
              <motion.h1 variants={revealUp} className="mt-2 text-xl font-extrabold leading-tight md:text-4xl">
                {hero.title}
              </motion.h1>
              <motion.p variants={revealUp} className="mt-1 line-clamp-2 text-xs text-muted-foreground md:text-sm">
                {hero.genres.slice(0, 4).join(" • ")}
              </motion.p>
              {hero.rating ? (
                <motion.p variants={revealUp} className="mt-1 inline-flex items-center gap-1 text-xs font-semibold">
                  <Star className="h-3 w-3 fill-[color:var(--gold)] text-[color:var(--gold)]" /> {hero.rating.toFixed(2)}
                </motion.p>
              ) : null}
              <motion.div variants={revealUp} className="mt-3">
                <Link to="/series/$slug" params={{ slug: hero.slug }}>
                  <motion.span
                    whileHover={{ scale: 1.04 }}
                    whileTap={{ scale: 0.95 }}
                    transition={spring.tap}
                    className="inline-block rounded-full bg-[image:var(--gradient-accent)] px-5 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
                  >
                    Read now
                  </motion.span>
                </Link>
              </motion.div>
            </div>
          </motion.div>
        </motion.div>
      )}

      <ContinueReading />
      <Row icon={Flame} title="Popular" items={popular.data?.items} loading={popular.isLoading} />
      <Row icon={Clock} title="Latest updates" items={latest.data?.items} loading={latest.isLoading} />
      <Row icon={Star} title="Top rated" items={top.data?.items} loading={top.isLoading} />
    </div>
  );
}
