import { defineConfig } from "@lovable.dev/vite-tanstack-config";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  tanstackStart: {
    server: { entry: "server" },
  },
  vite: {
    // The Aidoku runtime spawns its worker with new URL("./worker.js", import.meta.url);
    // pre-bundling rewrites that path and the worker 404s in dev.
    optimizeDeps: {
      exclude: ["@nemu.pm/aidoku-runtime"],
      // ...but its CommonJS dependencies still need pre-bundling.
      include: [
        "@nemu.pm/aidoku-runtime > cheerio",
        "@nemu.pm/aidoku-runtime > comlink",
        "@nemu.pm/aidoku-runtime > fflate",
        "@nemu.pm/aidoku-runtime > @variegated-coffee/serde-postcard-ts",
      ],
    },
    worker: { format: "es" },
    plugins: [
      VitePWA({
        registerType: "autoUpdate",
        injectRegister: false,
        devOptions: { enabled: false },
        manifest: false,
        workbox: {
          skipWaiting: true,
          clientsClaim: true,
          cleanupOutdatedCaches: true,
          globPatterns: ["**/*.{js,css,html,ico,png,svg,webp,woff2}"],
          // No navigateFallback: prevents the SW from ever serving a cached
          // HTML shell whose hashed asset URLs may no longer exist on the CDN.
          runtimeCaching: [
            {
              // Always go to the network for navigations when online so every
              // device receives the freshest HTML shell + current asset hashes.
              urlPattern: ({ request }) => request.mode === "navigate",
              handler: "NetworkOnly",
            },
            {
              urlPattern: ({ url }) =>
                /\.(?:png|jpg|jpeg|webp|avif|gif|svg)$/i.test(url.pathname),
              handler: "CacheFirst",
              options: {
                cacheName: "kt-images",
                expiration: { maxEntries: 500, maxAgeSeconds: 60 * 60 * 24 * 30 },
                cacheableResponse: { statuses: [0, 200] },
              },
            },
            {
              urlPattern: ({ url }) => url.origin === self.location.origin && url.pathname.startsWith("/_build/"),
              handler: "StaleWhileRevalidate",
              options: { cacheName: "kt-assets" },
            },
          ],
        },
      }),
    ],
  },
});
