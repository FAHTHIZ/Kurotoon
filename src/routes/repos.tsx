import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Boxes,
  Plus,
  RefreshCcw,
  Trash2,
  ChevronDown,
  CheckCircle2,
  AlertTriangle,
  Globe,
  Library,
} from "lucide-react";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/lib/auth";
import { fetchRepoManifest, type RepoManifest } from "@/lib/repos.functions";
import { loadRepoData, cacheSources, type Repository } from "@/lib/repos";
import { probeSources, type SourceProbe, type SourceStatus } from "@/lib/source-probe.functions";
import { KuroTip } from "@/components/Kuro";
import { spring, revealUp, staggerContainer } from "@/lib/motion";

const STATUS_META: Record<SourceStatus, { label: string; tone: string; icon: typeof CheckCircle2 }> = {
  ready: { label: "Readable", tone: "text-primary", icon: CheckCircle2 },
  compatible: { label: "Readable", tone: "text-primary", icon: CheckCircle2 },
  online: { label: "Online · not readable", tone: "text-amber-400", icon: AlertTriangle },
  offline: { label: "Offline", tone: "text-destructive", icon: AlertTriangle },
  blocked: { label: "Refused", tone: "text-destructive", icon: AlertTriangle },
  unknown: { label: "Not testable", tone: "text-muted-foreground", icon: AlertTriangle },
};

