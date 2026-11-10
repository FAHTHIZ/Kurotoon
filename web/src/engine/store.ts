/**
 * Persistent engine state (IndexedDB via idb-keyval) + a tiny reactive layer
 * so React components re-render when repositories / sources change.
 *
 * Nothing here ever leaves the device.
 */
import { createStore, del, get, set, type UseStore } from "idb-keyval";
import { track } from "@/lib/rewards";
import { useSyncExternalStore } from "react";
import { fetchRepository, fetchViaRelay } from "./repos";
import type { InstalledSource, RepoSourceEntry, Repository, SourceHealth } from "./types";

export type EnginePrefs = {
  /** Show suggestive (rating 1) sources and titles. */
  showSuggestive: boolean;
  /** Show adult (rating 2) sources and titles. Off by default. */
  showAdult: boolean;
  /** Preferred languages for source lists, empty = all. */
  languages: string[];
};

export type EngineState = {
  ready: boolean;
  repos: Repository[];
  installed: InstalledSource[];
  prefs: EnginePrefs;
  health: Record<string, SourceHealth>;
};

const DEFAULT_PREFS: EnginePrefs = { showSuggestive: true, showAdult: false, languages: [] };

let idb: UseStore | null = null;
function db(): UseStore {
  if (!idb) idb = createStore("kurotoon-engine", "kv");
  return idb;
}

let state: EngineState = { ready: false, repos: [], installed: [], prefs: DEFAULT_PREFS, health: {} };
const listeners = new Set<() => void>();

function emit(next: Partial<EngineState>) {
  state = { ...state, ...next };
  listeners.forEach((l) => l());
}

export function getEngineState(): EngineState {
  return state;
}

export function subscribeEngine(fn: () => void): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

const SERVER_STATE: EngineState = { ready: false, repos: [], installed: [], prefs: DEFAULT_PREFS, health: {} };

export function useEngine(): EngineState {
  return useSyncExternalStore(subscribeEngine, getEngineState, () => SERVER_STATE);
}

let booting: Promise<void> | null = null;

/** Loads persisted state once per page load. Safe to call repeatedly. */
export function bootEngine(): Promise<void> {
  if (typeof window === "undefined") return Promise.resolve();
  if (!booting) {
    booting = (async () => {
      try {
        const [repos, installed, prefs, health] = await Promise.all([
          get<Repository[]>("repos", db()),
          get<InstalledSource[]>("installed", db()),
          get<EnginePrefs>("prefs", db()),
          get<Record<string, SourceHealth>>("health", db()),
        ]);
        emit({
          ready: true,
          repos: repos ?? [],
          installed: installed ?? [],
          prefs: { ...DEFAULT_PREFS, ...(prefs ?? {}) },
          health: health ?? {},
        });
      } catch {
        emit({ ready: true });
      }
    })();
  }
  return booting;
}

/* ------------------------------ repositories ----------------------------- */

export async function addRepository(input: string): Promise<Repository> {
  await bootEngine();
  const repo = await fetchRepository(input);
  const existing = state.repos.find((r) => r.url === repo.url);
  const repos = existing
    ? state.repos.map((r) => (r.url === repo.url ? { ...repo, addedAt: r.addedAt } : r))
    : [...state.repos, repo];
  await set("repos", repos, db());
  emit({ repos });
  return repo;
}

export async function refreshRepositories(): Promise<void> {
  await bootEngine();
  const repos = await Promise.all(
    state.repos.map(async (r) => {
      try {
        const fresh = await fetchRepository(r.url);
        return { ...fresh, addedAt: r.addedAt };
      } catch (e) {
        return { ...r, error: e instanceof Error ? e.message : String(e), fetchedAt: Date.now() };
      }
    }),
  );
  await set("repos", repos, db());
  emit({ repos });
}

export async function removeRepository(url: string): Promise<void> {
  await bootEngine();
  const repos = state.repos.filter((r) => r.url !== url);
  await set("repos", repos, db());
  emit({ repos });
}

/* -------------------------------- sources -------------------------------- */

const aixKey = (id: string) => `aix:${id}`;

