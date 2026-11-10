import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Bookmark, BookOpen, Flame, Gift, ShoppingBag, Compass, Boxes, Library, Pencil, ShieldCheck } from "lucide-react";
import { KuroCoin } from "@/components/KuroBalance";
import { Kuro } from "@/components/Kuro";
import { getBookmarks, getContinueReading } from "@/lib/local-library";
import { useRewards } from "@/lib/rewards";
import { useEngine } from "@/engine/store";
import { BackupCard } from "@/components/BackupCard";

export const Route = createFileRoute("/profile")({
  head: () => ({
    meta: [
      { title: "Me — KuroToon" },
      { name: "description", content: "Your reading stats, Kuro Points, extensions and settings. Everything stays on this device." },
      { property: "og:title", content: "Me — KuroToon" },
      { property: "og:description", content: "Reading stats, rewards and extensions — all on your device." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Profile,
});

const LINKS = [
  { to: "/library", icon: Library, label: "Library", hint: "Your saved series and history" },
  { to: "/extensions", icon: Boxes, label: "Extensions", hint: "Repositories, sources & content filters" },
  { to: "/search", icon: Compass, label: "Universal Search", hint: "Search every source at once" },
  { to: "/rewards", icon: Gift, label: "Rewards", hint: "Missions, achievements & collection" },
  { to: "/shop", icon: ShoppingBag, label: "Kuro Shop", hint: "Spend your Kuro Points" },
] as const;

const NICK = "kt.v2.nickname";

function Profile() {
  const r = useRewards();
  const engine = useEngine();
  const [stats, setStats] = useState({ bookmarks: 0, reading: 0 });
  const [nick, setNick] = useState("Reader");
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    setNick(localStorage.getItem(NICK) || "Reader");
    const sync = () => setStats({ bookmarks: getBookmarks().length, reading: getContinueReading().length });
    sync();
    window.addEventListener("kt-local-change", sync);
    return () => window.removeEventListener("kt-local-change", sync);
  }, []);

  const saveNick = (v: string) => {
    const clean = v.trim().slice(0, 32) || "Reader";
    setNick(clean);
    localStorage.setItem(NICK, clean);
    setEditing(false);
  };

  return (
    <div className="space-y-6">
      <div className="kt-card relative flex items-center gap-4 overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative grid h-16 w-16 shrink-0 place-items-center rounded-full bg-[image:var(--gradient-accent)] shadow-[var(--shadow-glow)]">
          <Kuro mood="happy" size={52} />
        </div>
        <div className="relative min-w-0 flex-1">
          {editing ? (
            <input
              autoFocus
              defaultValue={nick}
              onBlur={(e) => saveNick(e.currentTarget.value)}
              onKeyDown={(e) => e.key === "Enter" && saveNick(e.currentTarget.value)}
              className="w-full rounded-lg bg-black/30 px-2 py-1 text-lg font-bold outline-none ring-1 ring-border focus:ring-primary"
              aria-label="Nickname"
            />
          ) : (
            <button onClick={() => setEditing(true)} className="flex max-w-full items-center gap-2 text-left">
              <h1 className="truncate text-lg font-bold">{nick}</h1>
              <Pencil className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
            </button>
          )}
          <p className="mt-0.5 flex items-center gap-1 text-xs text-muted-foreground">
            <ShieldCheck className="h-3.5 w-3.5 text-[color:var(--brand-aurora)]" /> No account. Your data stays on this device.
          </p>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-[color:var(--brand-gold)]">
            {r.chaptersRead.toLocaleString()} chapters read · {engine.installed.length} sources
          </p>
        </div>
      </div>

      <Link to="/rewards" className="block rounded-2xl p-4 ring-1 ring-border" style={{ background: "linear-gradient(135deg, oklch(0.22 0.08 25), oklch(0.16 0.02 25))" }}>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-[10px] uppercase tracking-widest text-muted-foreground">Kuro Points</p>
            <div className="mt-1 flex items-center gap-2">
              <KuroCoin />
              <span className="text-2xl font-black tabular-nums">{r.points.toLocaleString()}</span>
            </div>
            <span className="mt-2 inline-flex items-center gap-1 rounded-full bg-black/30 px-2 py-0.5 text-[11px] font-bold">
              <Flame className="h-3 w-3 text-orange-400" /> {r.streak}d streak
            </span>
          </div>
          <div className="grid h-12 w-12 place-items-center rounded-full bg-primary/20">
            <Gift className="h-5 w-5 text-primary" />
          </div>
        </div>
      </Link>

      <div className="grid grid-cols-2 gap-3">
        <Link to="/library" className="kt-card rounded-2xl p-4">
          <Bookmark className="mb-2 h-5 w-5 text-primary" />
          <p className="text-2xl font-bold">{stats.bookmarks}</p>
          <p className="text-xs text-muted-foreground">In library</p>
        </Link>
        <Link to="/library" className="kt-card rounded-2xl p-4">
          <BookOpen className="mb-2 h-5 w-5 text-primary" />
          <p className="text-2xl font-bold">{stats.reading}</p>
          <p className="text-xs text-muted-foreground">Reading</p>
        </Link>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {LINKS.map(({ to, icon: Icon, label, hint }) => (
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

      <BackupCard />
    </div>
  );
}
