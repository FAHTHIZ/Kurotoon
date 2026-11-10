/**
 * KuroToon FX — sound design + particle preferences.
 *
 * Every sound is synthesised on the fly with WebAudio (no files, nothing
 * downloaded). Sounds are short, quiet and pitched to the dark-fantasy brand:
 * soft glass taps, low "thock" toggles, a paper page-turn swish and a small
 * bell arpeggio for chapter completion / rewards.
 *
 * On Android the native shell also exposes real haptics via `KurotoonNative`.
 */
import { useSyncExternalStore } from "react";
import { toast } from "sonner";

export type FxPrefs = {
  sound: boolean;
  /** 0..1 */
  volume: number;
  particles: boolean;
  haptics: boolean;
};

const KEY = "kt:fx";
const DEFAULTS: FxPrefs = { sound: true, volume: 0.55, particles: true, haptics: true };

let prefs: FxPrefs = DEFAULTS;
let loaded = false;
const listeners = new Set<() => void>();

function load() {
  if (loaded || typeof window === "undefined") return;
  loaded = true;
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) prefs = { ...DEFAULTS, ...(JSON.parse(raw) as Partial<FxPrefs>) };
  } catch {
    /* ignore */
  }
}

export function getFxPrefs(): FxPrefs {
  load();
  return prefs;
}

export function setFxPrefs(patch: Partial<FxPrefs>) {
  load();
  prefs = { ...prefs, ...patch };
  try {
    localStorage.setItem(KEY, JSON.stringify(prefs));
  } catch {
    /* ignore */
  }
  if (master) master.gain.value = prefs.volume * 0.5;
  listeners.forEach((l) => l());
}

export function useFxPrefs(): FxPrefs {
  return useSyncExternalStore(
    (fn) => {
      listeners.add(fn);
      return () => listeners.delete(fn);
    },
    getFxPrefs,
    () => DEFAULTS,
  );
}

/* --------------------------------- audio --------------------------------- */

let ctx: AudioContext | null = null;
let master: GainNode | null = null;
let noise: AudioBuffer | null = null;

function audio(): AudioContext | null {
  if (typeof window === "undefined") return null;
  if (!getFxPrefs().sound) return null;
  try {
    if (!ctx) {
      const AC = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!AC) return null;
      ctx = new AC({ latencyHint: "interactive" });
      master = ctx.createGain();
      master.gain.value = prefs.volume * 0.5;
      const comp = ctx.createDynamicsCompressor();
      comp.threshold.value = -18;
      comp.ratio.value = 4;
      master.connect(comp).connect(ctx.destination);
    }
    if (ctx.state === "suspended") void ctx.resume();
    return ctx;
  } catch {
    return null;
  }
}

