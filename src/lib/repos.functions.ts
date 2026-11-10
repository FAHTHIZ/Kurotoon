import { createServerFn } from "@tanstack/react-start";

export type ManifestSource = {
  code: string;
  name: string;
  lang: string;
  baseUrl: string | null;
  version: string | null;
  nsfw: boolean;
};

export type ManifestCollection = {
  name: string;
  items: Array<{ ref: string; title: string; cover: string | null }>;
};

export type RepoManifest = {
  name: string;
  description: string | null;
  icon: string | null;
  sources: ManifestSource[];
  collections: ManifestCollection[];
  /** The URL the index was actually read from (may differ from what was pasted). */
  resolvedUrl: string;
  /** True when a public CORS/relay proxy was needed to read the index. */
  viaProxy: boolean;
};

export type FetchAttempt = { url: string; ok: boolean; note: string };

const BLOCKED_HOST = /^(localhost$|127\.|0\.|10\.|192\.168\.|169\.254\.|172\.(1[6-9]|2\d|3[01])\.|\[?::1\]?$)/i;

const PROXY = "https://api.allorigins.win/raw?url=";
const UA = "KuroToon/1.0 (+repository-index)";
const TIMEOUT_MS = 12_000;

function str(v: unknown, max = 200): string | null {
  return typeof v === "string" && v.trim() ? v.trim().slice(0, max) : null;
}

/* ------------------------------------------------------------------ */
/* URL resolution — Mihon/Tachiyomi style                              */
/* ------------------------------------------------------------------ */

/**
 * Turns whatever the user pasted into an ordered list of candidate index URLs.
 * Accepts raw JSON links, jsDelivr, GitHub Pages, and plain GitHub repo URLs
 * (which are expanded to their common index locations/branches).
 */
export function candidateUrls(raw: string): string[] {
  const out: string[] = [];
  const push = (u: string) => {
    if (!out.includes(u)) out.push(u);
  };

  let u: URL;
  try {
    u = new URL(raw.trim());
  } catch {
    return out;
  }
  const host = u.hostname.toLowerCase();
  const path = u.pathname.replace(/\/+$/, "");
  const isJson = /\.json$/i.test(path);

  // github.com/<owner>/<repo>[/tree/<branch>] -> raw index candidates
  if (host === "github.com") {
    const parts = path.split("/").filter(Boolean);
    const owner = parts[0];
    const repo = parts[1];
    if (owner && repo) {
      const branchFromUrl = parts[2] === "tree" && parts[3] ? parts[3] : null;
      const branches = [branchFromUrl, "repo", "gh-pages", "main", "master"].filter(
        (b, i, a): b is string => !!b && a.indexOf(b) === i,
      );
      const files = ["index.min.json", "index.json"];
      for (const b of branches) {
        for (const f of files) push(`https://raw.githubusercontent.com/${owner}/${repo}/${b}/${f}`);
      }
      for (const f of files) push(`https://${owner}.github.io/${repo}/${f}`);
    }
    return out;
  }

  if (isJson) {
    push(u.toString());
    return out;
  }

  // Any other base URL: try the two conventional index filenames, then the URL itself.
  const base = `${u.origin}${path}`;
  push(`${base}/index.min.json`);
  push(`${base}/index.json`);
  push(u.toString());
  return out;
}

/* ------------------------------------------------------------------ */
/* Parsing — Tachiyomi/Mihon schema + generic JSON                     */
/* ------------------------------------------------------------------ */

function pushSource(list: ManifestSource[], e: Record<string, unknown>, inherited?: Partial<ManifestSource>) {
  const code = str(e["code"] ?? e["id"] ?? e["pkg"] ?? e["sourceId"], 80) ?? inherited?.code ?? null;
  const name = str(e["name"] ?? e["title"], 120) ?? inherited?.name ?? code;
  if (!code || !name) return;
  list.push({
    code,
    name: name.replace(/^Tachiyomi:\s*/i, ""),
    lang: str(e["lang"] ?? e["language"], 12) ?? inherited?.lang ?? "en",
    baseUrl: str(e["baseUrl"] ?? e["base_url"] ?? e["website"] ?? e["url"], 300) ?? inherited?.baseUrl ?? null,
    version: str(e["version"] ?? e["versionName"], 40) ?? inherited?.version ?? null,
    nsfw: e["nsfw"] === true || e["nsfw"] === 1 || inherited?.nsfw === true,
  });
}

function collectSources(raw: unknown): ManifestSource[] {
  const list: ManifestSource[] = [];
  const entries = Array.isArray(raw) ? raw : [];
  for (const entry of entries.slice(0, 500)) {
    const e = (entry ?? {}) as Record<string, unknown>;
    // Tachiyomi/Mihon extension object: the real sources live in `sources[]`.
    const nested = Array.isArray(e["sources"]) ? (e["sources"] as unknown[]) : null;
    const inherited: Partial<ManifestSource> = {
      name: str(e["name"], 120)?.replace(/^Tachiyomi:\s*/i, "") ?? undefined,
      lang: str(e["lang"] ?? e["language"], 12) ?? undefined,
      version: str(e["version"], 40) ?? undefined,
      nsfw: e["nsfw"] === true || e["nsfw"] === 1,
    } as Partial<ManifestSource>;
    if (nested && nested.length) {
      for (const n of nested.slice(0, 100)) pushSource(list, (n ?? {}) as Record<string, unknown>, inherited);
    } else {
      pushSource(list, e);
    }
    if (list.length >= 2000) break;
  }
  return list;
}

