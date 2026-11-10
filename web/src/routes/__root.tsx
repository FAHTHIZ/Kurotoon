import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  Outlet, Link, createRootRouteWithContext, useRouter, HeadContent, Scripts, useLocation,
} from "@tanstack/react-router";
import appCss from "../styles.css?url";
import { TopBar } from "@/components/TopBar";
import { BottomNav } from "@/components/BottomNav";
import { Toaster } from "@/components/ui/sonner";
import { useEffect } from "react";
import type { ErrorComponentProps } from "@tanstack/react-router";
import { registerPWA } from "@/lib/pwa";
import { claimDailyOpen, setRewardNotifier } from "@/lib/rewards";
import { toast } from "sonner";
import { TouchBurst } from "@/components/TouchBurst";
import { ParticleField } from "@/components/ParticleField";
import { installToastSounds } from "@/lib/fx";

function NotFoundComponent() {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="max-w-md text-center">
        <h1 className="text-7xl font-bold text-primary">404</h1>
        <p className="mt-2 text-sm text-muted-foreground">This page doesn't exist.</p>
        <Link to="/" className="mt-6 inline-flex rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">Go home</Link>
      </div>
    </div>
  );
}

function ErrorComponent({ error, reset }: ErrorComponentProps) {
  const router = useRouter();
  const message = error instanceof Error ? error.message : String(error);
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="max-w-md text-center">
        <h1 className="text-xl font-semibold">Something broke</h1>
        <p className="mt-2 text-sm text-muted-foreground">{message}</p>
        <button onClick={() => { router.invalidate(); reset(); }} className="mt-6 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">Try again</button>
      </div>
    </div>
  );
}

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1, viewport-fit=cover" },
      { name: "theme-color", content: "#0b0b0b" },
      { title: "KuroToon — manga, manhwa & webtoon reader" },
      { name: "description", content: "An extension-based manga, manhwa and webtoon reader. Add source repositories, search every source at once and read on your phone. Everything stays on your device." },
      { property: "og:title", content: "KuroToon — manga, manhwa & webtoon reader" },
      { property: "og:description", content: "Extension-based manga reader with Universal Search. Everything stays on your device." },
      { property: "og:site_name", content: "KuroToon" },
      { property: "og:type", content: "website" },
      { name: "twitter:title", content: "KuroToon — manga, manhwa & webtoon reader" },
      { name: "twitter:description", content: "Extension-based manga reader with Universal Search. Everything stays on your device." },
      { name: "twitter:card", content: "summary" },
    ],
    links: [
      { rel: "stylesheet", href: appCss },
      { rel: "manifest", href: "/manifest.webmanifest" },
      { rel: "icon", href: "/icon-192.png", type: "image/png" },
      { rel: "apple-touch-icon", href: "/icon-192.png" },
    ],
  }),
  shellComponent: RootShell,
  component: RootComponent,
  notFoundComponent: NotFoundComponent,
  errorComponent: ErrorComponent,
});

function RootShell({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark">
      <head><HeadContent /></head>
      <body className="dark">
        {children}
        <Scripts />
      </body>
    </html>
  );
}

function Layout() {
  const { pathname } = useLocation();
  const isReader = pathname.startsWith("/read/");
  useEffect(() => {
    installToastSounds();
    setRewardNotifier((title, description) => toast.success(title, { description }));
    claimDailyOpen();
  }, []);
  if (isReader) return <Outlet />;
  return (
    <div className="min-h-screen bg-background pb-20 md:pb-0">
      <TopBar />
      <main className="mx-auto max-w-6xl px-3 py-4 md:px-6">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  );
}

function RootComponent() {
  const { queryClient } = Route.useRouteContext();
  useEffect(() => { registerPWA(); }, []);
  return (
    <QueryClientProvider client={queryClient}>
        <Layout />
        <ParticleField />
        <TouchBurst />
        <Toaster theme="dark" position="top-center" />
    </QueryClientProvider>
  );
}
