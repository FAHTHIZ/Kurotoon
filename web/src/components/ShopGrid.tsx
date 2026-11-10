import { motion } from "framer-motion";
import { CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import { KuroCoin } from "@/components/KuroBalance";
import { rarityRing } from "@/lib/brand";
import { revealUp, staggerContainer, spring } from "@/lib/motion";
import { buy, useRewards, type ShopItem } from "@/lib/rewards";

const ORDER = ["sticker", "avatar", "frame", "theme", "badge", "boost"];

export const KIND_LABEL: Record<string, string> = {
  sticker: "Kuro stickers",
  avatar: "Kuro avatars",
  frame: "Avatar frames",
  theme: "Reader themes",
  badge: "Badges",
  boost: "Boosts",
};

export function ShopGrid({ items }: { items: ShopItem[] }) {
  const r = useRewards();
  const groups = items.reduce<Record<string, ShopItem[]>>((acc, it) => {
    (acc[it.kind] ||= []).push(it);
    return acc;
  }, {});
  return (
    <>
      {Object.entries(groups).sort(([a], [b]) => ORDER.indexOf(a) - ORDER.indexOf(b)).map(([kind, list]) => (
        <section key={kind}>
          <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted-foreground">{KIND_LABEL[kind] ?? kind}</h2>
          <motion.div variants={staggerContainer(0.04)} initial="hidden" animate="show" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {list.map((it) => {
              const isOwned = it.kind !== "boost" && r.owned.includes(it.code);
              const afford = r.points >= it.price;
              return (
                <motion.div key={it.code} variants={revealUp} className={`kt-card rounded-2xl p-4 ring-1 ${rarityRing[it.rarity]}`}>
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{it.rarity}</p>
                      <p className="mt-1 text-sm font-bold">{it.name}</p>
                    </div>
                    {it.emoji && <span className="text-2xl">{it.emoji}</span>}
                  </div>
                  <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{it.description}</p>
                  <div className="mt-3 flex items-center justify-between">
                    <span className="inline-flex items-center gap-1 text-xs font-bold">
                      {it.price === 0 ? "Free" : (<><KuroCoin className="h-4 w-4 text-[10px]" /> {it.price.toLocaleString()}</>)}
                    </span>
                    {isOwned ? (
                      <span className="inline-flex items-center gap-1 text-xs font-bold text-[color:var(--brand-aurora)]">
                        <CheckCircle2 className="h-3.5 w-3.5" /> Owned
                      </span>
                    ) : (
                      <motion.button
                        whileTap={{ scale: 0.94 }}
                        transition={spring.tap}
                        disabled={!afford}
                        onClick={() => {
                          const res = buy(it.code);
                          if (res.ok) toast.success("Unlocked", { description: `${it.name} added to your collection.` });
                          else toast.error(res.error);
                        }}
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
    </>
  );
}

