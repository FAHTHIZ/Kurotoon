import { Link, useLocation } from "@tanstack/react-router";
import { Home, LibraryBig, PawPrint, Bookmark, User } from "lucide-react";
import { motion } from "framer-motion";
import { spring } from "@/lib/motion";

const items = [
  { to: "/", icon: Home, label: "Home" },
  { to: "/browse", icon: LibraryBig, label: "Browse" },
  { to: "/rewards", icon: PawPrint, label: "Rewards" },
  { to: "/library", icon: Bookmark, label: "Library" },
  { to: "/profile", icon: User, label: "Me" },
] as const;

export function BottomNav() {
  const { pathname } = useLocation();
  return (
    <nav className="fixed bottom-0 inset-x-0 z-40 border-t border-white/[0.06] bg-background/80 backdrop-blur-2xl pb-[env(safe-area-inset-bottom)] shadow-[0_-8px_30px_-12px_oklch(0_0_0/0.8)] md:hidden">
      <ul className="grid grid-cols-5">
        {items.map(({ to, icon: Icon, label }) => {
          const active = pathname === to || (to !== "/" && pathname.startsWith(to));
          return (
            <li key={to} className="relative">
              <Link to={to} className="block">
                <motion.span
                  whileTap={{ scale: 0.88 }}
                  transition={spring.tap}
                  className={`relative flex flex-col items-center gap-1 py-2.5 text-[10px] font-semibold ${
                    active ? "text-primary" : "text-muted-foreground"
                  }`}
                >
                  {active && (
                    <>
                      <motion.span
                        layoutId="kt-nav-pill"
                        transition={spring.glide}
                        className="absolute inset-x-3 inset-y-1 -z-10 rounded-2xl bg-primary/10 ring-1 ring-primary/25"
                      />
                      <motion.span
                        layoutId="kt-nav-glow"
                        transition={spring.glide}
                        className="absolute -top-px h-[2px] w-8 rounded-full bg-primary shadow-[0_0_12px_2px_var(--primary)]"
                      />
                    </>
                  )}
                  <motion.span
                    animate={active ? { y: -1, scale: 1.12 } : { y: 0, scale: 1 }}
                    transition={spring.soft}
                    className="block"
                  >
                    <Icon
                      className="h-5 w-5"
                      style={active ? { filter: "drop-shadow(0 0 8px var(--primary))" } : undefined}
                    />
                  </motion.span>
                  <motion.span animate={{ opacity: active ? 1 : 0.75 }} transition={spring.soft}>
                    {label}
                  </motion.span>
                </motion.span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
