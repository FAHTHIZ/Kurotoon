import { createFileRoute } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { ShoppingBag, Check } from "lucide-react";
import { useShop, usePurchase, useHubProfile } from "@/lib/economy";
import { rarityRing, type Rarity } from "@/lib/brand";
import { KuroCoin } from "@/components/KuroBalance";
import { KuroTip } from "@/components/Kuro";
import { useAuth } from "@/lib/auth";
import { revealUp, staggerContainer, spring } from "@/lib/motion";

export const Route = createFileRoute("/shop")({
  head: () => ({
    meta: [
      { title: "Kuro Shop — spend your Kuro Points" },
      { name: "description", content: "Unlock badges, reader themes and profile frames with Kuro Points on KuroToon." },
      { property: "og:title", content: "Kuro Shop — spend your Kuro Points" },
      { property: "og:description", content: "Badges, reader themes and profile frames earned by reading." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Shop,
});

const KIND_LABEL: Record<string, string> = {
  sticker: "Kuro stickers",
  avatar: "Kuro avatars",
  frame: "Avatar frames",
  theme: "Reader themes",
  badge: "Badges",
  boost: "Boosts",
  banner: "Banners",
};

const KIND_ORDER = ["sticker", "avatar", "frame", "theme", "badge", "boost", "banner"];

function Shop() {
  const { user } = useAuth();
  const { items, owned } = useShop();
  const profile = useHubProfile();
  const purchase = usePurchase();

  const groups = (items.data ?? []).reduce<Record<string, typeof items.data>>((acc, it) => {
    (acc[it.kind] ||= [])!.push(it);
    return acc;
  }, {});

  const sortedGroups = Object.entries(groups).sort(
    ([a], [b]) =>
      (KIND_ORDER.indexOf(a) < 0 ? 99 : KIND_ORDER.indexOf(a)) -
      (KIND_ORDER.indexOf(b) < 0 ? 99 : KIND_ORDER.indexOf(b)),
  );


  return (
    <div className="space-y-6">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative flex items-center justify-between">
          <div>
            <h1 className="flex items-center gap-2 text-xl font-black tracking-tight">
              <ShoppingBag className="h-5 w-5 text-[color:var(--brand-gold)]" /> Kuro Shop
            </h1>
            <p className="mt-1 text-xs text-muted-foreground">Everything here is earned by reading — never bought with money.</p>
          </div>
          <span className="inline-flex items-center gap-1.5 rounded-full bg-[color:var(--surface-2)] px-3 py-1.5 text-sm font-bold ring-1 ring-border">
            <KuroCoin />
            <span className="tabular-nums">{(profile.data?.credits ?? 0).toLocaleString()}</span>
          </span>
        </div>
      </header>

      {!user && <KuroTip>Sign in to collect items — your balance and unlocks follow your account.</KuroTip>}

      {sortedGroups.map(([kind, list]) => (
        <section key={kind}>
          <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">
            {KIND_LABEL[kind] ?? kind}
          </h2>
          <motion.div
            variants={staggerContainer(0.04)}
            initial="hidden"
            animate="show"
            className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3"
          >
            {(list ?? []).map((it) => {
              const isOwned = (owned.data ?? []).includes(it.id);
              const afford = (profile.data?.credits ?? 0) >= it.price;
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
                        <Check className="h-3.5 w-3.5" /> Owned
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
