/**
 * Kuro Points, missions, achievements and the shop — entirely on the device.
 * There are no accounts and no server: progress is earned by reading and is
 * kept in localStorage (included in backups). Nothing can be bought with money.
 */
import { useSyncExternalStore } from "react";
import type { Rarity } from "./brand";

export type ShopKind = "sticker" | "avatar" | "frame" | "theme" | "badge" | "boost";

export type ShopItem = {
  code: string;
  name: string;
  description: string;
  kind: ShopKind;
  price: number;
  rarity: Rarity;
  emoji?: string;
};

export const SHOP: ShopItem[] = [
  { code: "sticker_sparkle", name: "Sparkle Eyes", description: "A free starter sticker.", kind: "sticker", price: 0, rarity: "common", emoji: "✨" },
  { code: "sticker_heart", name: "Heart Burst", description: "A free starter sticker.", kind: "sticker", price: 0, rarity: "common", emoji: "💖" },
  { code: "sticker_fire", name: "Fire", description: "For chapters that hit hard.", kind: "sticker", price: 0, rarity: "common", emoji: "🔥" },
  { code: "sticker_happy", name: "Happy Kuro", description: "Kuro beaming with a big chibi smile.", kind: "sticker", price: 100, rarity: "common", emoji: "😺" },
  { code: "sticker_reading", name: "Reading Kuro", description: "Hood up, nose in a chapter. The classic.", kind: "sticker", price: 150, rarity: "common", emoji: "📖" },
  { code: "sticker_sleepy", name: "Sleepy Kuro", description: "One more chapter... zzz.", kind: "sticker", price: 150, rarity: "common", emoji: "😴" },
  { code: "sticker_love", name: "Love Kuro", description: "Heart eyes for your favourite series.", kind: "sticker", price: 200, rarity: "rare", emoji: "😻" },
  { code: "sticker_shock", name: "Shock Kuro", description: "That cliffhanger reaction, bottled.", kind: "sticker", price: 200, rarity: "rare", emoji: "🙀" },
  { code: "sticker_sakura", name: "Sakura", description: "Blossom season.", kind: "sticker", price: 250, rarity: "rare", emoji: "🌸" },
  { code: "sticker_crown", name: "Crown", description: "Royalty of the reading list.", kind: "sticker", price: 400, rarity: "epic", emoji: "👑" },
  { code: "sticker_rich", name: "Rich Kuro", description: "Showered in K Coins.", kind: "sticker", price: 800, rarity: "epic", emoji: "🪙" },
  { code: "sticker_dragon", name: "Dragon", description: "Legendary reader energy.", kind: "sticker", price: 1000, rarity: "legendary", emoji: "🐉" },
  { code: "sticker_galaxy", name: "Galaxy Kuro", description: "Kuro dissolving into cosmic particles.", kind: "sticker", price: 2000, rarity: "mythic", emoji: "🌌" },
  { code: "avatar_kuro_classic", name: "Classic Kuro", description: "The original hoodie-and-paw-gloves Kuro.", kind: "avatar", price: 200, rarity: "common" },
  { code: "avatar_kuro_hood", name: "Hood-Up Kuro", description: "Night reading mode Kuro.", kind: "avatar", price: 400, rarity: "rare" },
  { code: "avatar_kuro_winter", name: "Winter Kuro", description: "Snow-dusted scarf edition.", kind: "avatar", price: 1500, rarity: "epic" },
  { code: "frame_starfield", name: "Starfield Frame", description: "Quiet drifting stars around your avatar.", kind: "frame", price: 250, rarity: "common" },
  { code: "frame_nebula", name: "Nebula Frame", description: "Slow-swirling nebula ring in cosmic purple.", kind: "frame", price: 700, rarity: "rare" },
  { code: "frame_coin", name: "Coin Holder Frame", description: "A ring of spinning K Coins.", kind: "frame", price: 1400, rarity: "epic" },
  { code: "frame_mythic", name: "Mythic Frame", description: "Shifting mythic gradient frame.", kind: "frame", price: 3000, rarity: "mythic" },
  { code: "theme_midnight", name: "Midnight Theme", description: "Deepest black surface with crimson accents.", kind: "theme", price: 300, rarity: "common" },
  { code: "theme_aurora", name: "Aurora Theme", description: "Aurora-blue accents and glow.", kind: "theme", price: 600, rarity: "rare" },
  { code: "theme_galaxy", name: "Galaxy Theme", description: "Galaxy-purple accents and glow.", kind: "theme", price: 600, rarity: "rare" },
  { code: "theme_gold", name: "Soft Gold Theme", description: "Warm gold accents for a premium mood.", kind: "theme", price: 1200, rarity: "epic" },
  { code: "badge_kuro_scout", name: "Kuro Scout", description: "For early explorers.", kind: "badge", price: 150, rarity: "common" },
  { code: "badge_night_reader", name: "Night Reader", description: "For those who read past midnight.", kind: "badge", price: 400, rarity: "rare" },
  { code: "badge_galaxy_sage", name: "Galaxy Sage", description: "Purple shimmer reserved for deep readers.", kind: "badge", price: 1800, rarity: "legendary" },
  { code: "boost_double", name: "Double Points (7 days)", description: "Double points from missions for a week.", kind: "boost", price: 1500, rarity: "epic" },
];

