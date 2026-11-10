import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { Flame, CheckCircle2, Lock, Trophy, Gift, Sparkles, PawPrint } from "lucide-react";
import { useState } from "react";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/lib/auth";
import { useKuro } from "@/lib/kuro";
import { KuroCoin } from "@/components/KuroBalance";
import { Kuro, KuroTip } from "@/components/Kuro";
import { KuruSeasonalSpotlight, KuruVariantAlbum } from "@/components/KuruGallery";
import { useShop, usePurchase, useHubProfile } from "@/lib/economy";
import { rarityRing, type Rarity } from "@/lib/brand";
import { revealUp, staggerContainer, spring } from "@/lib/motion";
import { toast } from "sonner";

export const Route = createFileRoute("/rewards")({
  head: () => ({
    meta: [
      { title: "Rewards — missions, shop & collections | KuroToon" },
      { name: "description", content: "Complete daily missions, keep your streak alive and spend Kuro Points on stickers, themes and frames." },
      { property: "og:title", content: "Rewards — missions, shop & collections | KuroToon" },
      { property: "og:description", content: "Daily missions, streaks, stickers, themes and achievements on KuroToon." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Rewards,
});

type Tab = "missions" | "shop" | "achievements" | "collection";

const TABS: Array<{ id: Tab; label: string }> = [
  { id: "missions", label: "Missions" },
  { id: "shop", label: "Shop" },
  { id: "achievements", label: "Achievements" },
  { id: "collection", label: "Collection" },
];

function Rewards() {
  const { user } = useAuth();
  const { credits, streak, refresh } = useKuro();
  const [tab, setTab] = useState<Tab>("missions");

  if (!user) {
    return (
      <div className="grid place-items-center py-16 text-center">
        <Kuro mood="happy" size={110} />
        <h1 className="mt-3 text-xl font-bold">Sign in to start earning</h1>
        <p className="mt-1 max-w-xs text-xs text-muted-foreground">
          Kuro tracks your daily missions, streaks and sticker collection — reading is always free.
        </p>
        <Link to="/login" className="mt-4 rounded-full bg-primary px-5 py-2 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)]">
          Sign in
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <motion.header
        initial={{ y: -8, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={spring.soft}
        className="kt-card relative overflow-hidden rounded-3xl p-5"
      >
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-70" />
        <div className="relative flex items-center justify-between gap-4">
          <div>
            <p className="flex items-center gap-1.5 text-[11px] uppercase tracking-widest text-muted-foreground">
              <PawPrint className="h-3.5 w-3.5 text-[color:var(--brand-gold)]" /> Kuro Points
            </p>
            <div className="mt-1 flex items-center gap-2">
              <KuroCoin className="!h-7 !w-7 !text-base" />
              <span className="text-3xl font-black tabular-nums">{credits.toLocaleString()}</span>
            </div>
            <div className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-black/30 px-2.5 py-1 text-xs font-bold">
              <Flame className="h-3.5 w-3.5 text-orange-400" /> {streak} day streak
            </div>
          </div>
          <Kuro mood={streak > 0 ? "hyped" : "idle"} size={86} />
        </div>
      </motion.header>

      <div className="flex gap-1.5 overflow-x-auto rounded-full bg-[color:var(--surface)] p-1 ring-1 ring-border">
        {TABS.map((t) => (
          <motion.button
            key={t.id}
            whileTap={{ scale: 0.95 }}
            transition={spring.tap}
            onClick={() => setTab(t.id)}
            className={`flex-1 whitespace-nowrap rounded-full px-3 py-2 text-[11px] font-bold transition ${
              tab === t.id ? "bg-primary text-primary-foreground shadow-[var(--shadow-glow)]" : "text-muted-foreground"
            }`}
          >
            {t.label}
          </motion.button>
        ))}
      </div>

      {tab === "missions" && <Missions onChange={refresh} />}
      {tab === "shop" && <ShopTab />}
      {tab === "achievements" && <Achievements />}
      {tab === "collection" && <Collection />}
    </div>
  );
}

function Missions({ onChange }: { onChange: () => void }) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const today = new Date().toISOString().slice(0, 10);

  const { data } = useQuery({
    enabled: !!user,
    queryKey: ["missions", user?.id, today],
    queryFn: async () => {
      const [{ data: ms }, { data: um }] = await Promise.all([
        supabase.from("daily_missions").select("*"),
        supabase.from("user_missions").select("*").eq("user_id", user!.id).eq("day", today),
      ]);
      return (ms ?? []).map((m) => ({ ...m, progress: um?.find((u) => u.mission_id === m.id) ?? null }));
    },
  });

  const claimSafe = async (m: { id: string; reward: number; progress: { id: string; claimed: boolean } | null }) => {
    if (!user || !m.progress) return;
    await supabase.from("user_missions").update({ claimed: true }).eq("id", m.progress.id);
    const { data: prof } = await supabase.from("profiles").select("credits").eq("id", user.id).maybeSingle();
    await supabase.from("profiles").update({ credits: (prof?.credits ?? 0) + m.reward }).eq("id", user.id);
    await supabase.from("transactions").insert({ user_id: user.id, amount: m.reward, reason: "mission_claim", meta: { mission: m.id } as never });
    toast.success(`+${m.reward} Kuro Points`);
    qc.invalidateQueries({ queryKey: ["missions"] });
    onChange();
  };

  return (
    <div className="space-y-2.5">
      <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">Today's missions</h2>
      {data?.length === 0 && <KuroTip>No missions today — Kuro is napping. Check back tomorrow.</KuroTip>}
      {data?.map((m) => {
        const p = m.progress?.progress ?? 0;
        const done = p >= m.target;
        const claimed = m.progress?.claimed ?? false;
        const pct = Math.min(100, Math.round((p / m.target) * 100));
        return (
          <motion.div key={m.id} layout className="kt-card rounded-2xl p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-bold">{m.title}</p>
                <p className="text-xs text-muted-foreground">{m.description}</p>
              </div>
              <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-black/30 px-2 py-0.5 text-xs font-bold">
                <KuroCoin />+{m.reward}
              </span>
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-[color:var(--surface-2)]">
              <motion.div initial={{ width: 0 }} animate={{ width: `${pct}%` }} className="h-full bg-primary" />
            </div>
            <div className="mt-2 flex items-center justify-between text-xs">
              <span className="text-muted-foreground">{p}/{m.target}</span>
              <motion.button
                whileTap={{ scale: 0.94 }}
                transition={spring.tap}
                disabled={!done || claimed}
                onClick={() => claimSafe(m as never)}
                className="rounded-full bg-primary px-3 py-1 text-[11px] font-bold text-primary-foreground disabled:bg-[color:var(--surface-2)] disabled:text-muted-foreground"
              >
                {claimed ? "Claimed" : done ? "Claim" : "In progress"}
              </motion.button>
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}

const KIND_LABEL: Record<string, string> = {
  badge: "Badges",
  theme: "Reader themes",
  frame: "Avatar frames",
  banner: "Banners",
  boost: "Boosts",
};

function ShopTab() {
  const { user } = useAuth();
  const { items, owned } = useShop();
  const profile = useHubProfile();
  const purchase = usePurchase();
  const balance = profile.data?.credits ?? 0;

  const groups = (items.data ?? []).reduce<Record<string, NonNullable<typeof items.data>>>((acc, it) => {
    (acc[it.kind] ||= []).push(it);
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      <KuroTip>Every item here is earned by reading — never bought with money.</KuroTip>

      <Stickers balance={balance} onChange={() => profile.refresh()} />

      {Object.entries(groups).map(([kind, list]) => (
        <section key={kind}>
          <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">{KIND_LABEL[kind] ?? kind}</h2>
          <motion.div variants={staggerContainer(0.04)} initial="hidden" animate="show" className="grid gap-3 sm:grid-cols-2">
            {list.map((it) => {
              const isOwned = (owned.data ?? []).includes(it.id);
              const afford = balance >= it.price;
              return (
                <motion.div
                  key={it.id}
                  variants={revealUp}
                  className={`kt-card rounded-2xl p-4 ring-1 ${rarityRing[(it.rarity as Rarity) ?? "common"]}`}
                >
                  <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{it.rarity}</p>
                  <p className="mt-1 text-sm font-bold">{it.name}</p>
                  <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{it.description}</p>
                  <div className="mt-3 flex items-center justify-between">
                    <span className="inline-flex items-center gap-1 text-xs font-bold">
                      <KuroCoin className="h-4 w-4 text-[10px]" /> {it.price.toLocaleString()}
                    </span>
                    {isOwned ? (
                      <span className="inline-flex items-center gap-1 text-xs font-bold text-[color:var(--brand-aurora)]">
                        <CheckCircle2 className="h-3.5 w-3.5" /> Owned
                      </span>
                    ) : (
                      <motion.button
                        whileTap={{ scale: 0.94 }}
                        transition={spring.tap}
                        disabled={!user || !afford || purchase.isPending}
                        onClick={() => purchase.mutate(it.code)}
                        className="rounded-full bg-primary px-3 py-1.5 text-[11px] font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-50"
                      >
                        {afford ? "Unlock" : "Not enough"}
                      </motion.button>
                    )}
                  </div>
                </motion.div>
              );
            })}
          </motion.div>
        </section>
      ))}
    </div>
  );
}

function useStickers() {
  const { user } = useAuth();
  const all = useQuery({
    queryKey: ["stickers"],
    queryFn: async () => (await supabase.from("stickers").select("*").order("price")).data ?? [],
    staleTime: 5 * 60_000,
  });
  const mine = useQuery({
    enabled: !!user,
    queryKey: ["user_stickers", user?.id],
    queryFn: async () => (await supabase.from("user_stickers").select("sticker_id").eq("user_id", user!.id)).data ?? [],
  });
  return { all, mine, ownedIds: new Set((mine.data ?? []).map((o) => o.sticker_id)) };
}

function Stickers({ balance, onChange }: { balance: number; onChange: () => void }) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const { all, ownedIds } = useStickers();

  const buy = async (s: { id: string; price: number; name: string }) => {
    if (!user || ownedIds.has(s.id)) return;
    if (s.price > balance) {
      toast.error("Not enough Kuro Points");
      return;
    }
    if (s.price > 0) {
      const { data: prof } = await supabase.from("profiles").select("credits").eq("id", user.id).maybeSingle();
      await supabase.from("profiles").update({ credits: (prof?.credits ?? 0) - s.price }).eq("id", user.id);
      await supabase.from("transactions").insert({ user_id: user.id, amount: -s.price, reason: "sticker_purchase", meta: { sticker: s.id } as never });
    }
    await supabase.from("user_stickers").insert({ user_id: user.id, sticker_id: s.id });
    toast.success(`Unlocked ${s.name}`);
    qc.invalidateQueries({ queryKey: ["user_stickers"] });
    qc.invalidateQueries({ queryKey: ["kuro", user.id] });
    onChange();
  };

  return (
    <section>
      <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">Kuro stickers</h2>
      <div className="grid grid-cols-3 gap-2.5 sm:grid-cols-5">
        {all.data?.map((s) => {
          const isOwned = ownedIds.has(s.id);
          return (
            <motion.button
              key={s.id}
              whileTap={{ scale: 0.96 }}
              transition={spring.tap}
              onClick={() => buy(s as never)}
              className={`relative aspect-square rounded-2xl bg-[color:var(--surface)] p-3 ring-1 ${rarityRing[(s.rarity as Rarity) ?? "common"]}`}
            >
              <div className="grid h-full place-items-center text-3xl">{s.emoji}</div>
              <p className="absolute inset-x-1 bottom-1 truncate text-[10px] font-bold">{s.name}</p>
              {isOwned ? (
                <span className="absolute right-1 top-1 grid h-5 w-5 place-items-center rounded-full bg-[color:var(--brand-aurora)]/90 text-black">
                  <CheckCircle2 className="h-3 w-3" />
                </span>
              ) : s.price > 0 ? (
                <span className="absolute right-1 top-1 inline-flex items-center gap-0.5 rounded-full bg-black/60 px-1.5 py-0.5 text-[10px] font-bold">
                  <KuroCoin />{s.price}
                </span>
              ) : (
                <span className="absolute right-1 top-1 rounded-full bg-[color:var(--brand-aurora)]/80 px-1.5 py-0.5 text-[9px] font-bold text-black">FREE</span>
              )}
            </motion.button>
          );
        })}
      </div>
    </section>
  );
}

function Collection() {
  const { user } = useAuth();
  const { all, ownedIds } = useStickers();
  const { items, owned } = useShop();
  const ownedItems = (items.data ?? []).filter((it) => (owned.data ?? []).includes(it.id));
  const ownedStickers = (all.data ?? []).filter((s) => ownedIds.has(s.id));

  if (!user) return null;

  return (
    <div className="space-y-6">
      <KuruSeasonalSpotlight />

      <section>
        <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">
          Sticker collection ({ownedStickers.length}/{all.data?.length ?? 0})
        </h2>
        {ownedStickers.length === 0 ? (
          <KuroTip mood="sleepy">Your sticker album is empty. Earn points, then unlock Kuro's moods in the shop.</KuroTip>
        ) : (
          <div className="grid grid-cols-4 gap-2.5 sm:grid-cols-6">
            {ownedStickers.map((s) => (
              <motion.div
                key={s.id}
                variants={revealUp}
                initial="hidden"
                animate="show"
                className={`grid aspect-square place-items-center rounded-2xl bg-[color:var(--surface)] text-3xl ring-1 ${rarityRing[(s.rarity as Rarity) ?? "common"]}`}
              >
                {s.emoji}
              </motion.div>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">Unlocked items</h2>
        {ownedItems.length === 0 ? (
          <KuroTip>Badges, themes and frames you unlock will live here.</KuroTip>
        ) : (
          <div className="grid gap-2.5 sm:grid-cols-2">
            {ownedItems.map((it) => (
              <div key={it.id} className={`kt-card flex items-center gap-3 rounded-2xl p-3 ring-1 ${rarityRing[(it.rarity as Rarity) ?? "common"]}`}>
                <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/12 ring-1 ring-primary/20">
                  <Sparkles className="h-4 w-4 text-primary" />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-bold">{it.name}</p>
                  <p className="text-[10px] uppercase tracking-widest text-muted-foreground">{it.rarity} · {it.kind}</p>
                </div>
              </div>
            ))}
          </div>
        )}
        <p className="mt-3 px-1 text-[11px] text-muted-foreground">
          <Gift className="mr-1 inline h-3 w-3" /> Customise which badge and frame you show off from your profile.
        </p>
      </section>

      <KuruVariantAlbum />
    </div>
  );
}

function Achievements() {
  const { user } = useAuth();
  const { data } = useQuery({
    queryKey: ["achievements", user?.id],
    enabled: !!user,
    queryFn: async () => {
      const [{ data: all }, { data: mine }] = await Promise.all([
        supabase.from("achievements").select("*"),
        supabase.from("user_achievements").select("achievement_id,unlocked_at").eq("user_id", user!.id),
      ]);
      const set = new Map(mine?.map((m) => [m.achievement_id, m.unlocked_at]));
      return (all ?? []).map((a) => ({ ...a, unlocked: set.has(a.id) }));
    },
  });
  return (
    <div className="grid grid-cols-2 gap-2.5">
      {data?.map((a) => (
        <motion.div
          key={a.id}
          layout
          className={`kt-card rounded-2xl p-3 ring-1 ${a.unlocked ? "ring-[color:var(--brand-gold)]/45" : "ring-border opacity-70"}`}
        >
          <div className="flex items-center gap-2">
            <div className="grid h-9 w-9 place-items-center rounded-full bg-[color:var(--surface-2)]">
              {a.unlocked ? <Trophy className="h-4 w-4 text-[color:var(--brand-gold)]" /> : <Lock className="h-3.5 w-3.5 text-muted-foreground" />}
            </div>
            <span className="text-xl">{a.icon}</span>
          </div>
          <p className="mt-2 text-sm font-bold">{a.title}</p>
          <p className="text-[11px] text-muted-foreground">{a.description}</p>
          <span className="mt-1 inline-flex items-center gap-1 text-[11px] font-bold"><KuroCoin />+{a.reward}</span>
        </motion.div>
      ))}
    </div>
  );
}
