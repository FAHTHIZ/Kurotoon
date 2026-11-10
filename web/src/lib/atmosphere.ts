// Atmosphere engine: derives a listening/reading mood profile from *runtime
// metadata only* (title, genres, tags, synopsis, chapter title).
// Nothing is stored — no pages, no images, no scraped content.

export type AtmosphereProfile = {
  primary: string;
  secondary: string;
  intensity: number; // 0..100
  mood: string;
  soundtrack: SoundtrackProfile;
};

export type SoundtrackProfile =
  | "Dark Fantasy Orchestra"
  | "Epic Battle Orchestra"
  | "Mystical Ambient"
  | "Heroic Orchestra"
  | "Cyberpunk Synthwave"
  | "Cozy Lo-Fi";

export type AtmosphereInput = {
  title?: string;
  genres?: string[];
  description?: string;
  chapterTitle?: string | null;
};

type Rule = {
  soundtrack: SoundtrackProfile;
  primary: string;
  secondary: string;
  mood: string;
  weight: number;
  keys: string[];
};

const RULES: Rule[] = [
  {
    soundtrack: "Dark Fantasy Orchestra",
    primary: "Ominous",
    secondary: "Gothic",
    mood: "Tense & brooding",
    weight: 3,
    keys: ["horror", "dark", "demon", "vampire", "tragedy", "gore", "psychological", "curse", "death", "undead"],
  },
  {
    soundtrack: "Epic Battle Orchestra",
    primary: "Explosive",
    secondary: "Relentless",
    mood: "Adrenaline",
    weight: 3,
    keys: ["action", "martial arts", "battle", "tournament", "war", "military", "shounen", "superhero", "monsters"],
  },
  {
    soundtrack: "Mystical Ambient",
    primary: "Ethereal",
    secondary: "Mysterious",
    mood: "Curious & drifting",
    weight: 2,
    keys: ["mystery", "supernatural", "magic", "fantasy", "isekai", "philosophical", "reincarnation", "ghosts", "mythology"],
  },
  {
    soundtrack: "Heroic Orchestra",
    primary: "Uplifting",
    secondary: "Determined",
    mood: "Rising resolve",
    weight: 2,
    keys: ["adventure", "sports", "drama", "historical", "samurai", "survival", "villainess", "revenge"],
  },
  {
    soundtrack: "Cyberpunk Synthwave",
    primary: "Neon",
    secondary: "Restless",
    mood: "Wired & alert",
    weight: 3,
    keys: ["sci-fi", "mecha", "cyberpunk", "video games", "virtual reality", "aliens", "time travel", "crime", "delinquents"],
  },
  {
    soundtrack: "Cozy Lo-Fi",
    primary: "Warm",
    secondary: "Gentle",
    mood: "Calm & unhurried",
    weight: 2,
    keys: ["romance", "slice of life", "comedy", "school life", "cooking", "music", "office workers", "wholesome", "iyashikei"],
  },
];

const INTENSE_KEYS = ["action", "gore", "horror", "war", "battle", "military", "thriller", "survival", "psychological"];
const CALM_KEYS = ["slice of life", "romance", "comedy", "cooking", "iyashikei", "school life", "music"];

export function analyzeAtmosphere(input: AtmosphereInput): AtmosphereProfile {
  const haystack = [
    input.title ?? "",
    input.chapterTitle ?? "",
    (input.genres ?? []).join(" "),
    (input.description ?? "").slice(0, 900),
  ]
    .join(" ")
    .toLowerCase();

  let winner = RULES[5]!; // Cozy Lo-Fi default
  let bestScore = -1;
  const scores = new Map<Rule, number>();

  for (const rule of RULES) {
    let score = 0;
    for (const k of rule.keys) if (haystack.includes(k)) score += rule.weight;
    scores.set(rule, score);
    if (score > bestScore) {
      bestScore = score;
      winner = rule;
    }
  }

  // Secondary flavour comes from the runner-up rule when it scored at all.
  let runnerUp: Rule | null = null;
  for (const [rule, score] of scores) {
    if (rule === winner || score <= 0) continue;
    if (!runnerUp || score > (scores.get(runnerUp) ?? 0)) runnerUp = rule;
  }

  let intensity = 42;
  for (const k of INTENSE_KEYS) if (haystack.includes(k)) intensity += 7;
  for (const k of CALM_KEYS) if (haystack.includes(k)) intensity -= 5;
  intensity = Math.max(8, Math.min(98, intensity));

  return {
    primary: winner.primary,
    secondary: runnerUp?.primary ?? winner.secondary,
    intensity,
    mood: winner.mood,
    soundtrack: winner.soundtrack,
  };
}

/** Playback channels the player can render for a given profile. */
export const CHANNELS = ["recommended", "atmospheric", "intense", "focus"] as const;
export type Channel = (typeof CHANNELS)[number];

export const CHANNEL_LABEL: Record<Channel, string> = {
  recommended: "Recommended",
  atmospheric: "Atmospheric",
  intense: "Intense",
  focus: "Focus",
};
