import { motion, type HTMLMotionProps } from "framer-motion";
import { Link, type LinkComponentProps } from "@tanstack/react-router";
import { forwardRef } from "react";
import { cn } from "@/lib/utils";
import { spring } from "@/lib/motion";

const MotionLink = motion.create(Link);

type Tone = "primary" | "surface" | "ghost" | "glass";
type Size = "sm" | "md" | "lg" | "icon";

const tones: Record<Tone, string> = {
  primary:
    "bg-[image:var(--gradient-accent)] text-primary-foreground shadow-[var(--shadow-glow)] hover:shadow-[var(--shadow-glow-lg)]",
  surface:
    "bg-[color:var(--surface)] text-foreground ring-1 ring-border hover:bg-[color:var(--surface-2)] shadow-[var(--shadow-depth-1)]",
  ghost: "text-muted-foreground hover:text-foreground hover:bg-white/5",
  glass:
    "bg-white/[0.06] text-foreground ring-1 ring-white/10 backdrop-blur-xl hover:bg-white/[0.1] shadow-[var(--shadow-depth-1)]",
};

const sizes: Record<Size, string> = {
  sm: "h-8 px-3 text-[11px] gap-1.5",
  md: "h-10 px-4 text-sm gap-2",
  lg: "h-12 px-6 text-sm gap-2",
  icon: "h-9 w-9",
};

export function pressableClass(tone: Tone = "surface", size: Size = "md", className?: string) {
  return cn(
    "relative inline-flex select-none items-center justify-center rounded-full font-semibold",
    "transition-[box-shadow,background-color,color] duration-200 will-change-transform",
    "outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-background",
    "disabled:pointer-events-none disabled:opacity-40",
    tones[tone],
    sizes[size],
    className,
  );
}

const physics = {
  whileTap: { scale: 0.94 },
  whileHover: { scale: 1.025, y: -1 },
  transition: spring.tap,
};

export type PressableProps = HTMLMotionProps<"button"> & {
  tone?: Tone;
  size?: Size;
};

/** Button with tap compression, spring release, glow + elevation feedback. */
export const Pressable = forwardRef<HTMLButtonElement, PressableProps>(
  ({ tone = "surface", size = "md", className, ...props }, ref) => (
    <motion.button
      ref={ref}
      type={props.type ?? "button"}
      {...physics}
      {...props}
      className={pressableClass(tone, size, className)}
    />
  ),
);
Pressable.displayName = "Pressable";

type PressableLinkProps = LinkComponentProps & { tone?: Tone; size?: Size };

// framer-motion's DOM prop types clash with Link's; the runtime shape is fine.
const AnyMotionLink = MotionLink as unknown as React.ComponentType<Record<string, unknown>>;

/** Same physics, but navigates. */
export function PressableLink({ tone = "surface", size = "md", className, ...props }: PressableLinkProps) {
  return (
    <AnyMotionLink
      {...physics}
      {...(props as Record<string, unknown>)}
      className={pressableClass(tone, size, className as string)}
    />
  );
}
