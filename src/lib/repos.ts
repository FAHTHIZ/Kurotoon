import { supabase } from "@/integrations/supabase/client";

export type Repository = {
  id: string;
  name: string;
  url: string | null;
  kind: string;
  description: string | null;
  icon_url: string | null;
  enabled: boolean;
  builtin: boolean;
  last_synced_at: string | null;
};

export type RepoSource = {
  id: string;
  repo_id: string;
  code: string;
  name: string;
  lang: string;
  base_url: string | null;
  version: string | null;
  nsfw: boolean;
  enabled: boolean;
};

export type RepoItem = {
  id: string;
  repo_id: string;
  collection_name: string;
  series_ref: string;
  title: string;
  cover_url: string | null;
};

/** Sources KuroToon can actually read from today. */
export const SUPPORTED_SOURCES = new Set(["mangadex", "md", "mangadex-en"]);

export function isSupported(code: string) {
  return SUPPORTED_SOURCES.has(code.toLowerCase());
}

export async function loadRepoData(userId: string) {
  const [repos, sources, items] = await Promise.all([
    supabase
      .from("repositories")
      .select("id,name,url,kind,description,icon_url,enabled,builtin,last_synced_at")
      .eq("user_id", userId)
      .order("created_at", { ascending: true }),
    supabase
      .from("repo_sources")
      .select("id,repo_id,code,name,lang,base_url,version,nsfw,enabled")
      .eq("user_id", userId)
      .order("name", { ascending: true }),
    supabase
      .from("repo_items")
      .select("id,repo_id,collection_name,series_ref,title,cover_url")
      .eq("user_id", userId)
      .order("position", { ascending: true }),
  ]);
  return {
    repos: (repos.data ?? []) as Repository[],
    sources: (sources.data ?? []) as RepoSource[],
    items: (items.data ?? []) as RepoItem[],
  };
}

/* ------------------------------------------------------------------ */
/* Local cache so installed sources appear instantly across the app     */
/* ------------------------------------------------------------------ */

export type CachedSource = {
  id: string;
  code: string;
  name: string;
  lang: string;
  enabled: boolean;
  supported: boolean;
};

const CACHE_KEY = "kt.installed-sources.v1";

export function cacheSources(sources: RepoSource[]) {
  if (typeof window === "undefined") return;
  const rows: CachedSource[] = sources.map((s) => ({
    id: s.id,
    code: s.code,
    name: s.name,
    lang: s.lang,
    enabled: s.enabled,
    supported: isSupported(s.code),
  }));
  try {
    window.localStorage.setItem(CACHE_KEY, JSON.stringify(rows));
    window.dispatchEvent(new CustomEvent("kt:sources-changed"));
  } catch {
    /* storage full or unavailable — the page still works */
  }
}

export function readCachedSources(): CachedSource[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(CACHE_KEY);
    const parsed = raw ? (JSON.parse(raw) as CachedSource[]) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