export async function installSource(repo: Repository, entry: RepoSourceEntry): Promise<InstalledSource> {
  await bootEngine();
  const res = await fetchViaRelay(entry.downloadURL);
  if (!res.ok) throw new Error(`Download failed (${res.status})`);
  const bytes = new Uint8Array(await res.arrayBuffer());
  // .aix packages are zip files: "PK\x03\x04"
  if (bytes.length < 4 || bytes[0] !== 0x50 || bytes[1] !== 0x4b) throw new Error("Not a valid .aix package");
  if (bytes.length > 20 * 1024 * 1024) throw new Error("Package too large");
  await set(aixKey(entry.id), bytes, db());
  const record: InstalledSource = {
    id: entry.id,
    name: entry.name,
    version: entry.version,
    languages: entry.languages,
    contentRating: entry.contentRating,
    iconUrl: entry.iconURL ?? null,
    baseUrl: entry.baseURL ?? null,
    repoUrl: repo.url,
    downloadUrl: entry.downloadURL,
    installedAt: Date.now(),
    enabled: true,
  };
  const installed = [...state.installed.filter((s) => s.id !== entry.id), record].sort((a, b) =>
    a.name.localeCompare(b.name),
  );
  await set("installed", installed, db());
  emit({ installed });
  window.dispatchEvent(new CustomEvent("kt:source-updated", { detail: { id: entry.id } }));
  track("source_installed");
  return record;
}

export async function uninstallSource(id: string): Promise<void> {
  await bootEngine();
  await del(aixKey(id), db());
  await del(`settings:${id}`, db());
  const installed = state.installed.filter((s) => s.id !== id);
  await set("installed", installed, db());
  emit({ installed });
  window.dispatchEvent(new CustomEvent("kt:source-updated", { detail: { id } }));
}

export async function setSourceEnabled(id: string, enabled: boolean): Promise<void> {
  await bootEngine();
  const installed = state.installed.map((s) => (s.id === id ? { ...s, enabled } : s));
  await set("installed", installed, db());
  emit({ installed });
}

export async function readSourcePackage(id: string): Promise<Uint8Array | undefined> {
  return get<Uint8Array>(aixKey(id), db());
}

export async function readSourceSettings(id: string): Promise<Record<string, unknown>> {
  return (await get<Record<string, unknown>>(`settings:${id}`, db())) ?? {};
}

export async function writeSourceSettings(id: string, values: Record<string, unknown>): Promise<void> {
  await set(`settings:${id}`, values, db());
}

/** Sources with a newer version available in their repository. */
export function updatesAvailable(s: EngineState = state): Array<{ installed: InstalledSource; entry: RepoSourceEntry; repo: Repository }> {
  const out: Array<{ installed: InstalledSource; entry: RepoSourceEntry; repo: Repository }> = [];
  for (const inst of s.installed) {
    const repo = s.repos.find((r) => r.url === inst.repoUrl);
    const entry = repo?.sources.find((e) => e.id === inst.id);
    if (repo && entry && entry.version > inst.version) out.push({ installed: inst, entry, repo });
  }
  return out;
}

/* ------------------------------ preferences ------------------------------ */

export async function setPrefs(patch: Partial<EnginePrefs>): Promise<void> {
  await bootEngine();
  const prefs = { ...state.prefs, ...patch };
  await set("prefs", prefs, db());
  emit({ prefs });
}

export function allowsRating(rating: number, prefs: EnginePrefs = state.prefs): boolean {
  if (rating >= 2) return prefs.showAdult;
  if (rating === 1) return prefs.showSuggestive;
  return true;
}

/** Enabled sources the user's content preferences allow. */
export function activeSources(s: EngineState = state): InstalledSource[] {
  return s.installed.filter((x) => x.enabled && allowsRating(x.contentRating, s.prefs));
}

/* -------------------------------- health --------------------------------- */

let healthTimer: number | null = null;

export function recordHealth(id: string, ok: boolean, latencyMs: number, error?: string) {
  const prev = state.health[id] ?? { ok: 0, fail: 0, lastError: null, lastLatencyMs: null, lastCheckedAt: null };
  const next: SourceHealth = {
    ok: prev.ok + (ok ? 1 : 0),
    fail: prev.fail + (ok ? 0 : 1),
    lastError: ok ? prev.lastError : (error ?? "error"),
    lastLatencyMs: latencyMs,
    lastCheckedAt: Date.now(),
  };
  emit({ health: { ...state.health, [id]: next } });
  if (healthTimer) window.clearTimeout(healthTimer);
  healthTimer = window.setTimeout(() => void set("health", state.health, db()), 1500);
}

/** A source failing most recent calls is "degraded"; UI shows it, search deprioritises it. */
export function healthLabel(h: SourceHealth | undefined): "unknown" | "healthy" | "degraded" | "failing" {
  if (!h || h.ok + h.fail === 0) return "unknown";
  const rate = h.fail / (h.ok + h.fail);
  if (rate < 0.2) return "healthy";
  if (rate < 0.6) return "degraded";
  return "failing";
}