function noiseBuffer(c: AudioContext): AudioBuffer {
  if (noise) return noise;
  const len = Math.floor(c.sampleRate * 0.5);
  noise = c.createBuffer(1, len, c.sampleRate);
  const d = noise.getChannelData(0);
  for (let i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
  return noise;
}

type Tone = { f: number; t?: number; dur: number; type?: OscillatorType; gain?: number; glide?: number; detune?: number };

function tone(c: AudioContext, { f, t = 0, dur, type = "sine", gain = 0.3, glide, detune = 0 }: Tone) {
  const at = c.currentTime + t;
  const o = c.createOscillator();
  const g = c.createGain();
  o.type = type;
  o.frequency.setValueAtTime(f, at);
  o.detune.value = detune;
  if (glide) o.frequency.exponentialRampToValueAtTime(glide, at + dur);
  g.gain.setValueAtTime(0.0001, at);
  g.gain.exponentialRampToValueAtTime(gain, at + 0.006);
  g.gain.exponentialRampToValueAtTime(0.0001, at + dur);
  o.connect(g).connect(master!);
  o.start(at);
  o.stop(at + dur + 0.02);
}

function hiss(c: AudioContext, { t = 0, dur, from, to, gain = 0.2, q = 0.8 }: { t?: number; dur: number; from: number; to: number; gain?: number; q?: number }) {
  const at = c.currentTime + t;
  const s = c.createBufferSource();
  s.buffer = noiseBuffer(c);
  const bp = c.createBiquadFilter();
  bp.type = "bandpass";
  bp.Q.value = q;
  bp.frequency.setValueAtTime(from, at);
  bp.frequency.exponentialRampToValueAtTime(to, at + dur);
  const g = c.createGain();
  g.gain.setValueAtTime(0.0001, at);
  g.gain.exponentialRampToValueAtTime(gain, at + dur * 0.25);
  g.gain.exponentialRampToValueAtTime(0.0001, at + dur);
  s.connect(bp).connect(g).connect(master!);
  s.start(at);
  s.stop(at + dur + 0.02);
}

export type SoundName = "tap" | "toggleOn" | "toggleOff" | "page" | "success" | "error" | "complete" | "reward" | "swoosh" | "install";

let lastPlay = 0;

/** Plays one UI sound. Cheap; safe to call from any handler. */
export function sfx(name: SoundName) {
  const now = performance.now();
  if (name === "tap" && now - lastPlay < 35) return;
  lastPlay = now;
  const c = audio();
  if (!c || !master) return;
  const jitter = 1 + (Math.random() - 0.5) * 0.04;
  switch (name) {
    case "tap":
      tone(c, { f: 1850 * jitter, dur: 0.05, type: "triangle", gain: 0.12, glide: 1200 });
      tone(c, { f: 620 * jitter, dur: 0.07, gain: 0.08 });
      break;
    case "toggleOn":
      tone(c, { f: 520, dur: 0.08, type: "triangle", gain: 0.18 });
      tone(c, { f: 780, t: 0.05, dur: 0.12, type: "triangle", gain: 0.16 });
      break;
    case "toggleOff":
      tone(c, { f: 640, dur: 0.08, type: "triangle", gain: 0.16 });
      tone(c, { f: 420, t: 0.05, dur: 0.12, type: "triangle", gain: 0.14 });
      break;
    case "page":
      hiss(c, { dur: 0.22, from: 2400 * jitter, to: 700, gain: 0.22, q: 0.6 });
      tone(c, { f: 180, dur: 0.06, gain: 0.05 });
      break;
    case "swoosh":
      hiss(c, { dur: 0.32, from: 400, to: 3200, gain: 0.14, q: 1.2 });
      break;
    case "success":
      tone(c, { f: 659.3, dur: 0.16, type: "sine", gain: 0.2 });
      tone(c, { f: 987.8, t: 0.08, dur: 0.24, type: "sine", gain: 0.18 });
      break;
    case "install":
      tone(c, { f: 392, dur: 0.12, type: "triangle", gain: 0.16 });
      tone(c, { f: 587.3, t: 0.07, dur: 0.14, type: "triangle", gain: 0.16 });
      tone(c, { f: 784, t: 0.14, dur: 0.3, type: "sine", gain: 0.18 });
      break;
    case "error":
      tone(c, { f: 220, dur: 0.16, type: "sawtooth", gain: 0.08, glide: 160 });
      tone(c, { f: 233, t: 0.02, dur: 0.18, type: "sawtooth", gain: 0.06, glide: 150 });
      break;
    case "reward":
    case "complete": {
      const notes = name === "complete" ? [523.3, 659.3, 784, 1046.5, 1318.5] : [784, 987.8, 1318.5];
      notes.forEach((f, i) => {
        tone(c, { f, t: i * 0.07, dur: 0.9 - i * 0.08, type: "sine", gain: 0.16 });
        tone(c, { f: f * 2.01, t: i * 0.07, dur: 0.35, type: "sine", gain: 0.035 });
      });
      if (name === "complete") hiss(c, { t: 0.05, dur: 0.8, from: 6000, to: 9000, gain: 0.03, q: 2 });
      break;
    }
  }
}

/* -------------------------------- haptics -------------------------------- */

type NativeBridge = { haptic?: (kind: string) => void };

export function haptic(kind: "light" | "medium" | "heavy" | "success" = "light") {
  if (!getFxPrefs().haptics || typeof window === "undefined") return;
  const native = (window as unknown as { KurotoonNative?: NativeBridge }).KurotoonNative;
  try {
    if (native?.haptic) return native.haptic(kind);
    const ms = kind === "light" ? 6 : kind === "medium" ? 14 : kind === "heavy" ? 24 : [10, 40, 18];
    navigator.vibrate?.(ms as number);
  } catch {
    /* noop */
  }
}

/* ------------------------------- particles ------------------------------- */

export type BurstOptions = { x: number; y: number; count?: number; colors?: string[]; spread?: number; power?: number; gravity?: number };

/** Fires a one-shot particle burst (handled by <ParticleField />). */
export function burst(opts: BurstOptions) {
  if (typeof window === "undefined" || !getFxPrefs().particles) return;
  window.dispatchEvent(new CustomEvent<BurstOptions>("kt:burst", { detail: opts }));
}

/** Full-screen celebration (confetti + embers) from the centre. */
export function celebrate() {
  if (typeof window === "undefined") return;
  const x = window.innerWidth / 2;
  const y = window.innerHeight * 0.42;
  burst({ x, y, count: 90, power: 9, spread: Math.PI * 2, gravity: 0.16 });
  window.setTimeout(() => burst({ x: x * 0.5, y: y * 1.1, count: 40, power: 7, gravity: 0.18 }), 140);
  window.setTimeout(() => burst({ x: x * 1.5, y: y * 1.1, count: 40, power: 7, gravity: 0.18 }), 260);
}

/* ------------------------------ toast sounds ----------------------------- */

let toastsPatched = false;

/** Gives every success / error toast in the app a matching sound + haptic. */
export function installToastSounds() {
  if (toastsPatched || typeof window === "undefined") return;
  toastsPatched = true;
  const t = toast as unknown as Record<"success" | "error", (...a: unknown[]) => unknown>;
  const ok = t.success.bind(toast);
  const bad = t.error.bind(toast);
  t.success = (...a: unknown[]) => {
    sfx("success");
    haptic("medium");
    return ok(...a);
  };
  t.error = (...a: unknown[]) => {
    sfx("error");
    haptic("heavy");
    return bad(...a);
  };
}
