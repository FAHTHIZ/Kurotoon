import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { LogOut, Bookmark, BookOpen, Flame, Gift, PenTool, ShoppingBag, Compass, Boxes } from "lucide-react";
import { useKuro } from "@/lib/kuro";
import { KuroCoin } from "@/components/KuroBalance";
import { useHubProfile, trustLabel } from "@/lib/economy";
import { Kuro } from "@/components/Kuro";
import { getBookmarks, getContinueReading } from "@/lib/local-library";

export const Route = createFileRoute("/profile")({
  head: () => ({
    meta: [
      { title: "Your hub — KuroToon" },
      { name: "description", content: "Your reader, contributor and creator hub: points, trust level, studio and shop in one place." },
      { property: "og:title", content: "Your hub — KuroToon" },
      { property: "og:description", content: "Reader, contributor and creator — all in one KuroToon hub." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Profile,
});

const ROLES = [
  { to: "/rewards", icon: Gift, label: "Rewards", hint: "Missions, shop & collections" },
  { to: "/creator", icon: PenTool, label: "Creator studio", hint: "Publish your own comics" },
  { to: "/shop", icon: ShoppingBag, label: "Kuro Shop", hint: "Spend your Kuro Points" },
  { to: "/extensions", icon: Boxes, label: "Extensions", hint: "Repositories, sources & content filters" },
  { to: "/search", icon: Compass, label: "Universal Search", hint: "Search every source at once" },
] as const;

function Profile() {
  const { user, loading, signOut } = useAuth();
  const navigate = useNavigate();
  const hub = useHubProfile();
  const [stats, setStats] = useState({ bookmarks: 0, reading: 0 });

  useEffect(() => { if (!loading && !user) navigate({ to: "/login" }); }, [loading, user, navigate]);

  useEffect(() => {
    const sync = () => setStats({ bookmarks: getBookmarks().length, reading: getContinueReading().length });
    sync();
    window.addEventListener("kt-local-change", sync);
    return () => window.removeEventListener("kt-local-change", sync);
  }, []);

  if (!user) return null;

  const trust = trustLabel(hub.data?.trust_level);

  return (
    <div className="space-y-6">
      <div className="kt-card relative flex items-center gap-4 overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative grid h-16 w-16 place-items-center rounded-full bg-[image:var(--gradient-accent)] text-2xl font-black text-primary-foreground shadow-[var(--shadow-glow)]">
          {hub.data?.username?.[0]?.toUpperCase() ?? "?"}
        </div>
        <div className="relative min-w-0 flex-1">
          <h1 className="truncate text-lg font-bold">{hub.data?.creator_name ?? hub.data?.username ?? "…"}</h1>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-[color:var(--brand-gold)]">
            {trust.label} · {(hub.data?.contribution_xp ?? 0).toLocaleString()} XP
          </p>
        </div>
        <Kuro mood="happy" size={56} className="relative hidden sm:block" />
      </div>

      <KuroCard />

      <div className="grid grid-cols-2 gap-3">
        <Link to="/library" className="kt-card rounded-2xl p-4">
          <Bookmark className="mb-2 h-5 w-5 text-primary" />
          <p className="text-2xl font-bold">{stats.bookmarks}</p>
          <p className="text-xs text-muted-foreground">Bookmarked</p>
        </Link>
        <Link to="/library" className="kt-card rounded-2xl p-4">
          <BookOpen className="mb-2 h-5 w-5 text-primary" />
          <p className="text-2xl font-bold">{stats.reading}</p>
          <p className="text-xs text-muted-foreground">Reading</p>
        </Link>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {ROLES.map(({ to, icon: Icon, label, hint }) => (
          <Link key={to} to={to} className="kt-card flex items-center gap-3 rounded-2xl p-4">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/12 ring-1 ring-primary/20">
              <Icon className="h-4 w-4 text-primary" />
            </span>
            <span className="min-w-0">
              <span className="block text-sm font-bold">{label}</span>
              <span className="block text-[11px] text-muted-foreground">{hint}</span>
            </span>
          </Link>
        ))}
      </div>

      <button onClick={() => signOut().then(() => navigate({ to: "/" }))}
        className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-[color:var(--surface)] py-3 text-sm font-semibold ring-1 ring-border hover:bg-[color:var(--surface-2)]">
        <LogOut className="h-4 w-4" /> Sign out
      </button>
    </div>
  );
}


function KuroCard() {
  const { credits, streak } = useKuro();
  return (
    <Link to="/rewards" className="block rounded-2xl p-4 ring-1 ring-border" style={{ background: "linear-gradient(135deg, oklch(0.22 0.08 25), oklch(0.16 0.02 25))" }}>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-[10px] uppercase tracking-widest text-muted-foreground">Kuro Points</p>
          <div className="mt-1 flex items-center gap-2">
            <KuroCoin />
            <span className="text-2xl font-black tabular-nums">{credits.toLocaleString()}</span>
          </div>
          <span className="mt-2 inline-flex items-center gap-1 rounded-full bg-black/30 px-2 py-0.5 text-[11px] font-bold">
            <Flame className="h-3 w-3 text-orange-400" /> {streak}d streak
          </span>
        </div>
        <div className="grid h-12 w-12 place-items-center rounded-full bg-primary/20">
          <Gift className="h-5 w-5 text-primary" />
        </div>
      </div>
    </Link>
  );
}