function parseManifest(raw: unknown, fallbackName: string, resolvedUrl: string, viaProxy: boolean): RepoManifest {
  // A bare array is either a Tachiyomi extension index or a generic source list.
  const o = (Array.isArray(raw) ? { sources: raw } : (raw ?? {})) as Record<string, unknown>;

  const rawSources = Array.isArray(o["sources"])
    ? (o["sources"] as unknown[])
    : Array.isArray(o["extensions"])
      ? (o["extensions"] as unknown[])
      : Array.isArray(o["items"]) && !o["collections"]
        ? (o["items"] as unknown[])
        : [];
  const sources = collectSources(rawSources);

  const rawCollections = Array.isArray(o["collections"]) ? (o["collections"] as unknown[]) : [];
  const collections: ManifestCollection[] = [];
  for (const c of rawCollections.slice(0, 50)) {
    const e = (c ?? {}) as Record<string, unknown>;
    const name = str(e["name"] ?? e["title"], 120) ?? "Collection";
    const rawItems = Array.isArray(e["items"]) ? (e["items"] as unknown[]) : [];
    const items: ManifestCollection["items"] = [];
    for (const i of rawItems.slice(0, 500)) {
      const it = (i ?? {}) as Record<string, unknown>;
      const ref = str(it["ref"] ?? it["id"] ?? it["mangadex"], 100);
      const title = str(it["title"] ?? it["name"], 200) ?? ref;
      if (!ref || !title) continue;
      items.push({ ref, title, cover: str(it["cover"] ?? it["cover_url"], 500) });
    }
    if (items.length) collections.push({ name, items });
  }

  return {
    name: str(o["name"], 120) ?? fallbackName,
    description: str(o["description"], 400),
    icon: str(o["icon"] ?? o["icon_url"], 500),
    sources,
    collections,
    resolvedUrl,
    viaProxy,
  };
}

/* ------------------------------------------------------------------ */
/* Fetching — direct first, public relay proxy as fallback             */
/* ------------------------------------------------------------------ */

async function getText(url: string): Promise<{ ok: boolean; text?: string; note: string }> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(url, {
      headers: { accept: "application/json,text/plain,*/*", "user-agent": UA },
      signal: controller.signal,
      redirect: "follow",
    });
    if (!res.ok) return { ok: false, note: `HTTP ${res.status}` };
    const text = (await res.text()).slice(0, 4_000_000);
    return { ok: true, text, note: "ok" };
  } catch (e) {
    return { ok: false, note: e instanceof Error && e.name === "AbortError" ? "timed out" : "unreachable" };
  } finally {
    clearTimeout(timer);
  }
}

function looksLikeIndex(raw: unknown): boolean {
  if (Array.isArray(raw)) return raw.length > 0;
  if (raw && typeof raw === "object") {
    const o = raw as Record<string, unknown>;
    return Array.isArray(o["sources"]) || Array.isArray(o["extensions"]) || Array.isArray(o["collections"]) || Array.isArray(o["items"]);
  }
  return false;
}

/**
 * Fetches and validates a repository index. Accepts any public https URL —
 * raw JSON, jsDelivr, GitHub Pages or a plain GitHub repo link — trying the
 * conventional index locations and falling back to a public relay when the
 * host refuses a direct read.
 */
export const fetchRepoManifest = createServerFn({ method: "POST" })
  .inputValidator((data: { url: string }) => {
    let raw = (data?.url ?? "").trim();
    if (!raw) throw new Error("Repository URL is required");
    if (!/^https?:\/\//i.test(raw)) raw = `https://${raw}`;
    let u: URL;
    try {
      u = new URL(raw);
    } catch {
      throw new Error("That does not look like a valid URL");
    }
    if (u.protocol === "http:") u.protocol = "https:";
    if (u.protocol !== "https:") throw new Error("Repository URLs must use https");
    if (BLOCKED_HOST.test(u.hostname)) throw new Error("That host is not allowed");
    return { url: u.toString() };
  })
  .handler(
    async ({
      data,
    }): Promise<
      { ok: true; manifest: RepoManifest; attempts: FetchAttempt[] } | { ok: false; error: string; attempts: FetchAttempt[] }
    > => {
      const candidates = candidateUrls(data.url);
      if (!candidates.length) return { ok: false, error: "Could not work out an index URL from that link", attempts: [] };

      const attempts: FetchAttempt[] = [];
      let lastError = "Could not reach that repository";

      for (const url of candidates) {
        for (const viaProxy of [false, true]) {
          const target = viaProxy ? `${PROXY}${encodeURIComponent(url)}` : url;
          const r = await getText(target);
          if (!r.ok || !r.text) {
            attempts.push({ url, ok: false, note: viaProxy ? `relay: ${r.note}` : r.note });
            lastError = `Repository ${r.note}`;
            continue;
          }
          let parsed: unknown;
          try {
            parsed = JSON.parse(r.text);
          } catch {
            attempts.push({ url, ok: false, note: viaProxy ? "relay: not JSON" : "not JSON" });
            lastError = "Repository index is not valid JSON";
            continue;
          }
          if (!looksLikeIndex(parsed)) {
            attempts.push({ url, ok: false, note: "no sources in index" });
            lastError = "No sources or collections found in that index";
            continue;
          }
          const manifest = parseManifest(parsed, new URL(url).hostname, url, viaProxy);
          if (!manifest.sources.length && !manifest.collections.length) {
            attempts.push({ url, ok: false, note: "index had no readable entries" });
            lastError = "No sources or collections found in that index";
            continue;
          }
          attempts.push({
            url,
            ok: true,
            note: `${manifest.sources.length} sources · ${manifest.collections.length} collections${viaProxy ? " (via relay)" : ""}`,
          });
          return { ok: true, manifest, attempts };
        }
      }

      return { ok: false, error: lastError, attempts };
    },
  );
