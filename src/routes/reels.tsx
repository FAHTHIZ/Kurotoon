import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { motion } from "framer-motion";
import { Heart, Plus, Film } from "lucide-react";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/lib/auth";
import { encodeKey } from "@/engine/ids";
import { getBookmarks, getContinueReading, idOf } from "@/lib/local-library";
import { KuroTip } from "@/components/Kuro";
import { spring, revealUp, staggerContainer } from "@/lib/motion";
import { analyzeAtmosphere } from "@/lib/atmosphere";
import { toast } from "sonner";

export const Route = createFileRoute("/reels")({
  head: () => ({
    meta: [
      { title: "Kuro Reels — panel discovery feed" },
      { name: "description", content: "Scroll a feed of standout manga panels shared by the KuroToon community." },
      { property: "og:title", content: "Kuro Reels — panel discovery feed" },
      { property: "og:description", content: "A vertical feed of standout manga moments, shared by readers." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Reels,
});

type Reel = {
  id: string;
  caption: string;
  image_url: string;
  series_ref: string;
  series_title: string;
  atmosphere: string | null;
  likes: number;
};

function Composer({ onDone }: { onDone: () => void }) {
  const { user } = useAuth();
  const options = [...getContinueReading().map((r) => r.series), ...getBookmarks()]
    .filter((s, i, a) => a.findIndex((x) => idOf(x) === idOf(s)) === i)
    .slice(0, 12);
  const [pick, setPick] = useState(options[0] ? idOf(options[0]) : "");
  const [caption, setCaption] = useState("");
  const [busy, setBusy] = useState(false);

  if (!user) return <KuroTip mood="idle">Sign in to share a reel.</KuroTip>;
  if (options.length === 0)
    return <KuroTip mood="reading">Read or bookmark a series first — then you can share it as a reel.</KuroTip>;

  const submit = async () => {
    const series = options.find((s) => idOf(s) === pick);
    if (!series || !caption.trim()) return;
    setBusy(true);
    const atmo = analyzeAtmosphere({ title: series.title, genres: series.tags });
    const { error } = await supabase.from("reels").insert({
      created_by: user.id,
      caption: caption.trim().slice(0, 240),
      image_url: series.cover ?? "",
      series_ref: idOf(series),
      series_title: series.title,
      atmosphere: atmo.mood,
    });
    setBusy(false);
    if (error) return toast.error(error.message);
    setCaption("");
    toast.success("Reel shared");
    onDone();
  };

  return (
    <div className="kt-card space-y-3 rounded-2xl p-4">
      <p className="text-sm font-bold">Share a reel</p>
      <select
        value={pick}
        onChange={(e) => setPick(e.target.value)}
        className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
      >
        {options.map((s) => (
          <option key={idOf(s)} value={idOf(s)}>
            {s.title}
          </option>
        ))}
      </select>
      <textarea
        value={caption}
        onChange={(e) => setCaption(e.target.value)}
        rows={3}
        placeholder="Why does this moment hit?"
        className="w-full resize-none rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
      />
      <motion.button
        whileTap={{ scale: 0.96 }}
        transition={spring.tap}
        onClick={submit}
        disabled={busy || !caption.trim()}
        className="inline-flex items-center gap-1.5 rounded-full bg-[image:var(--gradient-accent)] px-4 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
      >
        <Plus className="h-3.5 w-3.5" /> Post reel
      </motion.button>
    </div>
  );
}

function Reels() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const [composing, setComposing] = useState(false);

  const feed = useQuery({
    queryKey: ["reels"],
    queryFn: async (): Promise<Reel[]> => {
      const { data } = await supabase
        .from("reels")
        .select("id,caption,image_url,series_ref,series_title,atmosphere,likes")
        .order("created_at", { ascending: false })
        .limit(40);
      return (data as Reel[]) ?? [];
    },
  });

  const like = async (id: string) => {
    if (!user) return toast.error("Sign in to like reels");
    const { error } = await supabase.rpc("toggle_reel_like", { _reel_id: id });
    if (error) return toast.error(error.message);
    qc.invalidateQueries({ queryKey: ["reels"] });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-lg font-bold tracking-tight">
          <Film className="h-4 w-4 text-[color:var(--brand-crimson)]" /> Kuro Reels
        </h1>
        <motion.button
          whileTap={{ scale: 0.95 }}
          transition={spring.tap}
          onClick={() => setComposing((v) => !v)}
          className="rounded-full bg-[color:var(--surface-2)] px-3 py-1.5 text-xs font-bold ring-1 ring-border"
        >
          {composing ? "Close" : "Share"}
        </motion.button>
      </div>

      {composing && <Composer onDone={() => qc.invalidateQueries({ queryKey: ["reels"] })} />}

      {feed.isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="kt-shimmer h-72 rounded-3xl" />
          ))}
        </div>
      ) : feed.data?.length ? (
        <motion.div variants={staggerContainer(0.05)} initial="hidden" animate="show" className="space-y-4">
          {feed.data.map((r) => (
            <motion.article
              key={r.id}
              variants={revealUp}
              className="kt-card relative overflow-hidden rounded-3xl"
            >
              <img
                src={r.image_url}
                alt=""
                referrerPolicy="no-referrer"
                className="h-80 w-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent" />
              <div className="absolute inset-x-0 bottom-0 p-4">
                {r.atmosphere && (
                  <span className="mb-1 inline-block rounded-full bg-[color:var(--brand-galaxy)]/25 px-2 py-0.5 text-[10px] font-bold uppercase tracking-widest">
                    {r.atmosphere}
                  </span>
                )}
                <p className="text-sm font-semibold">{r.caption}</p>
                <div className="mt-2 flex items-center gap-3">
                  {r.series_ref.includes("::") ? (
                    <Link
                      to="/manga/$sourceId/$key"
                      params={{
                        sourceId: r.series_ref.slice(0, r.series_ref.indexOf("::")),
                        key: encodeKey(r.series_ref.slice(r.series_ref.indexOf("::") + 2)),
                      }}
                      className="rounded-full bg-primary/20 px-3 py-1 text-[11px] font-bold text-primary ring-1 ring-primary/25"
                    >
                      {r.series_title}
                    </Link>
                  ) : (
                    <span className="rounded-full bg-white/10 px-3 py-1 text-[11px] font-bold">{r.series_title}</span>
                  )}
                  <motion.button
                    whileTap={{ scale: 0.85 }}
                    transition={spring.tap}
                    onClick={() => like(r.id)}
                    className="inline-flex items-center gap-1 text-xs font-bold text-muted-foreground hover:text-[color:var(--brand-crimson)]"
                  >
                    <Heart className="h-4 w-4" /> {r.likes}
                  </motion.button>
                </div>
              </div>
            </motion.article>
          ))}
        </motion.div>
      ) : (
        <KuroTip>No reels yet. Be the first to share a moment from something you're reading.</KuroTip>
      )}
    </div>
  );
}
