import { memo, useState } from "react";
import { ImageOff } from "lucide-react";
import { SmartImage } from "./SmartImage";
import { coverFallback } from "@/engine/runtime";
import { cn } from "@/lib/utils";

/**
 * Series cover: loads directly from the source CDN (no referrer), and on
 * failure retries once through the relay with the source's Referer — many
 * CDNs block hotlinking.
 */
export const Cover = memo(function Cover({
  sourceId,
  url,
  alt,
  className,
  eager,
  sizes,
}: {
  sourceId: string;
  url: string | null;
  alt: string;
  className?: string;
  eager?: boolean;
  sizes?: string;
}) {
  const [attempt, setAttempt] = useState(0);
  const src = attempt === 0 ? url : attempt === 1 ? coverFallback(sourceId, url) : null;
  if (!src) {
    return (
      <span className="absolute inset-0 grid place-items-center bg-[color:var(--surface-2)] text-muted-foreground" aria-label={alt}>
        <ImageOff className="h-5 w-5 opacity-50" />
      </span>
    );
  }
  return (
    <SmartImage
      key={src}
      src={src}
      alt={alt}
      eager={eager}
      sizes={sizes}
      draggable={false}
      onError={() => setAttempt((a) => a + 1)}
      className={cn("h-full w-full object-cover", className)}
    />
  );
});

/** Small round source icon with a letter fallback. */
export function SourceIcon({ name, url, className }: { name: string; url: string | null; className?: string }) {
  const [broken, setBroken] = useState(false);
  return (
    <span
      className={cn(
        "relative grid shrink-0 place-items-center overflow-hidden rounded-xl bg-[color:var(--surface-2)] text-xs font-black text-primary ring-1 ring-white/[0.08]",
        className ?? "h-10 w-10",
      )}
    >
      {url && !broken ? (
        <img src={url} alt="" loading="lazy" referrerPolicy="no-referrer" onError={() => setBroken(true)} className="h-full w-full object-cover" />
      ) : (
        name.slice(0, 1).toUpperCase()
      )}
    </span>
  );
}
