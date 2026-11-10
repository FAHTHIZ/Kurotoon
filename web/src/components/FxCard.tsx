import { Sparkles, Vibrate, Volume2, VolumeX } from "lucide-react";
import { setFxPrefs, sfx, useFxPrefs, burst, haptic } from "@/lib/fx";

/** Profile card: sound design, particles and haptics. */
export function FxCard() {
  const fx = useFxPrefs();
  const row = (on: boolean, label: string, hint: string, Icon: typeof Sparkles, onToggle: () => void) => (
    <button
      type="button"
      role="switch"
      aria-checked={on}
      onClick={onToggle}
      className="flex w-full items-center gap-3 rounded-xl px-1 py-2 text-left"
    >
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/12 ring-1 ring-primary/20">
        <Icon className="h-4 w-4 text-primary" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-bold">{label}</span>
        <span className="block text-[11px] text-muted-foreground">{hint}</span>
      </span>
      <span className={`relative h-6 w-10 rounded-full transition-colors ${on ? "bg-primary" : "bg-white/15"}`}>
        <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all ${on ? "left-[18px]" : "left-0.5"}`} />
      </span>
    </button>
  );
  return (
    <div className="kt-card rounded-2xl p-4">
      <p className="mb-2 text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Effects</p>
      {row(fx.sound, "Sound design", "Taps, page turns, rewards — all synthesised", fx.sound ? Volume2 : VolumeX, () => {
        setFxPrefs({ sound: !fx.sound });
        if (!fx.sound) sfx("toggleOn");
      })}
      {fx.sound && (
        <label className="flex items-center gap-3 px-1 pb-2 pl-[52px]">
          <input
            type="range"
            min={0}
            max={1}
            step={0.05}
            value={fx.volume}
            onChange={(e) => setFxPrefs({ volume: Number(e.target.value) })}
            onPointerUp={() => sfx("success")}
            className="w-full accent-[color:var(--primary)]"
            aria-label="Effects volume"
          />
          <span className="w-8 text-right text-[11px] tabular-nums text-muted-foreground">{Math.round(fx.volume * 100)}</span>
        </label>
      )}
      {row(fx.particles, "Particles", "Ambient embers, sparks and celebrations", Sparkles, () => {
        setFxPrefs({ particles: !fx.particles });
        sfx(fx.particles ? "toggleOff" : "toggleOn");
        if (!fx.particles) setTimeout(() => burst({ x: window.innerWidth / 2, y: window.innerHeight / 2, count: 24 }), 60);
      })}
      {row(fx.haptics, "Haptics", "Subtle vibration on taps and page turns", Vibrate, () => {
        setFxPrefs({ haptics: !fx.haptics });
        haptic("medium");
      })}
    </div>
  );
}
