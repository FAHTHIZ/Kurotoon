import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import { Boxes } from "lucide-react";
import { readCachedSources, type CachedSource } from "@/lib/repos";

/**
 * Shows the sources installed from repositories. The built-in source is what
 * Browse reads today; installed ones are listed honestly with their state so
 * nothing pretends to be readable when it is not.
 */
export function SourceStrip() {
  const [sources, setSources] = useState<CachedSource[]>([]);

  useEffect(() => {
    const sync = () => setSources(readCachedSources());
    sync();
    window.addEventListener("kt:sources-changed", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("kt:sources-changed", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  const installed = sources.filter((s) => s.enabled);
  if (!installed.length) return null;

  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
      <span className="shrink-0 rounded-full bg-primary px-3 py-1 text-[11px] font-bold text-primary-foreground">
        MangaDex
      </span>
      {installed.slice(0, 24).map((s) => (
        <span
          key={s.id}
          title={s.supported ? "Installed source" : "Installed — not readable by KuroToon yet"}
          className={`shrink-0 rounded-full px-3 py-1 text-[11px] font-semibold ring-1 ${
            s.supported
              ? "bg-primary/12 text-primary ring-primary/25"
              : "bg-[color:var(--surface)] text-muted-foreground ring-border"
          }`}
        >
          {s.name} <span className="opacity-70">{s.lang.toUpperCase()}</span>
        </span>
      ))}
      <Link
        to="/repos"
        className="ml-auto inline-flex shrink-0 items-center gap-1 rounded-full bg-[color:var(--surface)] px-3 py-1 text-[11px] font-bold text-muted-foreground ring-1 ring-border"
      >
        <Boxes className="h-3.5 w-3.5" /> Sources
      </Link>
    </div>
  );
}