export const Route = createFileRoute("/repos")({
  head: () => ({
    meta: [
      { title: "Repositories — KuroToon sources & collections" },
      {
        name: "description",
        content:
          "Add and manage KuroToon repositories: install source indexes, subscribe to curated collections and toggle what feeds your library.",
      },
      { property: "og:title", content: "Repositories — KuroToon sources & collections" },
      { property: "og:description", content: "Install source repos and curated collections, then toggle what feeds your library." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: ReposPage,
});

async function saveManifest(userId: string, url: string, manifest: RepoManifest, repoId?: string) {
  let id = repoId;
  if (id) {
    const { error } = await supabase
      .from("repositories")
      .update({
        name: manifest.name,
        description: manifest.description,
        icon_url: manifest.icon,
        kind: manifest.sources.length ? "source" : "collection",
        last_synced_at: new Date().toISOString(),
      })
      .eq("id", id);
    if (error) throw new Error(error.message);
    await supabase.from("repo_sources").delete().eq("repo_id", id);
    await supabase.from("repo_items").delete().eq("repo_id", id);
  } else {
    const { data, error } = await supabase
      .from("repositories")
      .insert({
        user_id: userId,
        name: manifest.name,
        url,
        description: manifest.description,
        icon_url: manifest.icon,
        kind: manifest.sources.length ? "source" : "collection",
        last_synced_at: new Date().toISOString(),
      })
      .select("id")
      .single();
    if (error) throw new Error(error.message.includes("duplicate") ? "That repository is already added" : error.message);
    id = data.id;
  }

  if (manifest.sources.length) {
    const { error } = await supabase.from("repo_sources").insert(
      manifest.sources.map((s) => ({
        repo_id: id!,
        user_id: userId,
        code: s.code,
        name: s.name,
        lang: s.lang,
        base_url: s.baseUrl,
        version: s.version,
        nsfw: s.nsfw,
      })),
    );
    if (error) throw new Error(error.message);
  }

  const rows = manifest.collections.flatMap((c) =>
    c.items.map((it, i) => ({
      repo_id: id!,
      user_id: userId,
      collection_name: c.name,
      series_ref: it.ref,
      title: it.title,
      cover_url: it.cover,
      position: i,
    })),
  );
  if (rows.length) {
    const { error } = await supabase.from("repo_items").insert(rows);
    if (error) throw new Error(error.message);
  }
}

function AddRepo({ userId, onDone }: { userId: string; onDone: () => void }) {
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState<{ tone: "info" | "ok" | "bad"; text: string } | null>(null);

  const submit = async () => {
    if (!url.trim()) return;
    setBusy(true);
    setStatus({ tone: "info", text: "Fetching index…" });
    try {
      const res = await fetchRepoManifest({ data: { url: url.trim() } });
      if (!res.ok) {
        setStatus({ tone: "bad", text: `Failed to parse — ${res.error}` });
        toast.error(res.error);
        return;
      }
      const m = res.manifest;
      setStatus({
        tone: "info",
        text: `Extracted ${m.sources.length} source${m.sources.length === 1 ? "" : "s"}${
          m.collections.length ? ` · ${m.collections.length} collections` : ""
        }${m.viaProxy ? " (via relay)" : ""} — saving…`,
      });
      await saveManifest(userId, m.resolvedUrl, m);
      setStatus({ tone: "ok", text: `Added ${m.name} · ${m.sources.length} sources ready in Browse` });
      toast.success(`Added ${m.name}`);
      setUrl("");
      onDone();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Could not add repository";
      setStatus({ tone: "bad", text: msg });
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="kt-card space-y-3 rounded-2xl p-4">
      <p className="flex items-center gap-2 text-sm font-bold">
        <Plus className="h-4 w-4 text-primary" /> Add repository
      </p>
      <input
        value={url}
        onChange={(e) => setUrl(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && submit()}
        placeholder="https://github.com/keiyoushi/extensions"
        className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2 text-xs ring-1 ring-border"
      />
      <p className="text-[11px] leading-relaxed text-muted-foreground">
        Paste any repository link — a GitHub repo, a raw <span className="font-semibold">index.min.json</span>, a jsDelivr
        or GitHub Pages URL. KuroToon finds the index itself and falls back to a relay when a host blocks direct reads.
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <motion.button
          whileTap={{ scale: 0.96 }}
          transition={spring.tap}
          onClick={submit}
          disabled={busy || !url.trim()}
          className="rounded-full bg-[image:var(--gradient-accent)] px-4 py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
        >
          {busy ? "Working…" : "Add repository"}
        </motion.button>
        {status && (
          <span
            className={`text-[11px] font-semibold ${
              status.tone === "ok" ? "text-primary" : status.tone === "bad" ? "text-destructive" : "text-muted-foreground"
            }`}
          >
            {status.text}
          </span>
        )}
      </div>
    </div>
  );
}

function ProbeLine({ probe }: { probe?: SourceProbe }) {
  if (!probe) {
    return (
      <span className="mt-0.5 inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
        <AlertTriangle className="h-3 w-3" /> Not scanned yet
      </span>
    );
  }
  const meta = STATUS_META[probe.status];
  const Icon = meta.icon;
  return (
    <span className="mt-0.5 block">
      <span className={`inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest ${meta.tone}`}>
        <Icon className="h-3 w-3" /> {meta.label}
        {probe.latencyMs != null ? ` · ${probe.latencyMs}ms` : ""}
      </span>
      <span className="mt-0.5 block text-[10px] text-muted-foreground">
        {probe.detail}
        {probe.capabilities.length > 0 ? ` · ${probe.capabilities.join(" · ")}` : ""}
      </span>
    </span>
  );
}

function BuiltinCard({ probe }: { probe?: SourceProbe }) {
  return (
    <div className="kt-card rounded-2xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="flex items-center gap-2 text-sm font-bold">
            <Globe className="h-4 w-4 text-primary" /> KuroToon built-in
          </p>
          <p className="mt-1 text-[11px] text-muted-foreground">
            MangaDex (English) — streamed live, always enabled. This is the source powering Browse and the reader.
          </p>
          <ProbeLine probe={probe} />
        </div>
        <span className="shrink-0 inline-flex items-center gap-1 rounded-full bg-primary/12 px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest text-primary ring-1 ring-primary/20">
          <CheckCircle2 className="h-3 w-3" /> Active
        </span>
      </div>
    </div>
  );
}

function ReposPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [open, setOpen] = useState<string | null>(null);
  const [syncing, setSyncing] = useState<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [probes, setProbes] = useState<Record<string, SourceProbe>>({});
  const [scannedAt, setScannedAt] = useState<string | null>(null);
  const [scanStatus, setScanStatus] = useState<string | null>(null);

  const q = useQuery({
    enabled: !!user,
    queryKey: ["repos", user?.id],
    queryFn: () => loadRepoData(user!.id),
  });

  const refresh = () => qc.invalidateQueries({ queryKey: ["repos", user?.id] });

  // Keep installed sources mirrored locally so Browse shows them instantly.
  useEffect(() => {
    if (q.data?.sources) cacheSources(q.data.sources);
  }, [q.data?.sources]);

  const scanAll = async () => {
    if (!user) return;
    setScanning(true);
    setScanStatus("Refreshing repository indexes…");
    try {
      for (const r of q.data?.repos ?? []) {
        if (!r.url) continue;
        setScanStatus(`Fetching index — ${r.name}…`);
        const fresh = await fetchRepoManifest({ data: { url: r.url } });
        if (fresh.ok) {
          setScanStatus(`Extracted ${fresh.manifest.sources.length} sources from ${r.name}`);
          await saveManifest(user.id, fresh.manifest.resolvedUrl, fresh.manifest, r.id);
        } else {
          setScanStatus(`Failed to parse ${r.name} — ${fresh.error}`);
        }
      }
      const latest = await loadRepoData(user.id);
      cacheSources(latest.sources);
      await refresh();

      setScanStatus("Testing sources…");
      const payload = [
        { id: "builtin-mangadex", code: "mangadex", baseUrl: "https://api.mangadex.org" },
        ...latest.sources.slice(0, 59).map((s) => ({ id: s.id, code: s.code, baseUrl: s.base_url })),
      ];
      const res = await probeSources({ data: { sources: payload } });
      const map: Record<string, SourceProbe> = {};
      for (const p of res.probes) map[p.id] = p;
      setProbes(map);
      setScannedAt(res.scannedAt);
      const readable = res.probes.filter((p) => p.status === "ready" || p.status === "compatible").length;
      toast.success(`Scanned ${res.probes.length} source${res.probes.length === 1 ? "" : "s"} · ${readable} readable`);
      setScanStatus(null);
    } catch (e) {
      setScanStatus("Scan failed");
      toast.error(e instanceof Error ? e.message : "Scan failed");
    } finally {
      setScanning(false);
    }
  };


  const toggleRepo = async (r: Repository) => {
    const { error } = await supabase.from("repositories").update({ enabled: !r.enabled }).eq("id", r.id);
    if (error) return toast.error(error.message);
    refresh();
  };

  const removeRepo = async (r: Repository) => {
    const { error } = await supabase.from("repositories").delete().eq("id", r.id);
    if (error) return toast.error(error.message);
    toast.success(`Removed ${r.name}`);
    refresh();
  };

  const syncRepo = async (r: Repository) => {
    if (!r.url || !user) return;
    setSyncing(r.id);
    try {
      const res = await fetchRepoManifest({ data: { url: r.url } });
      if (!res.ok) {
        toast.error(res.error);
        return;
      }
      await saveManifest(user.id, r.url, res.manifest, r.id);
      toast.success(`${r.name} refreshed`);
      refresh();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Refresh failed");
    } finally {
      setSyncing(null);
    }
  };

  const toggleSource = async (id: string, enabled: boolean) => {
    const { error } = await supabase.from("repo_sources").update({ enabled: !enabled }).eq("id", id);
    if (error) return toast.error(error.message);
    refresh();
  };

  if (!user) return <KuroTip mood="idle">Sign in to manage your repositories.</KuroTip>;

  const repos = q.data?.repos ?? [];

  return (
    <div className="space-y-5">
      <header className="kt-card relative overflow-hidden rounded-3xl p-5">
        <div className="pointer-events-none absolute inset-0 bg-[image:var(--gradient-ambient)] opacity-60" />
        <div className="relative">
          <h1 className="flex items-center gap-2 text-xl font-black tracking-tight">
            <Boxes className="h-5 w-5 text-[color:var(--brand-crimson)]" /> Repositories
          </h1>
          <p className="mt-1 max-w-md text-xs text-muted-foreground">
            Install source indexes and curated collections, then toggle exactly what feeds your library.
          </p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <motion.button
              whileTap={{ scale: 0.96 }}
              transition={spring.tap}
              onClick={scanAll}
              disabled={scanning}
              className="inline-flex items-center gap-1.5 rounded-full bg-[image:var(--gradient-accent)] px-3.5 py-1.5 text-[11px] font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60"
            >
              <RefreshCcw className={`h-3.5 w-3.5 ${scanning ? "animate-spin" : ""}`} />
              {scanning ? "Scanning sources…" : "Scan all sources"}
            </motion.button>
            {scanStatus && (
              <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{scanStatus}</span>
            )}
            {scannedAt && !scanStatus && (
              <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                {Object.values(probes).filter((p) => p.status === "ready" || p.status === "compatible").length} readable ·{" "}
                {Object.values(probes).filter((p) => p.status === "online").length} online ·{" "}
                {Object.values(probes).filter((p) => p.status === "offline" || p.status === "blocked").length} down ·{" "}
                {new Date(scannedAt).toLocaleTimeString()}
              </span>
            )}
          </div>
        </div>
      </header>

      <BuiltinCard probe={probes["builtin-mangadex"]} />
      <AddRepo userId={user.id} onDone={refresh} />

      {q.isLoading && <div className="kt-shimmer h-24 rounded-2xl" />}

      <motion.div variants={staggerContainer(0.04)} initial="hidden" animate="show" className="space-y-3">
        {repos.map((r) => {
          const sources = (q.data?.sources ?? []).filter((s) => s.repo_id === r.id);
          const items = (q.data?.items ?? []).filter((i) => i.repo_id === r.id);
          const collections = [...new Set(items.map((i) => i.collection_name))];
          const expanded = open === r.id;
          return (
            <motion.div key={r.id} variants={revealUp} className="kt-card rounded-2xl p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-bold">{r.name}</p>
                  <p className="mt-0.5 truncate text-[10px] font-bold uppercase tracking-widest text-muted-foreground">
                    {sources.length} source{sources.length === 1 ? "" : "s"} · {items.length} collection item
                    {items.length === 1 ? "" : "s"}
                    {r.last_synced_at ? ` · synced ${new Date(r.last_synced_at).toLocaleDateString()}` : ""}
                  </p>
                  {r.description && <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{r.description}</p>}
                </div>
                <div className="flex shrink-0 items-center gap-1.5">
                  <motion.button
                    whileTap={{ scale: 0.92 }}
                    transition={spring.tap}
                    onClick={() => toggleRepo(r)}
                    className={`rounded-full px-3 py-1.5 text-[11px] font-bold ring-1 ${
                      r.enabled
                        ? "bg-primary/12 text-primary ring-primary/25"
                        : "bg-[color:var(--surface-2)] text-muted-foreground ring-border"
                    }`}
                  >
                    {r.enabled ? "Enabled" : "Disabled"}
                  </motion.button>
                  <motion.button
                    whileTap={{ scale: 0.9 }}
                    transition={spring.tap}
                    onClick={() => syncRepo(r)}
                    disabled={syncing === r.id}
                    aria-label={`Refresh ${r.name}`}
                    className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface-2)] ring-1 ring-border disabled:opacity-50"
                  >
                    <RefreshCcw className={`h-3.5 w-3.5 ${syncing === r.id ? "animate-spin" : ""}`} />
                  </motion.button>
                  <motion.button
                    whileTap={{ scale: 0.9 }}
                    transition={spring.tap}
                    onClick={() => removeRepo(r)}
                    aria-label={`Remove ${r.name}`}
                    className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface-2)] text-destructive ring-1 ring-border"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </motion.button>
                </div>
              </div>

              <button
                onClick={() => setOpen(expanded ? null : r.id)}
                className="mt-2 inline-flex items-center gap-1 text-[11px] font-bold text-primary"
              >
                {expanded ? "Hide contents" : "View contents"}
                <ChevronDown className={`h-3.5 w-3.5 transition-transform ${expanded ? "rotate-180" : ""}`} />
              </button>

              <AnimatePresence initial={false}>
                {expanded && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={spring.glide}
                    className="overflow-hidden"
                  >
                    <div className="mt-3 space-y-3">
                      {sources.length > 0 && (
                        <ul className="space-y-1.5">
                          {sources.map((s) => {
                            return (
                              <li
                                key={s.id}
                                className="flex items-center justify-between gap-2 rounded-xl bg-[color:var(--surface-2)] px-3 py-2"
                              >
                                <span className="min-w-0">
                                  <span className="block truncate text-xs font-semibold">
                                    {s.name}{" "}
                                    <span className="font-normal text-muted-foreground">
                                      {s.lang.toUpperCase()}
                                      {s.version ? ` · v${s.version}` : ""}
                                      {s.nsfw ? " · 18+" : ""}
                                    </span>
                                  </span>
                                  <ProbeLine probe={probes[s.id]} />
                                </span>
                                <button
                                  onClick={() => toggleSource(s.id, s.enabled)}
                                  className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-bold ring-1 ${
                                    s.enabled
                                      ? "bg-primary/12 text-primary ring-primary/25"
                                      : "bg-background text-muted-foreground ring-border"
                                  }`}
                                >
                                  {s.enabled ? "On" : "Off"}
                                </button>
                              </li>
                            );
                          })}
                        </ul>
                      )}

                      {collections.map((name) => (
                        <div key={name}>
                          <p className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-widest text-muted-foreground">
                            <Library className="h-3.5 w-3.5" /> {name}
                          </p>
                          <div className="mt-2 flex gap-2 overflow-x-auto pb-1">
                            {items
                              .filter((i) => i.collection_name === name)
                              .map((i) => (
                                <Link
                                  key={i.id}
                                  to="/series/$slug"
                                  params={{ slug: i.series_ref }}
                                  className="w-24 shrink-0"
                                >
                                  <span className="block aspect-[2/3] overflow-hidden rounded-lg bg-[color:var(--surface-2)] ring-1 ring-border">
                                    {i.cover_url && (
                                      <img
                                        src={`/api/public/proxy-image?url=${encodeURIComponent(i.cover_url)}`}
                                        alt={i.title}
                                        loading="lazy"
                                        className="h-full w-full object-cover"
                                      />
                                    )}
                                  </span>
                                  <span className="mt-1 line-clamp-2 block text-[10px] font-semibold">{i.title}</span>
                                </Link>
                              ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          );
        })}
      </motion.div>

      {!q.isLoading && repos.length === 0 && (
        <KuroTip mood="happy">No repositories yet — paste an index URL above to install sources and collections.</KuroTip>
      )}
    </div>
  );
}
