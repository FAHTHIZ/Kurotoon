// Procedural ambient audio engine. Everything is synthesised in the browser
// with WebAudio (original tones, no recordings, nothing downloaded or stored).
import type { Channel, SoundtrackProfile } from "@/lib/atmosphere";

type Layer = { osc: OscillatorNode[]; gain: GainNode; lfo?: OscillatorNode };

type Recipe = {
  /** Base chord in Hz. */
  chord: number[];
  wave: OscillatorType;
  /** Low-pass cutoff. */
  cutoff: number;
  /** Slow amplitude drift speed. */
  drift: number;
  /** Detune spread in cents. */
  detune: number;
};

const BASE: Record<SoundtrackProfile, Recipe> = {
  "Dark Fantasy Orchestra": { chord: [65.4, 98, 155.6, 196], wave: "sawtooth", cutoff: 520, drift: 0.05, detune: 9 },
  "Epic Battle Orchestra": { chord: [82.4, 123.5, 164.8, 246.9], wave: "sawtooth", cutoff: 900, drift: 0.12, detune: 14 },
  "Mystical Ambient": { chord: [110, 164.8, 220, 329.6], wave: "sine", cutoff: 1400, drift: 0.04, detune: 6 },
  "Heroic Orchestra": { chord: [98, 146.8, 196, 293.7], wave: "triangle", cutoff: 1100, drift: 0.08, detune: 8 },
  "Cyberpunk Synthwave": { chord: [73.4, 110, 185, 220], wave: "square", cutoff: 780, drift: 0.16, detune: 18 },
  "Cozy Lo-Fi": { chord: [130.8, 196, 261.6, 329.6], wave: "sine", cutoff: 900, drift: 0.03, detune: 4 },
};

const CHANNEL_TWEAK: Record<Channel, { cutoff: number; drift: number; gain: number; shift: number }> = {
  recommended: { cutoff: 1, drift: 1, gain: 1, shift: 0 },
  atmospheric: { cutoff: 0.7, drift: 0.6, gain: 0.9, shift: -12 },
  intense: { cutoff: 1.6, drift: 1.8, gain: 1.05, shift: 0 },
  focus: { cutoff: 0.55, drift: 0.35, gain: 0.75, shift: 12 },
};

const CROSSFADE = 2.2;

export class AtmosphereEngine {
  private ctx: AudioContext | null = null;
  private master: GainNode | null = null;
  private current: Layer | null = null;
  private volume = 0.5;

  private ensure(): AudioContext {
    if (!this.ctx) {
      const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.ctx = new Ctor();
      this.master = this.ctx.createGain();
      this.master.gain.value = this.volume;
      this.master.connect(this.ctx.destination);
    }
    return this.ctx;
  }

  get running() {
    return !!this.current;
  }

  setVolume(v: number) {
    this.volume = Math.max(0, Math.min(1, v));
    if (this.master && this.ctx) {
      this.master.gain.cancelScheduledValues(this.ctx.currentTime);
      this.master.gain.linearRampToValueAtTime(this.volume, this.ctx.currentTime + 0.15);
    }
  }

  private build(profile: SoundtrackProfile, channel: Channel, intensity: number): Layer {
    const ctx = this.ensure();
    const recipe = BASE[profile];
    const tweak = CHANNEL_TWEAK[channel];

    const gain = ctx.createGain();
    gain.gain.value = 0;

    const filter = ctx.createBiquadFilter();
    filter.type = "lowpass";
    filter.frequency.value = recipe.cutoff * tweak.cutoff * (0.7 + intensity / 200);
    filter.Q.value = 0.8;
    filter.connect(gain);
    gain.connect(this.master!);

    // Slow amplitude drift keeps the pad alive without any percussion.
    const lfo = ctx.createOscillator();
    const lfoGain = ctx.createGain();
    lfo.frequency.value = recipe.drift * tweak.drift;
    lfoGain.gain.value = 180;
    lfo.connect(lfoGain);
    lfoGain.connect(filter.frequency);
    lfo.start();

    const osc: OscillatorNode[] = [];
    recipe.chord.forEach((freq, i) => {
      const o = ctx.createOscillator();
      o.type = recipe.wave;
      o.frequency.value = freq * Math.pow(2, tweak.shift / 12);
      o.detune.value = (i % 2 === 0 ? 1 : -1) * recipe.detune;
      const voice = ctx.createGain();
      voice.gain.value = (0.16 / recipe.chord.length) * tweak.gain * (i === 0 ? 1.6 : 1);
      o.connect(voice);
      voice.connect(filter);
      o.start();
      osc.push(o);
    });

    return { osc, gain, lfo };
  }

  private stopLayer(layer: Layer, at: number) {
    const ctx = this.ctx!;
    layer.gain.gain.cancelScheduledValues(ctx.currentTime);
    layer.gain.gain.setValueAtTime(layer.gain.gain.value, ctx.currentTime);
    layer.gain.gain.linearRampToValueAtTime(0, ctx.currentTime + at);
    window.setTimeout(() => {
      layer.osc.forEach((o) => { try { o.stop(); } catch { /* already stopped */ } });
      try { layer.lfo?.stop(); } catch { /* noop */ }
      layer.gain.disconnect();
    }, at * 1000 + 200);
  }

  /** Start or crossfade to a new pad. */
  async play(profile: SoundtrackProfile, channel: Channel, intensity: number) {
    const ctx = this.ensure();
    if (ctx.state === "suspended") await ctx.resume();
    const next = this.build(profile, channel, intensity);
    next.gain.gain.setValueAtTime(0, ctx.currentTime);
    next.gain.gain.linearRampToValueAtTime(1, ctx.currentTime + CROSSFADE);
    if (this.current) this.stopLayer(this.current, CROSSFADE);
    this.current = next;
  }

  stop() {
    if (this.current && this.ctx) {
      this.stopLayer(this.current, 0.9);
      this.current = null;
    }
  }
}

let singleton: AtmosphereEngine | null = null;
export function atmosphereEngine(): AtmosphereEngine {
  if (!singleton) singleton = new AtmosphereEngine();
  return singleton;
}

const VOL_KEY = "kt.atmos.volume";
const ON_KEY = "kt.atmos.channel";

export function loadVolume(): number {
  if (typeof window === "undefined") return 0.5;
  const raw = window.localStorage.getItem(VOL_KEY);
  const n = raw ? Number(raw) : NaN;
  return Number.isFinite(n) ? Math.max(0, Math.min(1, n)) : 0.5;
}

export function saveVolume(v: number) {
  if (typeof window !== "undefined") window.localStorage.setItem(VOL_KEY, String(v));
}

export function loadChannel(): Channel {
  if (typeof window === "undefined") return "recommended";
  const raw = window.localStorage.getItem(ON_KEY);
  return (raw as Channel) || "recommended";
}

export function saveChannel(c: Channel) {
  if (typeof window !== "undefined") window.localStorage.setItem(ON_KEY, c);
}
