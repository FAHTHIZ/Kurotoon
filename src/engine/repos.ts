/**
 * Extension repositories — the Mihon "extension repos" model, using the Aidoku
 * source-list format (`index.min.json` → `{ name, sources: [...] }`).
 *
 * KuroToon bundles no repositories and no sources. Users add the repositories
 * they trust, exactly like Mihon since v0.16.
 */
import type { ContentRating, RepoSourceEntry, Repository } from "./types";

/** Turns anything a user pastes into ordered candidate index URLs. */
export function candidateIndexUrls(raw: string): string[] {
  const out: string[] = [];
  const push = (u: string) => {
    if (!out.includes(u)) out.push(u);
  };
  let u: URL;
  try {
    u = new URL(raw.trim().replace(/^aidoku:\/\/addSourceList\?url=/i, ""));
  } catch {
    return out;
  }
  if (u.protocol !== "https:" && u.protocol !== "http:") return out;
  u.hash = "";
  const path = u.pathname.replace(/\/+$/, "");

  if (/\.json$/i.test(path)) {
    push(u.toString());
    return out;
  }

  // github.com/<owner>/<repo>[/tree/<branch>] → raw + Pages candidates
  if (u.hostname === "github.com") {
    const [owner, repo, kind, branch] = path.split("/").filter(Boolean);
    if (owner && repo) {
      const branches = [kind === "tree" ? branch : null, "gh-pages", "main", "master"].filter(
        (b, i, a): b is string => !!b && a.indexOf(b) === i,
      );
      push(`https://${owner.toLowerCase()}.github.io/${repo}/index.min.json`);
      for (const b of branches) {
        push(`https://raw.githubusercontent.com/${owner}/${repo}/${b}/index.min.json`);
        push(`https://raw.githubusercontent.com/${owner}/${repo}/${b}/index.json`);
      }
      return out;
    }
  }

  const base = `${u.origin}${path}`;
  push(`${base}/index.min.json`);
  push(`${base}/index.json`);
  return out;
}

function str(v: unknown, max = 300): string | undefined {
  return typeof v === "string" && v.trim() ? v.trim().slice(0, max) : undefined;
}

export class RepoFormatError extends Error {}

/** Resolves a possibly-relative asset path against the index URL. */
export function resolveAsset(indexUrl: string, path: string | undefined): string | null {
  if (!path) return null;
  try {
    return new URL(path, indexUrl).toString();
  } catch {
    return null;
  }
}

/**
 * Validates an untrusted repository index. Throws a RepoFormatError with a
 * human explanation when the file is not an Aidoku source list.
 */
export function parseIndex(indexUrl: string, raw: unknown): { name: string; sources: RepoSourceEntry[] } {
  // Mihon / Tachiyomi repositories are a bare array of APK entries.
  if (Array.isArray(raw)) {
    const looksLikeMihon = raw.some((e) => e && typeof e === "object" && "apk" in (e as object));
    throw new RepoFormatError(
      looksLikeMihon
        ? "This is a Mihon/Tachiyomi repository (Android APK extensions). Browsers cannot run APKs — add an Aidoku-format source list instead, or run Suwayomi and install its bridge source."
        : "This file is a list, not an Aidoku source list.",
    );
  }
  if (!raw || typeof raw !== "object") throw new RepoFormatError("Not a JSON object.");
  const o = raw as Record<string, unknown>;
  const list = Array.isArray(o["sources"]) ? (o["sources"] as unknown[]) : null;
  if (!list) throw new RepoFormatError("Missing a \"sources\" array — this is not an Aidoku source list.");

  const sources: RepoSourceEntry[] = [];
  const seen = new Set<string>();
  for (const item of list.slice(0, 2000)) {
    if (!item || typeof item !== "object") continue;
    const e = item as Record<string, unknown>;
    const id = str(e["id"], 120);
    const name = str(e["name"], 120);
    const download = str(e["downloadURL"] ?? e["file"], 500);
    if (!id || !name || !download || seen.has(id)) continue;
    const downloadURL = resolveAsset(indexUrl, download);
    if (!downloadURL) continue;
    seen.add(id);
    const langs = Array.isArray(e["languages"])
      ? (e["languages"] as unknown[]).filter((l): l is string => typeof l === "string").slice(0, 40)
      : str(e["lang"], 20)
        ? [str(e["lang"], 20)!]
        : [id.split(".")[0] ?? "multi"];
    const ratingRaw = Number(e["contentRating"] ?? (e["nsfw"] ? 2 : 0));
    const contentRating = (ratingRaw === 1 || ratingRaw === 2 ? ratingRaw : 0) as ContentRating;
    sources.push({
      id,
      name,
      version: Number(e["version"]) || 1,
      iconURL: resolveAsset(indexUrl, str(e["iconURL"] ?? e["icon"], 500)) ?? undefined,
      downloadURL,
      languages: langs,
      contentRating,
      baseURL: str(e["baseURL"], 300),
      altNames: Array.isArray(e["altNames"])
        ? (e["altNames"] as unknown[]).filter((n): n is string => typeof n === "string").slice(0, 10)
        : undefined,
      minAppVersion: str(e["minAppVersion"], 20),
    });
  }
  return { name: str(o["name"], 120) ?? new URL(indexUrl).hostname, sources };
}

/** fetch() that falls back to the same-origin relay when CORS blocks us. */
export async function fetchViaRelay(url: string, init?: RequestInit): Promise<Response> {
  try {
    const direct = await fetch(url, { ...init, mode: "cors", credentials: "omit" });
    if (direct.ok) return direct;
  } catch {
    /* CORS or network — fall through to relay */
  }
  return fetch(`/api/relay?url=${encodeURIComponent(url)}`, init);
}

export async function fetchRepository(input: string): Promise<Repository> {
  const candidates = candidateIndexUrls(input);
  if (candidates.length === 0) throw new RepoFormatError("Enter a valid http(s) URL.");
  let lastError = "Could not find an index.min.json at that address.";
  for (const url of candidates) {
    try {
      const res = await fetchViaRelay(url, { headers: { accept: "application/json" } });
      if (!res.ok) {
        lastError = `${url} answered ${res.status}`;
        continue;
      }
      const text = await res.text();
      let json: unknown;
      try {
        json = JSON.parse(text);
      } catch {
        lastError = `${url} is not JSON`;
        continue;
      }
      const parsed = parseIndex(url, json);
      return { url, name: parsed.name, sources: parsed.sources, addedAt: Date.now(), fetchedAt: Date.now(), error: null };
    } catch (e) {
      if (e instanceof RepoFormatError) throw e;
      lastError = e instanceof Error ? e.message : String(e);
    }
  }
  throw new RepoFormatError(lastError);
}
