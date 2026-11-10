import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Boxes, ChevronRight, Flame, Globe2, History, Sparkles } from "lucide-react";
import { CardSkeletons, MangaCard } from "@/components/MangaCard";
import { Cover } from "@/components/Cover";
import { Kuro } from "@/components/Kuro";
import { revealUp, staggerContainer, spring, ease } from "@/lib/motion";
import { getContinueReading, type ProgressEntry } from "@/lib/local-library";
import { useEngineReady, usePopular } from "@/engine/hooks";
import { activeSources, allowsRating } from "@/engine/store";
import { encodeKey } from "@/engine/ids";
import type { InstalledSource, SeriesCard } from "@/engine/types";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "KuroToon — your manga, manhwa & webtoon reader" },
      { name: "description", content: "An extensible reader: add the sources you trust, search them all at once, and read on any device." },
      { property: "og:title", content: "KuroToon — your manga, manhwa & webtoon reader" },
      { property: "og:description", content: "Add sources, search everything at once, and read beautifully." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Home,
});

/** Rows render progressively; at most this many sources fetch on Home. */
const HOME_SOURCES = 4;

function Home() {
  const engine = useEngineReady();
  const sources = activeSources(engine).slice(0, HOME_SOURCES);
  const first = usePopular(sources[0]?.id ?? null);
  const hero = first.data?.items.find((m) => allowsRating(m.contentRating, engine.prefs) && m.cover);

  if (engine.ready && engine.installed.length === 0) return <Welcome hasRepos={engine.repos.length > 0} />;

  return (
    <div>
      {hero && sources[0] && <Hero s={hero} sourceName={sources[0].name} />}
      <ContinueReading />
      {sources.map((src) => (
        <SourceRow key={src.id} source={src} />
      ))}
      {engine.ready && activeSources(engine).length > HOME_SOURCES && (
        <Link
          to="/browse"
          className="mt-8 flex items-center justify-center gap-1 rounded-2xl bg-[color:var(--surface)] py-3 text-xs font-semibold text-muted-foreground ring-1 ring-border"
        >
          All sources <ChevronRight className="h-3.5 w-3.5" />
        </Link>
      )}
    </div>
  );
}

function Hero({ s, sourceName }: { s: SeriesCard; sourceName: string }) {
  const params = { sourceId: s.sourceId, key: encodeKey(s.key) };
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={spring.surface}
      className="relative overflow-hidden rounded-3xl ring-1 ring-white/[0.07] shadow-[var(--shadow-depth-3)]"
      style={{ background: "var(--gradient-hero)" }}
    >
      <motion.div
        initial={{ scale: 1.12, opacity: 0 }}
        animate={{ scale: 1, opacity: 0.32 }}
        transition={{ duration: 1.4, ease: ease.out }}
        className="absolute inset-0 -z-10 blur-2xl"
      >
        <Cover sourceId={s.sourceId} url={s.cover} alt="" eager />
      </motion.div>
      <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-70" />
      <motion.div variants={staggerContainer(0.06, 0.1)} initial="hidden" animate="show" className="relative flex gap-4 p-4 md:p-7">
        <motion.div variants={revealUp} className="shrink-0">
          <Link to="/manga/$sourceId/$key" params={params} className="block">
            <motion.div
              whileHover={{ y: -5, rotate: -1 }}
              whileTap={{ scale: 0.97 }}
              transition={spring.soft}
              className="relative h-36 w-24 overflow-hidden rounded-xl ring-1 ring-white/10 shadow-[var(--shadow-depth-3)] md:h-52 md:w-36"
            >
              <Cover sourceId={s.sourceId} url={s.cover} alt={s.title} eager />
            </motion.div>
          </Link>
        </motion.div>
        <div className="flex min-w-0 flex-col justify-end">
          <motion.span
            variants={revealUp}
            className="inline-flex w-fit items-center gap-1 rounded-full bg-primary/15 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-primary ring-1 ring-primary/25"
          >
            <Sparkles className="h-3 w-3" /> Top on {sourceName}
          </motion.span>
          <motion.h1 variants={revealUp} className="mt-2 line-clamp-3 text-xl font-extrabold leading-tight md:text-4xl">
            {s.title}
          </motion.h1>
          {s.tags.length > 0 && (
            <motion.p variants={revealUp} className="mt-1 line-clamp-2 text-xs text-muted-foreground md:text-sm">
              {s.tags.slice(0, 4).join(" • ")}
            </motion.p>
          )}
          <motion.div variants={revealUp} className="mt-3">
            <Link to="/manga/$sourceId/$key" params={params}>
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
  );
}

