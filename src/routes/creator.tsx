import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { motion } from "framer-motion";
import { PenTool, BookPlus, Eye, EyeOff } from "lucide-react";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/lib/auth";
import { useHubProfile, trustLabel } from "@/lib/economy";
import { Kuro, KuroTip } from "@/components/Kuro";
import { ATMOSPHERES } from "@/lib/brand";
import { spring, revealUp, staggerContainer } from "@/lib/motion";
import { toast } from "sonner";

export const Route = createFileRoute("/creator")({
  head: () => ({
    meta: [
      { title: "Creator studio — publish on KuroToon" },
      { name: "description", content: "Publish your own comics on KuroToon: create works, upload chapters and keep full ownership." },
      { property: "og:title", content: "Creator studio — publish on KuroToon" },
      { property: "og:description", content: "Create works, add chapters and publish to readers. You keep ownership." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: CreatorStudio,
});

type Work = {
  id: string;
  title: string;
  slug: string;
  synopsis: string;
  cover_url: string | null;
  status: string;
  visibility: string;
  tags: string[];
  atmosphere: string | null;
};

function slugify(s: string) {
  return s.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 60);
}

function Onboarding({ onDone }: { onDone: () => void }) {
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);
  const submit = async () => {
    if (name.trim().length < 2) return;
    setBusy(true);
    const { error } = await supabase.rpc("become_creator", { _creator_name: name.trim() });
    setBusy(false);
    if (error) return toast.error(error.message);
    toast.success("Creator profile unlocked");
    onDone();
  };
  return (
    <div className="kt-card space-y-3 rounded-3xl p-5 text-center">
      <div className="flex justify-center">
        <Kuro mood="hyped" size={92} />
      </div>
      <h1 className="text-xl font-black tracking-tight">Become a creator</h1>
      <p className="mx-auto max-w-sm text-xs text-muted-foreground">
        Publish original comics on KuroToon. Your work stays yours — you can unpublish any time.
      </p>
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Creator name"
        className="mx-auto block w-full max-w-xs rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-sm ring-1 ring-border"
      />
      <motion.button
        whileTap={{ scale: 0.96 }}
        transition={spring.tap}
        onClick={submit}
        disabled={busy}
        className="rounded-full bg-[image:var(--gradient-accent)] px-5 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
      >
        {busy ? "Setting up…" : "Create my studio"}
      </motion.button>
    </div>
  );
}

function NewWork({ onDone }: { onDone: () => void }) {
  const { user } = useAuth();
  const [title, setTitle] = useState("");
  const [synopsis, setSynopsis] = useState("");
  const [cover, setCover] = useState("");
  const [atmosphere, setAtmosphere] = useState<string>(ATMOSPHERES[0]);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (!user || title.trim().length < 2) return;
    setBusy(true);
    const { error } = await supabase.from("creator_works").insert({
      owner_id: user.id,
      title: title.trim(),
      slug: `${slugify(title)}-${Math.random().toString(36).slice(2, 6)}`,
      synopsis: synopsis.trim(),
      cover_url: cover.trim() || null,
      atmosphere,
      tags: [],
    });
    setBusy(false);
    if (error) return toast.error(error.message);
    setTitle("");
    setSynopsis("");
    setCover("");
    toast.success("Draft created");
    onDone();
  };

  return (
    <div className="kt-card space-y-3 rounded-2xl p-4">
      <p className="flex items-center gap-2 text-sm font-bold">
        <BookPlus className="h-4 w-4 text-primary" /> New work
      </p>
      <input
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="Title"
        className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
      />
      <textarea
        value={synopsis}
        onChange={(e) => setSynopsis(e.target.value)}
        rows={3}
        placeholder="Synopsis"
        className="w-full resize-none rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
      />
      <input
        value={cover}
        onChange={(e) => setCover(e.target.value)}
        placeholder="Cover image URL (optional)"
        className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
      />
      <select
        value={atmosphere}
        onChange={(e) => setAtmosphere(e.target.value)}
        className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
      >
        {ATMOSPHERES.map((a) => (
          <option key={a} value={a}>
            {a}
          </option>
        ))}
      </select>
      <motion.button
        whileTap={{ scale: 0.96 }}
        transition={spring.tap}
        onClick={submit}
        disabled={busy || title.trim().length < 2}
        className="rounded-full bg-primary px-4 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
      >
        Create draft
      </motion.button>
    </div>
  );
}

function ChapterComposer({ work, onDone }: { work: Work; onDone: () => void }) {
  const { user } = useAuth();
  const [number, setNumber] = useState("1");
  const [title, setTitle] = useState("");
  const [urls, setUrls] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    const pages = urls
      .split(/\s+/)
      .map((u) => u.trim())
      .filter(Boolean);
    if (!user || pages.length === 0) return;
    setBusy(true);
    const { error } = await supabase.from("creator_chapters").insert({
      owner_id: user.id,
      work_id: work.id,
      number: Number(number) || 1,
      title: title.trim() || null,
      pages,
      published: false,
    });
    setBusy(false);
    if (error) return toast.error(error.message);
    setUrls("");
    setTitle("");
    toast.success("Chapter draft saved");
    onDone();
  };

  return (
    <div className="mt-3 space-y-2 rounded-xl bg-[color:var(--surface-2)] p-3">
      <div className="flex gap-2">
        <input
          value={number}
          onChange={(e) => setNumber(e.target.value)}
          className="w-20 rounded-lg bg-background px-2 py-1.5 text-xs ring-1 ring-border"
          placeholder="No."
        />
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="min-w-0 flex-1 rounded-lg bg-background px-2 py-1.5 text-xs ring-1 ring-border"
          placeholder="Chapter title (optional)"
        />
      </div>
      <textarea
        value={urls}
        onChange={(e) => setUrls(e.target.value)}
        rows={3}
        placeholder="Page image URLs, one per line"
        className="w-full resize-none rounded-lg bg-background px-2 py-1.5 text-xs ring-1 ring-border"
      />
      <motion.button
        whileTap={{ scale: 0.95 }}
        transition={spring.tap}
        onClick={submit}
        disabled={busy}
        className="rounded-full bg-primary/20 px-3 py-1.5 text-[11px] font-bold text-primary ring-1 ring-primary/25 disabled:opacity-60"
      >
        Add chapter
      </motion.button>
    </div>
  );
}