export type MissionCode = "read_3" | "bookmark_1" | "search_1";
export const MISSIONS: Array<{ code: MissionCode; title: string; description: string; target: number; reward: number }> = [
  { code: "read_3", title: "Read 3 chapters", description: "Finish 3 chapters today", target: 3, reward: 30 },
  { code: "bookmark_1", title: "Add to library", description: "Add 1 series to your library", target: 1, reward: 15 },
  { code: "search_1", title: "Universal Search", description: "Search across your sources once", target: 1, reward: 10 },
];

export type AchievementCode = "first_chapter" | "first_bookmark" | "first_source" | "night_reader" | "seven_day_streak" | "hundred_chapters";
export const ACHIEVEMENTS: Array<{ code: AchievementCode; title: string; description: string; icon: string; reward: number }> = [
  { code: "first_chapter", title: "First Chapter", description: "Finish your first chapter", icon: "📖", reward: 25 },
  { code: "first_bookmark", title: "Collector", description: "Add your first series to the library", icon: "🔖", reward: 25 },
  { code: "first_source", title: "Explorer", description: "Install your first source", icon: "🧭", reward: 25 },
  { code: "night_reader", title: "Night Reader", description: "Finish a chapter between midnight and 4am", icon: "🌙", reward: 50 },
  { code: "seven_day_streak", title: "7-Day Reader", description: "Open KuroToon 7 days in a row", icon: "📚", reward: 150 },
  { code: "hundred_chapters", title: "100 Chapters", description: "Finish 100 chapters", icon: "💯", reward: 300 },
];

type State = {
  points: number;
  streak: number;
  lastDay: string | null;
  day: string;
  progress: Partial<Record<MissionCode, number>>;
  claimed: MissionCode[];
  owned: string[];
  achievements: Partial<Record<AchievementCode, number>>;
  chaptersRead: number;
  boostUntil: number;
};

const KEY = "kt.v2.rewards";
const today = () => new Date().toISOString().slice(0, 10);
const EMPTY: State = {
  points: 0,
  streak: 0,
  lastDay: null,
  day: today(),
  progress: {},
  claimed: [],
  owned: ["sticker_sparkle", "sticker_heart", "sticker_fire"],
  achievements: {},
  chaptersRead: 0,
  boostUntil: 0,
};

let state: State = EMPTY;
let loaded = false;
const listeners = new Set<() => void>();

function load() {
  if (loaded || typeof window === "undefined") return;
  loaded = true;
  try {
    state = { ...EMPTY, ...(JSON.parse(localStorage.getItem(KEY) ?? "{}") as Partial<State>) };
  } catch {
    state = EMPTY;
  }
  rollDay();
}

