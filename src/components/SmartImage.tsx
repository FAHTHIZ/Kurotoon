import { memo, useCallback, useState } from "react";
import { isImageCached, markImageCached, getPerfProfile } from "@/lib/perf";
import { cn } from "@/lib/utils";

type Props = {
  src: string;
  alt: string;
  className?: string;
  /** Renders a shimmer placeholder underneath until the image decodes. */
  skeletonClassName?: string;
  eager?: boolean;
  draggable?: boolean;
  onError?: React.ReactEventHandler<HTMLImageElement>;
  sizes?: string;
};

/**
 * Lazy + progressive image with a shimmer skeleton and a session decode cache.
 * Cached sources render instantly (no fade), so scrolling back never flashes.
 */
export const SmartImage = memo(function SmartImage({
  src,
  alt,
  className,
  skeletonClassName,
  eager = false,
  draggable = true,
  onError,
  sizes,
}: Props) {
  const [loaded, setLoaded] = useState(() => isImageCached(src));
  const { reducedMotion } = getPerfProfile();

  const handleLoad = useCallback(() => {
    markImageCached(src);
    setLoaded(true);
  }, [src]);

  return (
    <>
      {!loaded && <span className={cn("kt-shimmer absolute inset-0 block", skeletonClassName)} aria-hidden />}
      <img
        src={src}
        alt={alt}
        sizes={sizes}
        loading={eager ? "eager" : "lazy"}
        fetchPriority={eager ? "high" : "auto"}
        decoding="async"
        draggable={draggable}
        referrerPolicy="no-referrer"
        onLoad={handleLoad}
        onError={onError}
        className={cn(
          className,
          "kt-gpu",
          reducedMotion ? "" : "transition-opacity duration-300",
          loaded ? "opacity-100" : "opacity-0",
        )}
      />
    </>
  );
});
