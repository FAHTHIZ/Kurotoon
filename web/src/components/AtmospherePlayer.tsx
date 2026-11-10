import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Music, Pause, Play, Volume2, X } from "lucide-react";
import { spring, ease } from "@/lib/motion";
import { CHANNELS, CHANNEL_LABEL, type AtmosphereProfile, type Channel } from "@/lib/atmosphere";
import { atmosphereEngine, loadChannel, loadVolume, saveChannel, saveVolume } from "@/lib/atmosphere-audio";

/**
 * Stateless atmosphere player. The mood profile is derived at runtime from the
 * series metadata and the audio is synthesised in-browser — nothing is stored.
 */
export function AtmospherePlayer({ profile, compact }: { profile: AtmosphereProfile; compact?: boolean }) {
  const [open, setOpen] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [channel, setChannel] = useState<Channel>("recommended");
  const [volume, setVolume] = useState(0.5);
  const engine = useRef<ReturnType<typeof atmosphereEngine> | null>(null);

  useEffect(() => {
    engine.current = atmosphereEngine();
    setChannel(loadChannel());
    const v = loadVolume();
    setVolume(v);
    engine.current.setVolume(v);
    return () => { engine.current?.stop(); };
  }, []);

  // Crossfade whenever the mood or channel changes while playing.
  useEffect(() => {
    if (!playing || !engine.current) return;
    void engine.current.play(profile.soundtrack, channel, profile.intensity);
  }, [playing, channel, profile.soundtrack, profile.intensity]);

  const toggle = () => {
    if (!engine.current) return;
    if (playing) {
      engine.current.stop();
      setPlaying(false);
    } else {
      setPlaying(true);
    }
  };

  const onVolume = (v: number) => {
    setVolume(v);
    saveVolume(v);
    engine.current?.setVolume(v);
  };

  const onChannel = (c: Channel) => {
    setChannel(c);
    saveChannel(c);
  };

  return (
    <div className={compact ? "" : "relative"}>
      <motion.button
        onClick={() => setOpen((o) => !o)}
        whileHover={{ scale: 1.06 }}
        whileTap={{ scale: 0.9 }}
        transition={spring.tap}
        aria-label="Atmosphere"
        className={`relative grid h-9 w-9 place-items-center rounded-full ring-1 transition-colors ${
          playing ? "bg-primary/20 text-primary ring-primary/40" : "bg-white/5 text-white/80 ring-white/10"
        }`}
      >
        <Music className="h-4 w-4" />
        {playing && (
          <motion.span
            animate={{ opacity: [0.3, 0.8, 0.3], scale: [1, 1.35, 1] }}
            transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut" }}
            className="absolute inset-0 rounded-full ring-1 ring-primary/50"
          />
        )}
      </motion.button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.96 }}
            transition={{ duration: 0.2, ease: ease.out }}
            className="absolute right-0 top-full z-50 mt-2 w-64 origin-top-right rounded-2xl border border-white/10 bg-black/90 p-3 text-white shadow-[var(--shadow-depth-3)] backdrop-blur-2xl"
          >
            <div className="flex items-start justify-between">
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-primary">Atmosphere</p>
                <p className="mt-0.5 text-sm font-bold leading-tight">{profile.soundtrack}</p>
                <p className="text-[11px] text-white/60">{profile.mood}</p>
              </div>
              <button onClick={() => setOpen(false)} className="grid h-6 w-6 place-items-center rounded-full bg-white/5" aria-label="Close">
                <X className="h-3 w-3" />
              </button>
            </div>

            <div className="mt-2 flex flex-wrap gap-1">
              {[profile.primary, profile.secondary].filter(Boolean).map((t) => (
                <span key={t} className="rounded-full bg-white/[0.07] px-2 py-0.5 text-[10px] font-medium text-white/80">
                  {t}
                </span>
              ))}
              <span className="rounded-full bg-white/[0.07] px-2 py-0.5 text-[10px] font-medium text-white/80">
                Intensity {profile.intensity}
              </span>
            </div>

            <div className="mt-3 grid grid-cols-2 gap-1.5">
              {CHANNELS.map((c) => (
                <motion.button
                  key={c}
                  whileTap={{ scale: 0.95 }}
                  transition={spring.tap}
                  onClick={() => onChannel(c)}
                  className={`rounded-lg px-2 py-1.5 text-[11px] font-semibold ring-1 transition-colors ${
                    channel === c ? "bg-primary/20 text-primary ring-primary/40" : "bg-white/[0.04] text-white/70 ring-white/10"
                  }`}
                >
                  {CHANNEL_LABEL[c]}
                </motion.button>
              ))}
            </div>

            <div className="mt-3 flex items-center gap-2">
              <motion.button
                whileTap={{ scale: 0.9 }}
                transition={spring.tap}
                onClick={toggle}
                className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-[image:var(--gradient-accent)] text-primary-foreground shadow-[var(--shadow-glow)]"
                aria-label={playing ? "Pause atmosphere" : "Play atmosphere"}
              >
                {playing ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
              </motion.button>
              <Volume2 className="h-3.5 w-3.5 text-white/50" />
              <input
                type="range"
                min={0}
                max={1}
                step={0.01}
                value={volume}
                onChange={(e) => onVolume(Number(e.target.value))}
                className="h-1 w-full accent-[color:var(--primary)]"
                aria-label="Atmosphere volume"
              />
            </div>
            <p className="mt-2 text-[10px] leading-snug text-white/40">
              Generated live in your browser from this title’s tags. Nothing is stored.
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