function SourceRow({ source }: { source: InstalledSource }) {
  const { prefs } = useEngineReady();
  const q = usePopular(source.id);
  const items = (q.data?.items ?? []).filter((m) => allowsRating(m.contentRating, prefs)).slice(0, 18);
  if (q.isSuccess && items.length === 0) return null;
  return (
    <section className="mt-8">
      <div className="mb-3 flex items-end justify-between">
        <h2 className="flex min-w-0 items-center gap-2 text-lg font-bold tracking-tight">
          <span className="grid h-7 w-7 shrink-0 place-items-center rounded-lg bg-primary/12 ring-1 ring-primary/20">
            <Flame className="h-4 w-4 text-primary" />
          </span>
          <span className="truncate">{source.name}</span>
          {q.data?.listing?.name && <span className="truncate text-xs font-medium text-muted-foreground">· {q.data.listing.name}</span>}
        </h2>
        <Link
          to="/source/$sourceId"
          params={{ sourceId: source.id }}
          className="group inline-flex shrink-0 items-center gap-0.5 text-xs font-medium text-muted-foreground hover:text-primary"
        >
          See all
          <ChevronRight className="h-3.5 w-3.5 transition-transform duration-300 group-hover:translate-x-0.5" />
        </Link>
      </div>
      {q.error ? (
        <p className="kt-card rounded-2xl p-4 text-xs text-muted-foreground">
          {source.name} is unavailable right now. <button onClick={() => void q.refetch()} className="font-semibold text-primary">Retry</button>
        </p>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {q.isLoading ? <CardSkeletons count={6} /> : items.map((s, i) => <MangaCard key={s.key} s={s} index={i} />)}
        </div>
      )}
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
      <motion.div variants={staggerContainer(0.04)} initial="hidden" animate="show" className="no-scrollbar -mx-4 flex gap-3 overflow-x-auto px-4 pb-1">
        {rows.map(({ series, progress }) => (
          <motion.div key={`${series.sourceId}:${series.key}`} variants={revealUp} className="w-[240px] shrink-0">
            <Link
              to="/read/$sourceId/$mangaKey/$chapterKey"
              params={{ sourceId: series.sourceId, mangaKey: encodeKey(series.key), chapterKey: encodeKey(progress.chapterKey) }}
              className="group flex gap-3 rounded-2xl bg-[color:var(--surface)] p-2.5 ring-1 ring-white/[0.06] shadow-[var(--shadow-depth-2)] transition-shadow hover:shadow-[var(--shadow-depth-3)]"
            >
              <div className="relative h-20 w-14 shrink-0 overflow-hidden rounded-lg">
                <Cover sourceId={series.sourceId} url={series.cover} alt="" />
              </div>
              <div className="flex min-w-0 flex-1 flex-col justify-center">
                <p className="line-clamp-2 text-xs font-semibold group-hover:text-primary">{series.title}</p>
                <p className="mt-1 text-[11px] text-muted-foreground">
                  {progress.chapterLabel} · p.{progress.page}/{progress.total}
                </p>
                <div className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-white/10">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${Math.min(100, Math.max(6, (progress.page / Math.max(1, progress.total)) * 100))}%` }}
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

function Welcome({ hasRepos }: { hasRepos: boolean }) {
  const steps = [
    { icon: Boxes, title: "Add a repository", text: "Paste an extension repository you trust in Extensions." },
    { icon: Sparkles, title: "Install sources", text: "Pick the sources you want. They run on your device." },
    { icon: Globe2, title: "Search everything", text: "Universal Search queries every source at once." },
  ];
  return (
    <motion.div variants={staggerContainer(0.08)} initial="hidden" animate="show" className="space-y-5 py-4">
      <motion.div
        variants={revealUp}
        className="kt-card relative overflow-hidden rounded-3xl p-6 text-center"
        style={{ background: "var(--gradient-hero)" }}
      >
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-70" />
        <div className="relative mx-auto w-fit">
          <Kuro mood="hyped" size={88} />
        </div>
        <h1 className="relative mt-3 text-2xl font-black tracking-tight">Welcome to KuroToon</h1>
        <p className="relative mx-auto mt-1 max-w-sm text-xs leading-relaxed text-muted-foreground">
          KuroToon is a reader, like Mihon and Aidoku. It comes with no content — you choose the sources.
        </p>
        <Link to="/extensions" className="relative mt-5 inline-block">
          <motion.span
            whileHover={{ scale: 1.04 }}
            whileTap={{ scale: 0.95 }}
            transition={spring.tap}
            className="inline-block rounded-full bg-[image:var(--gradient-accent)] px-6 py-2.5 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
          >
            {hasRepos ? "Install a source" : "Add a repository"}
          </motion.span>
        </Link>
      </motion.div>
      {steps.map(({ icon: Icon, title, text }, i) => (
        <motion.div key={title} variants={revealUp} className="kt-card flex items-center gap-3 rounded-2xl p-4">
          <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-primary/12 ring-1 ring-primary/20">
            <Icon className="h-5 w-5 text-primary" />
          </span>
          <div>
            <p className="text-sm font-bold">
              {i + 1}. {title}
            </p>
            <p className="text-[11px] text-muted-foreground">{text}</p>
          </div>
        </motion.div>
      ))}
    </motion.div>
  );
}
