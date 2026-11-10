import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  Outlet, Link, createRootRouteWithContext, useRouter, HeadContent, Scripts, useLocation,
} from "@tanstack/react-router";
import appCss from "../styles.css?url";
import { AuthProvider } from "@/lib/auth";
import { TopBar } from "@/components/TopBar";
import { BottomNav } from "@/components/BottomNav";
import { Toaster } from "@/components/ui/sonner";
import { useEffect } from "react";
import type { ErrorComponentProps } from "@tanstack/react-router";
import { registerPWA } from "@/lib/pwa";
import { useDailyLoginClaim } from "@/lib/kuro";
import { TouchBurst } from "@/components/TouchBurst";

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
      { title: "KuroToon — Manga, manhwa & creator discovery" },
      { name: "description", content: "A next-generation manga, manhwa, webcomic, and creator discovery platform. Read, follow and explore in a sleek dark experience." },
      { property: "og:title", content: "KuroToon — Manga, manhwa & creator discovery" },
      { property: "og:description", content: "A next-generation manga, manhwa, webcomic, and creator discovery platform." },
      { property: "og:site_name", content: "KuroToon" },
      { property: "og:type", content: "website" },
      { name: "twitter:title", content: "KuroToon — Manga, manhwa & creator discovery" },
      { name: "twitter:description", content: "A next-generation manga, manhwa, webcomic, and creator discovery platform." },
      { property: "og:image", content: "https://pub-bb2e103a32db4e198524a2e9ed8f35b4.r2.dev/e9db122d-b027-4482-bdaa-f28b36592594/id-preview-620161ad--eef62591-ae69-4f91-bf3b-04bb3e69bd43.lovable.app-1778545002314.png" },
      { name: "twitter:image", content: "https://pub-bb2e103a32db4e198524a2e9ed8f35b4.r2.dev/e9db122d-b027-4482-bdaa-f28b36592594/id-preview-620161ad--eef62591-ae69-4f91-bf3b-04bb3e69bd43.lovable.app-1778545002314.png" },
      { name: "twitter:card", content: "summary_large_image" },
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
  useDailyLoginClaim();
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
      <AuthProvider>
        <Layout />
        <TouchBurst />
        <Toaster theme="dark" position="top-center" />
      </AuthProvider>
    </QueryClientProvider>
  );
}
