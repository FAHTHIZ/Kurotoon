import { useEffect, useRef } from "react";
import { useLocation } from "@tanstack/react-router";
import { getPerfProfile } from "@/lib/perf";
import { useFxPrefs, type BurstOptions } from "@/lib/fx";

/**
 * One GPU-friendly canvas for every particle effect in the app:
 *  - ambient: slow purple/crimson embers drifting up behind the UI
 *  - bursts:  one-shot sparks/confetti fired via `burst()` / `celebrate()`
 *
 * Pointer-events none, pauses when the tab is hidden, honours reduced motion
 * and scales particle counts down on low-memory devices.
 */
type P = {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  max: number;
  size: number;
  color: string;
  g: number;
  spin: number;
  rot: number;
  shape: 0 | 1; // 0 = glow dot, 1 = confetti chip
  ambient: boolean;
};

const AMBIENT = ["#a855f7", "#7c3aed", "#ff4d4d", "#60a5fa"];
const BURST = ["#ff4d4d", "#f5c451", "#a855f7", "#60a5fa", "#ffffff"];

export function ParticleField() {
  const ref = useRef<HTMLCanvasElement | null>(null);
  const { pathname } = useLocation();
  const prefs = useFxPrefs();
  const ambientOn = useRef(true);
  ambientOn.current = prefs.particles && !pathname.startsWith("/read/");
  useEffect(() => {
    window.dispatchEvent(new Event("kt:fx-wake"));
  }, [pathname]);

  useEffect(() => {
    const canvas = ref.current;
    if (!canvas || !prefs.particles) return;
    const perf = getPerfProfile();
    if (perf.reducedMotion) return;
    const c2d = canvas.getContext("2d");
    if (!c2d) return;
    const ctx: CanvasRenderingContext2D = c2d;

    const dpr = Math.min(window.devicePixelRatio || 1, perf.lowMemory ? 1.25 : 2);
    let w = 0;
    let h = 0;
    const resize = () => {
      w = window.innerWidth;
      h = window.innerHeight;
      canvas.width = Math.floor(w * dpr);
      canvas.height = Math.floor(h * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    window.addEventListener("resize", resize);

    const ps: P[] = [];
    const maxAmbient = perf.lowMemory ? 14 : 28;
    const scale = perf.lowMemory ? 0.5 : 1;

    const spawnAmbient = () => {
      const max = 600 + Math.random() * 500;
      ps.push({
        x: Math.random() * w,
        y: h + 10,
        vx: (Math.random() - 0.5) * 0.15,
        vy: -(0.25 + Math.random() * 0.45),
        life: 0,
        max,
        size: 0.8 + Math.random() * 1.8,
        color: AMBIENT[(Math.random() * AMBIENT.length) | 0]!,
        g: 0,
        spin: 0,
        rot: Math.random() * Math.PI * 2,
        shape: 0,
        ambient: true,
      });
    };

    const onBurst = (e: Event) => {
      const o = (e as CustomEvent<BurstOptions>).detail;
      const n = Math.round((o.count ?? 18) * scale);
      const spread = o.spread ?? Math.PI * 2;
      const power = o.power ?? 4;
      const colors = o.colors ?? BURST;
      for (let i = 0; i < n; i++) {
        const a = -Math.PI / 2 + (Math.random() - 0.5) * spread;
        const v = power * (0.35 + Math.random() * 0.75);
        const confetti = (o.count ?? 18) > 30 && Math.random() < 0.55;
        ps.push({
          x: o.x,
          y: o.y,
          vx: Math.cos(a) * v,
          vy: Math.sin(a) * v,
          life: 0,
          max: 50 + Math.random() * 60,
          size: confetti ? 3 + Math.random() * 3 : 1.2 + Math.random() * 2.2,
          color: colors[(Math.random() * colors.length) | 0]!,
          g: o.gravity ?? 0.12,
          spin: (Math.random() - 0.5) * 0.4,
          rot: Math.random() * Math.PI,
          shape: confetti ? 1 : 0,
          ambient: false,
        });
      }
      if (!raf) loop();
    };
    window.addEventListener("kt:burst", onBurst);

    let raf = 0;
    let lastAmbient = 0;
    const loop = () => {
      raf = 0;
      if (document.hidden) return;
      const t = performance.now();
      ctx.clearRect(0, 0, w, h);
      const ambientCount = ps.reduce((n, p) => n + (p.ambient ? 1 : 0), 0);
      if (ambientOn.current && ambientCount < maxAmbient && t - lastAmbient > 280) {
        lastAmbient = t;
        spawnAmbient();
      }
      ctx.globalCompositeOperation = "lighter";
      for (let i = ps.length - 1; i >= 0; i--) {
        const p = ps[i]!;
        p.life++;
        p.vy += p.g;
        p.vx *= p.ambient ? 1 : 0.985;
        p.vy *= p.ambient ? 1 : 0.985;
        if (p.ambient) p.vx += Math.sin((p.life + i * 40) / 60) * 0.004;
        p.x += p.vx;
        p.y += p.vy;
        p.rot += p.spin;
        const k = p.life / p.max;
        if (k >= 1 || p.y < -20 || (!p.ambient && p.y > h + 20) || (p.ambient && !ambientOn.current && k > 0.1)) {
          ps.splice(i, 1);
          continue;
        }
        const alpha = p.ambient ? Math.sin(Math.PI * k) * 0.35 : 1 - k * k;
        ctx.globalAlpha = alpha;
        ctx.fillStyle = p.color;
        if (p.shape === 1) {
          ctx.save();
          ctx.translate(p.x, p.y);
          ctx.rotate(p.rot);
          ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
          ctx.restore();
        } else {
          const r = p.size * (p.ambient ? 3 : 2.4);
          const grd = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, r);
          grd.addColorStop(0, p.color);
          grd.addColorStop(1, "transparent");
          ctx.fillStyle = grd;
          ctx.beginPath();
          ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      ctx.globalAlpha = 1;
      if (ps.length > 0 || ambientOn.current) raf = requestAnimationFrame(loop);
    };
    const onVis = () => {
      if (!document.hidden && !raf) loop();
    };
    document.addEventListener("visibilitychange", onVis);
    window.addEventListener("kt:fx-wake", onVis);
    loop();

    return () => {
      if (raf) cancelAnimationFrame(raf);
      window.removeEventListener("resize", resize);
      window.removeEventListener("kt:burst", onBurst);
      document.removeEventListener("visibilitychange", onVis);
      window.removeEventListener("kt:fx-wake", onVis);
    };
  }, [prefs.particles]);

  if (!prefs.particles) return null;
  return <canvas ref={ref} aria-hidden className="pointer-events-none fixed inset-0 z-[70] h-full w-full" />;
}
