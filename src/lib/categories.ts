/**
 * Library categories, sorting and filters — modelled on Mihon's library.
 * Categories are user-defined labels; a series can be in several. Series in
 * no category show under "Default". Everything lives in localStorage.
 */
import type { SeriesCard } from "@/engine/types";
import { getProgress, getReadChapters, idOf } from "./local-library";

const CATS = "kt.v2.categories";
const MEMBERS = "kt.v2.series-categories";
const VIEW = "kt.v2.library-view";
const KNOWN = "kt.v2.known-chapters";

function read<T>(k: string, fb: T): T {
  if (typeof window === "undefined") return fb;
  try {
    const v = localStorage.getItem(k);
    return v ? (JSON.parse(v) as T) : fb;
  } catch {
    return fb;
  }
}
function write(k: string, v: unknown) {
  try {
    localStorage.setItem(k, JSON.stringify(v));
    window.dispatchEvent(new Event("kt-local-change"));
  } catch {
    /* full */
  }
}

export function getCategories(): string[] {
  return read<string[]>(CATS, []);
}

export function addCategory(name: string): boolean {
  const n = name.trim().slice(0, 40);
  if (!n || n.toLowerCase() === "default") return false;
  const list = getCategories();
  if (list.some((c) => c.toLowerCase() === n.toLowerCase())) return false;
  write(CATS, [...list, n]);
  return true;
}

export function renameCategory(from: string, to: string): boolean {
  const n = to.trim().slice(0, 40);
  const list = getCategories();
  if (!n || n.toLowerCase() === "default" || list.some((c) => c !== from && c.toLowerCase() === n.toLowerCase())) return false;
  write(CATS, list.map((c) => (c === from ? n : c)));
  const m = read<Record<string, string[]>>(MEMBERS, {});
  for (const id of Object.keys(m)) m[id] = m[id]!.map((c) => (c === from ? n : c));
  write(MEMBERS, m);
  return true;
}

export function deleteCategory(name: string) {
  write(CATS, getCategories().filter((c) => c !== name));
  const m = read<Record<string, string[]>>(MEMBERS, {});
  for (const id of Object.keys(m)) m[id] = m[id]!.filter((c) => c !== name);
  write(MEMBERS, m);
}

export function moveCategory(name: string, dir: -1 | 1) {
  const list = getCategories();
  const i = list.indexOf(name);
  const j = i + dir;
  if (i < 0 || j < 0 || j >= list.length) return;
  [list[i], list[j]] = [list[j]!, list[i]!];
  write(CATS, list);
}

export function getSeriesCategories(id: string): string[] {
  return read<Record<string, string[]>>(MEMBERS, {})[id] ?? [];
}

export function setSeriesCategories(id: string, cats: string[]) {
  const m = read<Record<string, string[]>>(MEMBERS, {});
  if (cats.length) m[id] = [...new Set(cats)];
  else delete m[id];
  write(MEMBERS, m);
}

/** Unread = chapters seen by the updates checker that are not marked read. */
export function unreadCount(id: string): number | null {
  const known = read<Record<string, string[]>>(KNOWN, {})[id];
  if (!known) return null;
  const readSet = new Set(getReadChapters(id));
  return known.filter((k) => !readSet.has(k)).length;
}

export type LibrarySort = "added" | "title" | "lastRead" | "unread";
export type LibraryFilter = { unread: boolean; started: boolean; source: string | null };
export type LibraryView = { sort: LibrarySort; desc: boolean; filter: LibraryFilter; category: string };

export const DEFAULT_VIEW: LibraryView = {
  sort: "added",
  desc: true,
  filter: { unread: false, started: false, source: null },
  category: "__all",
};

export function getLibraryView(): LibraryView {
  const v = read<Partial<LibraryView>>(VIEW, {});
  return { ...DEFAULT_VIEW, ...v, filter: { ...DEFAULT_VIEW.filter, ...(v.filter ?? {}) } };
}

export function setLibraryView(v: LibraryView) {
  write(VIEW, v);
}

/**
 * Applies category, filters and sort. `items` must be in date-added order,
 * newest first (the order bookmarks are stored in).
 */
export function arrangeLibrary(items: SeriesCard[], view: LibraryView): SeriesCard[] {
  const members = read<Record<string, string[]>>(MEMBERS, {});
  let list = items.map((s, i) => ({ s, id: idOf(s), added: items.length - i }));
  if (view.category === "__default") list = list.filter((x) => !(members[x.id]?.length));
  else if (view.category !== "__all") list = list.filter((x) => members[x.id]?.includes(view.category));
  if (view.filter.source) list = list.filter((x) => x.s.sourceId === view.filter.source);
  if (view.filter.unread) list = list.filter((x) => (unreadCount(x.id) ?? 0) > 0);
  if (view.filter.started) list = list.filter((x) => !!getProgress(x.id));
  const key = (x: (typeof list)[number]): number | string => {
    switch (view.sort) {
      case "title":
        return x.s.title.toLowerCase();
      case "lastRead":
        return getProgress(x.id)?.at ?? 0;
      case "unread":
        return unreadCount(x.id) ?? -1;
      default:
        return x.added;
    }
  };
  const sorted = [...list].sort((a, b) => {
    const ka = key(a);
    const kb = key(b);
    const c = typeof ka === "string" ? ka.localeCompare(kb as string) : (ka as number) - (kb as number);
    return view.desc ? -c : c;
  });
  return sorted.map((x) => x.s);
}
