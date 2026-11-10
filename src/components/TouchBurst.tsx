import { useEffect, useRef } from "react";

/**
 * Global premium tap/click burst:
 *  - Purple mystical flame aura + electric-blue lightning sparks.
 *  - Spawns at pointer location on pointerdown for interactive elements.
 *  - Pointer-events: none on overlay so it never blocks clicks.
 *  - Subtle haptic feedback on supported devices.
 */
export function TouchBurst() {
  const layerRef = useRef<HTMLDivElement | null>(null);
  const lastAt = useRef(0);

  useEffect(() => {
    const layer = layerRef.current;
    if (!layer) return;

    const isInteractive = (el: EventTarget | null): boolean => {
      if (!(el instanceof Element)) return false;
      // Don't fire inside the reader image surface to keep tap-zones snappy.
      if (el.closest("[data-no-burst], .reader-surface")) return false;
      return !!el.closest(
        'button, a, [role="button"], [role="tab"], [role="menuitem"], [role="option"], input[type="button"], input[type="submit"], label, summary, [data-burst]',
      );
    };

    const haptic = () => {
      try {
        if ("vibrate" in navigator) navigator.vibrate(8);
      } catch {
        /* noop */
      }
    };

    const spawn = (x: number, y: number) => {
      const now = performance.now();
      if (now - lastAt.current < 40) return; // coalesce rapid fire
      lastAt.current = now;

      const burst = document.createElement("span");
      burst.className = "kt-burst";
      burst.style.left = `${x}px`;
      burst.style.top = `${y}px`;

      // Aura (purple flame) + core highlight
      burst.innerHTML = `
        <span class="kt-burst__aura"></span>
        <span class="kt-burst__core"></span>
        <span class="kt-burst__ring"></span>
        <span class="kt-burst__spark" style="--a:12deg"></span>
        <span class="kt-burst__spark" style="--a:78deg"></span>
        <span class="kt-burst__spark" style="--a:152deg"></span>
        <span class="kt-burst__spark" style="--a:228deg"></span>
        <span class="kt-burst__spark" style="--a:304deg"></span>
      `;
      layer.appendChild(burst);
      window.setTimeout(() => burst.remove(), 700);
    };

    const onPointerDown = (e: PointerEvent) => {
      if (!isInteractive(e.target)) return;
      spawn(e.clientX, e.clientY);
      if (e.pointerType === "touch") haptic();
    };

    window.addEventListener("pointerdown", onPointerDown, { passive: true });
    return () => window.removeEventListener("pointerdown", onPointerDown);
  }, []);

  return (
    <div
      ref={layerRef}
      aria-hidden
      className="pointer-events-none fixed inset-0 z-[9999] overflow-hidden"
    />
  );
}
