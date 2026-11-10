import { createFileRoute } from "@tanstack/react-router";
import { ShoppingBag } from "lucide-react";
import { KuroCoin } from "@/components/KuroBalance";
import { KuroTip } from "@/components/Kuro";
import { ShopGrid } from "@/components/ShopGrid";
import { SHOP, useRewards } from "@/lib/rewards";

export const Route = createFileRoute("/shop")({
  head: () => ({
    meta: [
      { title: "Kuro Shop — spend your Kuro Points" },
      { name: "description", content: "Unlock stickers, reader themes and profile frames with Kuro Points earned by reading on KuroToon." },
      { property: "og:title", content: "Kuro Shop — spend your Kuro Points" },
      { property: "og:description", content: "Stickers, reader themes and profile frames earned by reading." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Shop,
});

function Shop() {
  const { points } = useRewards();
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
            <span className="tabular-nums">{points.toLocaleString()}</span>
          </span>
        </div>
      </header>
      <KuroTip>Read chapters, finish daily missions and keep your streak to earn points. Unlocks stay on this device.</KuroTip>
      <ShopGrid items={SHOP} />
    </div>
  );
}
