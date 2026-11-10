import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { motion } from "framer-motion";
import { Wrench, Search } from "lucide-react";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/lib/auth";
import { useHubProfile, trustLabel } from "@/lib/economy";
import { suggest, type Suggestion } from "@/lib/catalog";
import { KuroTip } from "@/components/Kuro";
import { proxied } from "@/lib/image";
import { spring, revealUp, staggerContainer } from "@/lib/motion";
import { toast } from "sonner";

export const Route = createFileRoute("/contribute")({
  head: () => ({
    meta: [
      { title: "Contribute — improve the KuroToon catalogue" },
      { name: "description", content: "Report wrong covers, missing chapters and bad tags. Earn Kuro Points and trust for accepted fixes." },
      { property: "og:title", content: "Contribute — improve the KuroToon catalogue" },
      { property: "og:description", content: "Submit catalogue fixes and earn Kuro Points for accepted contributions." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Contribute,
});

const KINDS = [
  { id: "tag_fix", label: "Wrong or missing tags" },
  { id: "metadata_fix", label: "Wrong title / author / year" },
  { id: "cover_fix", label: "Wrong or broken cover" },
  { id: "missing_chapter", label: "Missing chapter" },
  { id: "other", label: "Something else" },
] as const;

type Row = {
  id: string;
  kind: string;
  target_title: string | null;
  status: string;
  note: string | null;
  created_at: string;
};

function Contribute() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const profile = useHubProfile();

  const [kind, setKind] = useState<string>(KINDS[0].id);
  const [term, setTerm] = useState("");
  const [target, setTarget] = useState<Suggestion | null>(null);
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);

  const hits = useQuery({
    enabled: term.trim().length > 1 && !target,
    queryKey: ["contrib-suggest", term.trim()],
    queryFn: () => suggest(term.trim()),
    staleTime: 60_000,
  });

  const mine = useQuery({
    enabled: !!user,
    queryKey: ["my-contributions", user?.id],
    queryFn: async (): Promise<Row[]> => {
      const { data } = await supabase
        .from("contributions")
        .select("id,kind,target_title,status,note,created_at")
        .eq("user_id", user!.id)
        .order("created_at", { ascending: false })
        .limit(25);
      return (data as Row[]) ?? [];
    },
  });

  const submit = async () => {
    if (!target || note.trim().length < 8) return;
    setBusy(true);
    const { data, error } = await supabase.rpc("submit_contribution", {
      _kind: kind,
      _note: note.trim().slice(0, 800),
      _payload: { source: "web" },
      _target_ref: target.id,
      _target_title: target.title,
    });
    setBusy(false);
    if (error) return toast.error(error.message);
    const r = data as { ok?: boolean; error?: string; reward?: number } | null;
    if (!r?.ok) return toast.error(r?.error ?? "Could not submit");
    toast.success("Contribution submitted", {
      description: r.reward ? `+${r.reward} Kuro Points` : "Thanks for helping the catalogue.",
    });
    setNote("");
    setTarget(null);
    setTerm("");
    qc.invalidateQueries({ queryKey: ["my-contributions", user?.id] });
    profile.refresh();
  };

  if (!user) return <KuroTip mood="idle">Sign in to submit catalogue fixes and earn Kuro Points.</KuroTip>;

  const trust = trustLabel(profile.data?.trust_level);

  return (
    <div className="space-y-5">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative">
          <h1 className="flex items-center gap-2 text-xl font-black tracking-tight">
            <Wrench className="h-5 w-5 text-[color:var(--brand-aurora)]" /> Contributor desk
          </h1>
          <p className="mt-1 text-xs text-muted-foreground">
            Accepted fixes earn Kuro Points and move you up the trust ladder.
          </p>
          <div className="mt-3 flex gap-2 text-[11px] font-bold">
            <span className="rounded-full bg-[color:var(--surface-2)] px-3 py-1 ring-1 ring-border">{trust.label}</span>
            <span className="rounded-full bg-[color:var(--surface-2)] px-3 py-1 ring-1 ring-border">
              {(profile.data?.contribution_xp ?? 0).toLocaleString()} XP
            </span>
          </div>
        </div>
      </header>

      <section className="kt-card space-y-3 rounded-2xl p-4">
        <select
          value={kind}
          onChange={(e) => setKind(e.target.value)}
          className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
        >
          {KINDS.map((k) => (
            <option key={k.id} value={k.id}>
              {k.label}
            </option>
          ))}
        </select>

        {target ? (
          <div className="flex items-center gap-2 rounded-xl bg-[color:var(--surface-2)] p-2 ring-1 ring-border">
            {target.cover_url && (
              <img src={proxied(target.cover_url)} alt="" referrerPolicy="no-referrer" className="h-12 w-8 rounded object-cover" />
            )}
            <span className="min-w-0 flex-1 truncate text-xs font-semibold">{target.title}</span>
            <button onClick={() => setTarget(null)} className="text-[11px] font-bold text-primary">
              Change
            </button>
          </div>
        ) : (
          <div>
            <div className="flex items-center gap-2 rounded-xl bg-[color:var(--surface-2)] px-3 py-2 ring-1 ring-border">
              <Search className="h-3.5 w-3.5 text-muted-foreground" />
              <input
                value={term}
                onChange={(e) => setTerm(e.target.value)}
                placeholder="Which series?"
                className="w-full bg-transparent text-xs outline-none"
              />
            </div>
            {(hits.data ?? []).length > 0 && (
              <ul className="mt-2 space-y-1">
                {(hits.data ?? []).slice(0, 6).map((s) => (
                  <li key={s.id}>
                    <button
                      onClick={() => setTarget(s)}
                      className="w-full truncate rounded-lg px-2 py-1.5 text-left text-xs hover:bg-[color:var(--surface-2)]"
                    >
                      {s.title}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          rows={4}
          placeholder="Describe the fix precisely — what's wrong and what it should be."
          className="w-full resize-none rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
        />

        <motion.button
          whileTap={{ scale: 0.96 }}
          transition={spring.tap}
          onClick={submit}
          disabled={busy || !target || note.trim().length < 8}
          className="rounded-full bg-[image:var(--gradient-accent)] px-4 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
        >
          {busy ? "Submitting…" : "Submit fix"}
        </motion.button>
      </section>

      <section>
        <h2 className="mb-2 text-sm font-bold uppercase tracking-widest text-muted-foreground">Your submissions</h2>
        {mine.data?.length ? (
          <motion.ul variants={staggerContainer(0.03)} initial="hidden" animate="show" className="space-y-2">
            {mine.data.map((r) => (
              <motion.li key={r.id} variants={revealUp} className="kt-card rounded-xl p-3">
                <div className="flex items-center justify-between gap-2">
                  <span className="min-w-0 truncate text-xs font-semibold">{r.target_title ?? "—"}</span>
                  <span className="shrink-0 rounded-full bg-[color:var(--surface-2)] px-2 py-0.5 text-[10px] font-bold uppercase tracking-widest ring-1 ring-border">
                    {r.status}
                  </span>
                </div>
                <p className="mt-1 line-clamp-2 text-[11px] text-muted-foreground">{r.note}</p>
              </motion.li>
            ))}
          </motion.ul>
        ) : (
          <KuroTip mood="reading">Nothing submitted yet. Spot something wrong while reading? Report it here.</KuroTip>
        )}
      </section>
    </div>
  );
}
