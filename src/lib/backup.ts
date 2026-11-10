/**
 * Backup & restore, modelled on Mihon's backup flow: a single JSON file with
 * the library, reading progress, history, reader settings, rewards,
 * extension repositories, the list of installed sources (not their binaries —
 * they are re-downloaded from their repository on restore) and per-source
 * settings. Nothing is uploaded anywhere; the file is saved by the browser.
 */
import {
  addRepository,
  bootEngine,
  getEngineState,
  installSource,
  readSourceSettings,
  setPrefs,
  setSourceEnabled,
  writeSourceSettings,
  type EnginePrefs,
} from "@/engine/store";

export const BACKUP_VERSION = 1;

export type Backup = {
  app: "kurotoon";
  version: number;
  createdAt: string;
  local: Record<string, string>;
  engine: {
    repos: string[];
    installed: Array<{ id: string; repoUrl: string; enabled: boolean }>;
    prefs: EnginePrefs;
    settings: Record<string, Record<string, unknown>>;
  };
};

/** Every localStorage key KuroToon owns starts with "kt.". */
const OWNED = (k: string) => k.startsWith("kt.");

export async function createBackup(): Promise<Backup> {
  await bootEngine();
  const s = getEngineState();
  const local: Record<string, string> = {};
  for (let i = 0; i < localStorage.length; i++) {
    const k = localStorage.key(i);
    if (k && OWNED(k)) local[k] = localStorage.getItem(k) ?? "";
  }
  const settings: Backup["engine"]["settings"] = {};
  for (const src of s.installed) {
    const v = await readSourceSettings(src.id);
    if (Object.keys(v).length) settings[src.id] = v;
  }
  return {
    app: "kurotoon",
    version: BACKUP_VERSION,
    createdAt: new Date().toISOString(),
    local,
    engine: {
      repos: s.repos.map((r) => r.url),
      installed: s.installed.map((x) => ({ id: x.id, repoUrl: x.repoUrl, enabled: x.enabled })),
      prefs: s.prefs,
      settings,
    },
  };
}

export function backupFileName(d = new Date()) {
  return `kurotoon_${d.toISOString().slice(0, 16).replace(/[:T]/g, "-")}.json`;
}

export async function downloadBackup() {
  const b = await createBackup();
  const blob = new Blob([JSON.stringify(b)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = backupFileName();
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

/** Validates an untrusted backup file. Throws with a readable message. */
export function parseBackup(text: string): Backup {
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new Error("This file is not valid JSON.");
  }
  const b = raw as Partial<Backup>;
  if (!b || typeof b !== "object" || b.app !== "kurotoon") throw new Error("This is not a KuroToon backup.");
  if (typeof b.version !== "number" || b.version > BACKUP_VERSION) throw new Error("This backup was made by a newer version of KuroToon.");
  const local: Record<string, string> = {};
  for (const [k, v] of Object.entries(b.local ?? {})) if (OWNED(k) && typeof v === "string") local[k] = v;
  const e = (b.engine ?? {}) as Partial<Backup["engine"]>;
  return {
    app: "kurotoon",
    version: b.version,
    createdAt: String(b.createdAt ?? ""),
    local,
    engine: {
      repos: (e.repos ?? []).filter((u): u is string => typeof u === "string" && /^https?:\/\//.test(u)),
      installed: (e.installed ?? []).filter((x) => x && typeof x.id === "string" && typeof x.repoUrl === "string"),
      prefs: (e.prefs ?? {}) as EnginePrefs,
      settings: (e.settings ?? {}) as Backup["engine"]["settings"],
    },
  };
}

export type RestoreReport = { keys: number; repos: number; sources: number; failed: string[] };

export async function restoreBackup(b: Backup, onStep?: (msg: string) => void): Promise<RestoreReport> {
  const report: RestoreReport = { keys: 0, repos: 0, sources: 0, failed: [] };
  for (const [k, v] of Object.entries(b.local)) {
    localStorage.setItem(k, v);
    report.keys++;
  }
  window.dispatchEvent(new Event("kt-local-change"));
  await bootEngine();
  if (b.engine.prefs && typeof b.engine.prefs === "object") await setPrefs(b.engine.prefs);
  for (const url of b.engine.repos) {
    onStep?.(`Adding repository ${new URL(url).hostname}`);
    try {
      await addRepository(url);
      report.repos++;
    } catch {
      report.failed.push(url);
    }
  }
  const s = getEngineState();
  for (const want of b.engine.installed) {
    if (s.installed.some((x) => x.id === want.id)) continue;
    const repo = s.repos.find((r) => r.url === want.repoUrl) ?? s.repos.find((r) => r.sources.some((e) => e.id === want.id));
    const entry = repo?.sources.find((e) => e.id === want.id);
    if (!repo || !entry) {
      report.failed.push(want.id);
      continue;
    }
    onStep?.(`Installing ${entry.name}`);
    try {
      await installSource(repo, entry);
      if (!want.enabled) await setSourceEnabled(want.id, false);
      report.sources++;
    } catch {
      report.failed.push(want.id);
    }
  }
  for (const [id, v] of Object.entries(b.engine.settings)) await writeSourceSettings(id, v);
  return report;
}
