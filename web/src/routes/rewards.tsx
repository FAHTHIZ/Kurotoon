import { createFileRoute } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { Flame, CheckCircle2, Lock, Trophy, Gift, Sparkles, PawPrint } from "lucide-react";
import { useState } from "react";
import { KuroCoin } from "@/components/KuroBalance";
import { Kuro, KuroTip } from "@/components/Kuro";
import { KuruSeasonalSpotlight, KuruVariantAlbum } from "@/components/KuruGallery";
import { ACHIEVEMENTS, MISSIONS, SHOP, claimMission, useRewards } from "@/lib/rewards";
import { ShopGrid } from "@/components/ShopGrid";
import { rarityRing } from "@/lib/brand";
import { revealUp, spring } from "@/lib/motion";

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
  const { points: credits, streak } = useRewards();
  const [tab, setTab] = useState<Tab>("missions");

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

      {tab === "missions" && <Missions />}
      {tab === "shop" && <ShopTab />}
      {tab === "achievements" && <Achievements />}
      {tab === "collection" && <Collection />}
    </div>
  );
}

function Missions() {
  const r = useRewards();
  const boosted = r.boostUntil > Date.now();
  return (
    <div className="space-y-2.5">
      <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">Today's missions</h2>
      {boosted && <KuroTip mood="hyped">Double Points is active — mission rewards are doubled.</KuroTip>}
      {MISSIONS.map((m) => {
        const p = r.progress[m.code] ?? 0;
        const done = p >= m.target;
        const claimed = r.claimed.includes(m.code);
        const pct = Math.min(100, Math.round((p / m.target) * 100));
        return (
          <motion.div key={m.code} layout className="kt-card rounded-2xl p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-bold">{m.title}</p>
                <p className="text-xs text-muted-foreground">{m.description}</p>
              </div>
              <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-black/30 px-2 py-0.5 text-xs font-bold">
                <KuroCoin />+{boosted ? m.reward * 2 : m.reward}
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
                onClick={() => claimMission(m.code)}
                className="rounded-full bg-primary px-3 py-1 text-[11px] font-bold text-primary-foreground disabled:bg-[color:var(--surface-2)] disabled:text-muted-foreground"
              >
                {claimed ? "Claimed" : done ? "Claim" : "In progress"}
              </motion.button>
            </div>
          </motion.div>
        );
      })}
      <p className="px-1 text-[11px] text-muted-foreground">Missions reset every day. Your points and unlocks are stored on this device and included in backups.</p>
    </div>
  );
}

function ShopTab() {
  return (
    <div className="space-y-6">
      <KuroTip>Every item here is earned by reading — never bought with money.</KuroTip>
      <ShopGrid items={SHOP} />
    </div>
  );
}

function Collection() {
  const r = useRewards();
  const owned = SHOP.filter((it) => r.owned.includes(it.code));
  const stickers = owned.filter((it) => it.kind === "sticker");
  const items = owned.filter((it) => it.kind !== "sticker");
  const totalStickers = SHOP.filter((it) => it.kind === "sticker").length;
  return (
    <div className="space-y-6">
      <KuruSeasonalSpotlight />
      <section>
        <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">
          Sticker collection ({stickers.length}/{totalStickers})
        </h2>
        <div className="grid grid-cols-4 gap-2.5 sm:grid-cols-6">
          {stickers.map((s) => (
            <motion.div key={s.code} variants={revealUp} initial="hidden" animate="show"
              className={`grid aspect-square place-items-center rounded-2xl bg-[color:var(--surface)] text-3xl ring-1 ${rarityRing[s.rarity]}`}>
              {s.emoji}
            </motion.div>
          ))}
        </div>
      </section>
      <section>
        <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">Unlocked items</h2>
        {items.length === 0 ? (
          <KuroTip>Badges, themes and frames you unlock will live here.</KuroTip>
        ) : (
          <div className="grid gap-2.5 sm:grid-cols-2">
            {items.map((it) => (
              <div key={it.code} className={`kt-card flex items-center gap-3 rounded-2xl p-3 ring-1 ${rarityRing[it.rarity]}`}>
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
          <Gift className="mr-1 inline h-3 w-3" /> Unlocks are kept on this device. Export a backup from your profile to move them.
        </p>
      </section>
      <KuruVariantAlbum />
    </div>
  );
}

function Achievements() {
  const r = useRewards();
  return (
    <div className="grid grid-cols-2 gap-2.5">
      {ACHIEVEMENTS.map((a) => {
        const unlocked = !!r.achievements[a.code];
        return (
          <motion.div key={a.code} layout
            className={`kt-card rounded-2xl p-3 ring-1 ${unlocked ? "ring-[color:var(--brand-gold)]/45" : "ring-border opacity-70"}`}>
            <div className="flex items-center gap-2">
              <div className="grid h-9 w-9 place-items-center rounded-full bg-[color:var(--surface-2)]">
                {unlocked ? <Trophy className="h-4 w-4 text-[color:var(--brand-gold)]" /> : <Lock className="h-3.5 w-3.5 text-muted-foreground" />}
              </div>
              <span className="text-xl">{a.icon}</span>
            </div>
            <p className="mt-2 text-sm font-bold">{a.title}</p>
            <p className="text-[11px] text-muted-foreground">{a.description}</p>
            <span className="mt-1 inline-flex items-center gap-1 text-[11px] font-bold"><KuroCoin />+{a.reward}</span>
          </motion.div>
        );
      })}
    </div>
  );
}