function save(next: State) {
  state = next;
  try {
    localStorage.setItem(KEY, JSON.stringify(state));
  } catch {
    /* ignore */
  }
  listeners.forEach((l) => l());
}

function rollDay() {
  const d = today();
  if (state.day !== d) state = { ...state, day: d, progress: {}, claimed: [] };
}

export function useRewards(): State {
  return useSyncExternalStore(
    (fn) => {
      listeners.add(fn);
      return () => listeners.delete(fn);
    },
    () => {
      load();
      return state;
    },
    () => EMPTY,
  );
}

type Notify = (title: string, description?: string) => void;
let notify: Notify = () => {};
/** The UI registers a toast function so earning points can celebrate. */
export function setRewardNotifier(fn: Notify) {
  notify = fn;
}

function grant(points: number, title: string, description?: string) {
  save({ ...state, points: state.points + points });
  notify(`+${points} Kuro Points`, description ?? title);
}

function unlock(code: AchievementCode) {
  if (state.achievements[code]) return;
  const a = ACHIEVEMENTS.find((x) => x.code === code)!;
  save({ ...state, achievements: { ...state.achievements, [code]: Date.now() } });
  grant(a.reward, a.title, `Achievement unlocked: ${a.title}`);
}

/** Once per day on app open: keeps the streak and pays a small bonus. */
export function claimDailyOpen() {
  load();
  rollDay();
  const d = today();
  if (state.lastDay === d) return;
  const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);
  const streak = state.lastDay === yesterday ? state.streak + 1 : 1;
  save({ ...state, lastDay: d, streak });
  grant(Math.min(50, 5 + streak * 5), "Daily visit", `Day ${streak} streak — keep it up`);
  if (streak >= 7) unlock("seven_day_streak");
}

function bump(code: MissionCode) {
  rollDay();
  const m = MISSIONS.find((x) => x.code === code)!;
  const cur = state.progress[code] ?? 0;
  if (cur >= m.target) return;
  save({ ...state, progress: { ...state.progress, [code]: cur + 1 } });
}

export type RewardEvent = "chapter_read" | "bookmark" | "search" | "source_installed";

export function track(event: RewardEvent) {
  load();
  if (event === "chapter_read") {
    bump("read_3");
    const n = state.chaptersRead + 1;
    save({ ...state, chaptersRead: n });
    unlock("first_chapter");
    const h = new Date().getHours();
    if (h < 4) unlock("night_reader");
    if (n >= 100) unlock("hundred_chapters");
  } else if (event === "bookmark") {
    bump("bookmark_1");
    unlock("first_bookmark");
  } else if (event === "search") {
    bump("search_1");
  } else if (event === "source_installed") {
    unlock("first_source");
  }
}

export function claimMission(code: MissionCode): boolean {
  load();
  rollDay();
  const m = MISSIONS.find((x) => x.code === code)!;
  if ((state.progress[code] ?? 0) < m.target || state.claimed.includes(code)) return false;
  const reward = state.boostUntil > Date.now() ? m.reward * 2 : m.reward;
  save({ ...state, claimed: [...state.claimed, code] });
  grant(reward, m.title);
  return true;
}

export function buy(code: string): { ok: true } | { ok: false; error: string } {
  load();
  const item = SHOP.find((i) => i.code === code);
  if (!item) return { ok: false, error: "Unknown item" };
  if (state.owned.includes(code) && item.kind !== "boost") return { ok: false, error: "Already owned" };
  if (state.points < item.price) return { ok: false, error: "Not enough Kuro Points" };
  save({
    ...state,
    points: state.points - item.price,
    owned: item.kind === "boost" ? state.owned : [...state.owned, code],
    boostUntil: item.kind === "boost" ? Math.max(Date.now(), state.boostUntil) + 7 * 86_400_000 : state.boostUntil,
  });
  return { ok: true };
}
