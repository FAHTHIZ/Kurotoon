import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import {
  AlertTriangle,
  ArrowUpCircle,
  Boxes,
  ChevronDown,
  Download,
  Loader2,
  Plus,
  RefreshCcw,
  Search,
  ShieldCheck,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import { KuroTip } from "@/components/Kuro";
import { SourceIcon } from "@/components/Cover";
import { spring, revealUp, staggerContainer } from "@/lib/motion";
import { useEngineReady } from "@/engine/hooks";
import {
  addRepository,
  allowsRating,
  healthLabel,
  installSource,
  refreshRepositories,
  removeRepository,
  setPrefs,
  setSourceEnabled,
  uninstallSource,
  updatesAvailable,
} from "@/engine/store";
import type { RepoSourceEntry, Repository } from "@/engine/types";

export const Route = createFileRoute("/extensions")({
  head: () => ({
    meta: [
      { title: "Extensions — KuroToon" },
      {
        name: "description",
        content: "Add extension repositories, install sources and manage what KuroToon can read — the Mihon / Aidoku extension model.",
      },
      { property: "og:title", content: "Extensions — KuroToon" },
      { property: "og:description", content: "Add repositories, install sources, keep them updated." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: ExtensionsPage,
});

const RATING_LABEL = ["", "16+", "18+"] as const;

const HEALTH_TONE: Record<ReturnType<typeof healthLabel>, string> = {
  unknown: "bg-white/20",
  healthy: "bg-emerald-400",
  degraded: "bg-amber-400",
  failing: "bg-destructive",
};

function langLabel(langs: string[]): string {
  if (langs.includes("multi") || langs.length > 3) return "Multi";
  return langs.map((l) => l.toUpperCase()).join(" · ");
}

function ExtensionsPage() {
  const engine = useEngineReady();
  const [tab, setTab] = useState<"installed" | "available">("installed");
  const [filter, setFilter] = useState("");
  const [busy, setBusy] = useState<Record<string, boolean>>({});
  const updates = useMemo(() => updatesAvailable(engine), [engine]);

  const run = async (id: string, fn: () => Promise<unknown>, ok?: string) => {
    setBusy((b) => ({ ...b, [id]: true }));
    try {
      await fn();
      if (ok) toast.success(ok);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Something went wrong");
    } finally {
      setBusy((b) => ({ ...b, [id]: false }));
    }
  };

  const installedIds = new Set(engine.installed.map((s) => s.id));
  const term = filter.trim().toLowerCase();
  const match = (name: string, id: string) => !term || name.toLowerCase().includes(term) || id.toLowerCase().includes(term);

  const available = engine.repos.flatMap((repo) =>
    repo.sources
      .filter((s) => !installedIds.has(s.id) && allowsRating(s.contentRating, engine.prefs) && match(s.name, s.id))
      .filter((s) => engine.prefs.languages.length === 0 || s.languages.some((l) => l === "multi" || engine.prefs.languages.includes(l)))
      .map((entry) => ({ repo, entry })),
  );

  return (
    <div className="space-y-5">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <h1 className="relative flex items-center gap-2 text-xl font-black tracking-tight">
          <Boxes className="h-5 w-5 text-primary" /> Extensions
        </h1>
        <p className="relative mt-1 text-xs leading-relaxed text-muted-foreground">
          KuroToon ships with no content. Add the extension repositories you trust, install sources, and they run on this
          device — just like Mihon and Aidoku.
        </p>
      </header>

      <AddRepo hasRepos={engine.repos.length > 0} />

      {engine.repos.length > 0 && <RepoList repos={engine.repos} />}

      {updates.length > 0 && (
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="kt-card flex items-center gap-3 rounded-2xl p-3">
          <ArrowUpCircle className="h-5 w-5 shrink-0 text-primary" />
          <p className="flex-1 text-xs font-semibold">
            {updates.length} update{updates.length === 1 ? "" : "s"} available
          </p>
          <motion.button
            whileTap={{ scale: 0.95 }}
            transition={spring.tap}
            disabled={busy["all-updates"]}
            onClick={() =>
              run(
                "all-updates",
                async () => {
                  for (const u of updates) await installSource(u.repo, u.entry);
                },
                "Sources updated",
              )
            }
            className="rounded-full bg-primary px-3 py-1.5 text-[11px] font-bold text-primary-foreground disabled:opacity-60"
          >
            {busy["all-updates"] ? "Updating…" : "Update all"}
          </motion.button>
        </motion.div>
      )}

      <div className="flex gap-1.5 rounded-full bg-[color:var(--surface)] p-1 ring-1 ring-border">
        {(["installed", "available"] as const).map((t) => (
          <motion.button
            key={t}
            whileTap={{ scale: 0.95 }}
            transition={spring.tap}
            onClick={() => setTab(t)}
            className={`relative flex-1 rounded-full px-3 py-2 text-[11px] font-bold capitalize ${
              tab === t ? "text-primary-foreground" : "text-muted-foreground"
            }`}
          >
            {tab === t && (
              <motion.span layoutId="kt-ext-tab" transition={spring.glide} className="absolute inset-0 -z-10 rounded-full bg-primary shadow-[var(--shadow-glow)]" />
            )}
            {t} <span className="tabular-nums opacity-70">{t === "installed" ? engine.installed.length : available.length}</span>
          </motion.button>
        ))}
      </div>

      <label className="flex items-center gap-2 rounded-full bg-[color:var(--surface)] px-3 py-2 ring-1 ring-border">
        <Search className="h-4 w-4 text-muted-foreground" />
        <input
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Filter sources"
          className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
      </label>

      {!engine.ready ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="kt-shimmer h-16 rounded-2xl" />
          ))}
        </div>
      ) : tab === "installed" ? (
        engine.installed.length === 0 ? (
          <KuroTip mood="sleepy">
            No sources installed yet. {engine.repos.length ? "Open the Available tab and install one." : "Add a repository first."}
          </KuroTip>
        ) : (
          <motion.ul variants={staggerContainer(0.03)} initial="hidden" animate="show" className="space-y-2">
            {engine.installed
              .filter((s) => match(s.name, s.id))
              .map((s) => {
                const h = healthLabel(engine.health[s.id]);
                const upd = updates.find((u) => u.installed.id === s.id);
                const hidden = !allowsRating(s.contentRating, engine.prefs);
                return (
                  <motion.li key={s.id} variants={revealUp} className="kt-card flex items-center gap-3 rounded-2xl p-3">
                    <SourceIcon name={s.name} url={s.iconUrl} />
                    <Link to="/source/$sourceId" params={{ sourceId: s.id }} className="min-w-0 flex-1">
                      <p className="flex items-center gap-1.5 truncate text-sm font-bold">
                        {s.name}
                        <span className={`h-1.5 w-1.5 shrink-0 rounded-full ${HEALTH_TONE[h]}`} title={`Health: ${h}`} />
                      </p>
                      <p className="truncate text-[11px] text-muted-foreground">
                        {langLabel(s.languages)} · v{s.version}
                        {s.contentRating > 0 && ` · ${RATING_LABEL[s.contentRating]}`}
                        {hidden && " · hidden by content settings"}
                        {h === "failing" && engine.health[s.id]?.lastError ? ` · ${engine.health[s.id]!.lastError}` : ""}
                      </p>
                    </Link>
                    {upd && (
                      <IconBtn label="Update" busy={busy[s.id]} onClick={() => run(s.id, () => installSource(upd.repo, upd.entry), `${s.name} updated`)}>
                        <ArrowUpCircle className="h-4 w-4 text-primary" />
                      </IconBtn>
                    )}
                    <Toggle on={s.enabled} onChange={(v) => void setSourceEnabled(s.id, v)} label={`Enable ${s.name}`} />
                    <IconBtn label="Uninstall" busy={busy[s.id]} onClick={() => run(s.id, () => uninstallSource(s.id), `${s.name} removed`)}>
                      <Trash2 className="h-4 w-4" />
                    </IconBtn>
                  </motion.li>
                );
              })}
          </motion.ul>
        )
      ) : available.length === 0 ? (
        <KuroTip mood="idle">
          {engine.repos.length ? "Nothing else to install with your current filters." : "Add a repository to see its sources."}
        </KuroTip>
      ) : (
        <ul className="space-y-2">
          {available.slice(0, 300).map(({ repo, entry }) => (
            <AvailableRow
              key={`${repo.url}#${entry.id}`}
              entry={entry}
              busy={busy[entry.id]}
              onInstall={() => run(entry.id, () => installSource(repo, entry), `${entry.name} installed`)}
            />
          ))}
        </ul>
      )}

      <ContentSettings />
    </div>
  );
}

function AvailableRow({ entry, busy, onInstall }: { entry: RepoSourceEntry; busy?: boolean; onInstall: () => void }) {
  return (
    <li className="kt-card flex items-center gap-3 rounded-2xl p-3">
      <SourceIcon name={entry.name} url={entry.iconURL ?? null} />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-bold">{entry.name}</p>
        <p className="truncate text-[11px] text-muted-foreground">
          {langLabel(entry.languages)} · v{entry.version}
          {entry.contentRating > 0 && ` · ${RATING_LABEL[entry.contentRating]}`}
        </p>
      </div>
      <motion.button
        whileTap={{ scale: 0.92 }}
        transition={spring.tap}
        disabled={busy}
        onClick={onInstall}
        className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-3 py-1.5 text-[11px] font-bold text-primary ring-1 ring-primary/25 disabled:opacity-60"
      >
        {busy ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Download className="h-3.5 w-3.5" />}
        Install
      </motion.button>
    </li>
  );
}

function AddRepo({ hasRepos }: { hasRepos: boolean }) {
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(!hasRepos);

  const submit = async () => {
    if (!url.trim()) return;
    setBusy(true);
    setError(null);
    try {
      const repo = await addRepository(url.trim());
      toast.success(`Added ${repo.name} · ${repo.sources.length} sources`);
      setUrl("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not add repository");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="kt-card rounded-2xl p-4">
      <button onClick={() => setOpen((o) => !o)} className="flex w-full items-center gap-2 text-left text-sm font-bold">
        <Plus className="h-4 w-4 text-primary" /> Add repository
        <ChevronDown className={`ml-auto h-4 w-4 text-muted-foreground transition-transform ${open ? "rotate-180" : ""}`} />
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={spring.surface}
            className="overflow-hidden"
          >
            <div className="space-y-3 pt-3">
              <input
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && submit()}
                inputMode="url"
                autoCapitalize="off"
                autoCorrect="off"
                placeholder="https://…/index.min.json"
                className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2.5 text-xs ring-1 ring-border focus:outline-none focus:ring-primary/60"
              />
              <p className="text-[11px] leading-relaxed text-muted-foreground">
                Paste an Aidoku-format source list (<span className="font-semibold">index.min.json</span>), its GitHub repository or
                an <span className="font-semibold">aidoku://</span> link. Only add repositories you trust, and respect each site's
                terms and the creators' rights.
              </p>
              {error && (
                <p className="flex items-start gap-1.5 rounded-xl bg-destructive/10 p-2.5 text-[11px] font-semibold text-destructive ring-1 ring-destructive/25">
                  <AlertTriangle className="mt-px h-3.5 w-3.5 shrink-0" /> {error}
                </p>
              )}
              <motion.button
                whileTap={{ scale: 0.96 }}
                transition={spring.tap}
                onClick={submit}
                disabled={busy || !url.trim()}
                className="inline-flex items-center gap-1.5 rounded-full bg-[image:var(--gradient-accent)] px-4 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
              >
                {busy && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                {busy ? "Reading index…" : "Add repository"}
              </motion.button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function RepoList({ repos }: { repos: Repository[] }) {
  const [refreshing, setRefreshing] = useState(false);
  return (
    <section className="space-y-2">
      <div className="flex items-center justify-between">
        <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Repositories</h2>
        <button
          onClick={async () => {
            setRefreshing(true);
            await refreshRepositories().catch(() => {});
            setRefreshing(false);
          }}
          className="inline-flex items-center gap-1 text-[11px] font-semibold text-muted-foreground hover:text-primary"
        >
          <RefreshCcw className={`h-3.5 w-3.5 ${refreshing ? "animate-spin" : ""}`} /> Refresh
        </button>
      </div>
      {repos.map((r) => (
        <div key={r.url} className="kt-card flex items-center gap-3 rounded-2xl p-3">
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-bold">{r.name}</p>
            <p className="truncate text-[11px] text-muted-foreground">
              {r.error ? <span className="text-destructive">{r.error}</span> : `${r.sources.length} sources`} · {new URL(r.url).hostname}
            </p>
          </div>
          <IconBtn
            label="Remove repository"
            onClick={() => {
              if (window.confirm(`Remove ${r.name}? Installed sources stay installed.`)) void removeRepository(r.url);
            }}
          >
            <Trash2 className="h-4 w-4" />
          </IconBtn>
        </div>
      ))}
    </section>
  );
}

function ContentSettings() {
  const { prefs } = useEngineReady();
  return (
    <section className="kt-card space-y-3 rounded-2xl p-4">
      <h2 className="flex items-center gap-2 text-sm font-bold">
        <ShieldCheck className="h-4 w-4 text-primary" /> Content filters
      </h2>
      <Row label="Suggestive (16+)" hint="Sources and titles rated suggestive" on={prefs.showSuggestive} onChange={(v) => void setPrefs({ showSuggestive: v })} />
      <Row
        label="Adult (18+)"
        hint="Off by default. Turn on only if you are of legal age where you live."
        on={prefs.showAdult}
        onChange={(v) => {
          if (v && !window.confirm("Show adult (18+) sources and titles? Confirm you are of legal age.")) return;
          void setPrefs({ showAdult: v });
        }}
      />
      <LanguagePicker />
    </section>
  );
}

const COMMON_LANGS = ["en", "ja", "ko", "zh", "es", "fr", "pt-br", "id", "vi", "it", "de", "ru", "tr", "ar"];

function LanguagePicker() {
  const { prefs } = useEngineReady();
  const toggle = (l: string) => {
    const has = prefs.languages.includes(l);
    void setPrefs({ languages: has ? prefs.languages.filter((x) => x !== l) : [...prefs.languages, l] });
  };
  return (
    <div>
      <p className="text-xs font-semibold">Languages</p>
      <p className="text-[11px] text-muted-foreground">{prefs.languages.length ? "Only these appear in Available" : "All languages"}</p>
      <div className="mt-2 flex flex-wrap gap-1.5">
        {COMMON_LANGS.map((l) => (
          <motion.button
            key={l}
            whileTap={{ scale: 0.92 }}
            transition={spring.tap}
            onClick={() => toggle(l)}
            className={`rounded-full px-2.5 py-1 text-[11px] font-bold uppercase ring-1 ${
              prefs.languages.includes(l) ? "bg-primary/20 text-primary ring-primary/40" : "bg-[color:var(--surface-2)] text-muted-foreground ring-border"
            }`}
          >
            {l}
          </motion.button>
        ))}
      </div>
    </div>
  );
}

function Row({ label, hint, on, onChange }: { label: string; hint: string; on: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="flex items-center gap-3">
      <div className="min-w-0 flex-1">
        <p className="text-xs font-semibold">{label}</p>
        <p className="text-[11px] text-muted-foreground">{hint}</p>
      </div>
      <Toggle on={on} onChange={onChange} label={label} />
    </div>
  );
}

function Toggle({ on, onChange, label }: { on: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <button
      role="switch"
      aria-checked={on}
      aria-label={label}
      onClick={() => onChange(!on)}
      className={`relative h-6 w-10 shrink-0 rounded-full transition-colors ${on ? "bg-primary" : "bg-white/15"}`}
    >
      <motion.span
        layout
        transition={spring.tap}
        className="absolute top-0.5 h-5 w-5 rounded-full bg-white shadow"
        style={{ left: on ? "1.125rem" : "0.125rem" }}
      />
    </button>
  );
}

function IconBtn({ children, label, onClick, busy }: { children: React.ReactNode; label: string; onClick: () => void; busy?: boolean }) {
  return (
    <motion.button
      whileTap={{ scale: 0.88 }}
      transition={spring.tap}
      aria-label={label}
      title={label}
      disabled={busy}
      onClick={onClick}
      className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-[color:var(--surface-2)] text-muted-foreground ring-1 ring-border hover:text-foreground disabled:opacity-50"
    >
      {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : children}
    </motion.button>
  );
}