function CreatorStudio() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const profile = useHubProfile();
  const [openWork, setOpenWork] = useState<string | null>(null);

  const works = useQuery({
    enabled: !!user,
    queryKey: ["creator-works", user?.id],
    queryFn: async (): Promise<Work[]> => {
      const { data } = await supabase
        .from("creator_works")
        .select("id,title,slug,synopsis,cover_url,status,visibility,tags,atmosphere")
        .eq("owner_id", user!.id)
        .order("created_at", { ascending: false });
      return (data as Work[]) ?? [];
    },
  });

  const chapters = useQuery({
    enabled: !!openWork,
    queryKey: ["creator-chapters", openWork],
    queryFn: async () => {
      const { data } = await supabase
        .from("creator_chapters")
        .select("id,number,title,published")
        .eq("work_id", openWork!)
        .order("number");
      return data ?? [];
    },
  });

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ["creator-works", user?.id] });
    qc.invalidateQueries({ queryKey: ["creator-chapters", openWork] });
  };

  const toggleVisibility = async (w: Work) => {
    const next = w.visibility === "public" ? "private" : "public";
    const { error } = await supabase
      .from("creator_works")
      .update({ visibility: next, status: next === "public" ? "published" : "draft" })
      .eq("id", w.id);
    if (error) return toast.error(error.message);
    toast.success(next === "public" ? "Published" : "Unpublished");
    refresh();
  };

  if (!user) return <KuroTip mood="idle">Sign in to open your creator studio.</KuroTip>;
  if (profile.isLoading) return <div className="kt-shimmer h-40 rounded-3xl" />;
  if (!profile.data?.is_creator) return <Onboarding onDone={() => profile.refresh()} />;

  const trust = trustLabel(profile.data.trust_level);

  return (
    <div className="space-y-5">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative flex items-center gap-4">
          <Kuro mood="reading" size={70} />
          <div>
            <h1 className="flex items-center gap-2 text-xl font-black tracking-tight">
              <PenTool className="h-5 w-5 text-[color:var(--brand-crimson)]" />
              {profile.data.creator_name ?? "Creator studio"}
            </h1>
            <p className="mt-1 text-[11px] font-bold uppercase tracking-widest text-[color:var(--brand-gold)]">
              {trust.label}
            </p>
          </div>
        </div>
      </header>

      <NewWork onDone={refresh} />

      <motion.div variants={staggerContainer(0.04)} initial="hidden" animate="show" className="space-y-3">
        {works.data?.map((w) => (
          <motion.div key={w.id} variants={revealUp} className="kt-card rounded-2xl p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-bold">{w.title}</p>
                <p className="mt-0.5 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                  {w.status} · {w.visibility}
                  {w.atmosphere ? ` · ${w.atmosphere}` : ""}
                </p>
                <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{w.synopsis}</p>
              </div>
              <motion.button
                whileTap={{ scale: 0.94 }}
                transition={spring.tap}
                onClick={() => toggleVisibility(w)}
                className="shrink-0 inline-flex items-center gap-1 rounded-full bg-[color:var(--surface-2)] px-3 py-1.5 text-[11px] font-bold ring-1 ring-border"
              >
                {w.visibility === "public" ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                {w.visibility === "public" ? "Unpublish" : "Publish"}
              </motion.button>
            </div>

            <button
              onClick={() => setOpenWork(openWork === w.id ? null : w.id)}
              className="mt-2 text-[11px] font-bold text-primary"
            >
              {openWork === w.id ? "Hide chapters" : "Manage chapters"}
            </button>

            {openWork === w.id && (
              <div className="mt-2">
                <ul className="space-y-1">
                  {(chapters.data ?? []).map((c) => (
                    <li key={c.id as string} className="flex justify-between text-xs text-muted-foreground">
                      <span>
                        Ch. {c.number as number} {(c.title as string) ?? ""}
                      </span>
                      <span>{(c.published as boolean) ? "published" : "draft"}</span>
                    </li>
                  ))}
                </ul>
                <ChapterComposer work={w} onDone={refresh} />
              </div>
            )}
          </motion.div>
        ))}
      </motion.div>

      {works.data?.length === 0 && <KuroTip mood="happy">No works yet — create your first draft above.</KuroTip>}
    </div>
  );
}
