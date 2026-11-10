import { motion } from "framer-motion";
import { Sparkles, Lock } from "lucide-react";
import { Kuro } from "@/components/Kuro";
import { rarityRing, type Rarity } from "@/lib/brand";
import { revealUp, staggerContainer, spring } from "@/lib/motion";
import {
  KURU,
  KURU_LEGENDARY,
  KURU_PALETTE,
  KURU_SEASONAL,
  KURU_ACCESSORIES,
  activeSeasonalKuru,
  type KuruVariant,
} from "@/lib/kuru";

function accentVar(accent: KuruVariant["accent"]) {
  return KURU_PALETTE[accent];
}

/** Highlights whichever seasonal Kuru is currently in its window. */
export function KuruSeasonalSpotlight() {
  const active = activeSeasonalKuru();
  if (!active) return null;
  return (
    <motion.section
      initial={{ y: 8, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={spring.soft}
      className={`kt-card relative overflow-hidden rounded-3xl p-4 ring-1 ${rarityRing[active.rarity]}`}
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-30"
        style={{ background: `radial-gradient(120% 90% at 85% 0%, ${accentVar(active.accent)}, transparent 65%)` }}
      />
      <div className="relative flex items-center gap-4">
        <Kuro mood="hyped" size={72} />
        <div className="min-w-0">
          <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">In season now</p>
          <p className="mt-0.5 text-base font-black">{active.label}</p>
          <p className="text-xs text-muted-foreground">
            {active.window} · {active.rarity}
          </p>
        </div>
      </div>
      <p className="relative mt-3 text-[11px] text-muted-foreground">{KURU.motto}</p>
    </motion.section>
  );
}

function VariantCard({ v, unlocked }: { v: KuruVariant; unlocked: boolean }) {
  return (
    <motion.div
      variants={revealUp}
      className={`relative overflow-hidden rounded-2xl bg-[color:var(--surface)] p-3 ring-1 ${rarityRing[v.rarity as Rarity]}`}
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-25"
        style={{ background: `radial-gradient(100% 80% at 50% 0%, ${accentVar(v.accent)}, transparent 70%)` }}
      />
      <div className="relative grid place-items-center">
        <div className={unlocked ? "" : "opacity-35 grayscale"}>
          <Kuro mood={unlocked ? "happy" : "sleepy"} size={62} />
        </div>
        {!unlocked && (
          <span className="absolute right-0 top-0 grid h-5 w-5 place-items-center rounded-full bg-black/60">
            <Lock className="h-3 w-3 text-muted-foreground" />
          </span>
        )}
      </div>
      <p className="relative mt-2 truncate text-center text-[11px] font-bold">{v.label}</p>
      <p className="relative truncate text-center text-[10px] text-muted-foreground">{v.window}</p>
    </motion.div>
  );
}

/**
 * Kuru variant album — seasonal editions unlock while in season, legendary
 * easter eggs stay locked until discovered.
 */
export function KuruVariantAlbum() {
  const active = activeSeasonalKuru();
  return (
    <div className="space-y-6">
      <section>
        <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">
          Seasonal Kuru ({active ? 1 : 0}/{KURU_SEASONAL.length})
        </h2>
        <motion.div
          variants={staggerContainer(0.04)}
          initial="hidden"
          animate="show"
          className="grid grid-cols-3 gap-2.5 sm:grid-cols-4"
        >
          {KURU_SEASONAL.map((v) => (
            <VariantCard key={v.id} v={v} unlocked={active?.id === v.id} />
          ))}
        </motion.div>
      </section>

      <section>
        <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">Easter eggs</h2>
        <motion.div
          variants={staggerContainer(0.04)}
          initial="hidden"
          animate="show"
          className="grid grid-cols-3 gap-2.5 sm:grid-cols-4"
        >
          {KURU_LEGENDARY.map((v) => (
            <VariantCard key={v.id} v={v} unlocked={false} />
          ))}
        </motion.div>
      </section>

      <section>
        <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">Accessories</h2>
        <div className="grid gap-2.5 sm:grid-cols-2">
          {KURU_ACCESSORIES.map((a) => (
            <div key={a.id} className="kt-card flex items-center gap-3 rounded-2xl p-3">
              <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/12 ring-1 ring-primary/20">
                <Sparkles className="h-4 w-4 text-primary" />
              </span>
              <div className="min-w-0">
                <p className="truncate text-sm font-bold">{a.label}</p>
                <p className="truncate text-[11px] text-muted-foreground">{a.note}</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
