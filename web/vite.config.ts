import { defineConfig } from "@lovable.dev/vite-tanstack-config";
import type { Plugin } from "vite";

// Emits /kt-precache.json listing every client asset so public/sw.js can
// cache the whole app (all lazy route chunks) on install.
function ktPrecache(): Plugin {
  return {
    name: "kt-precache",
    apply: "build",
    generateBundle(_opts, bundle) {
      if (this.environment?.name !== "client") return;
      const files = Object.keys(bundle)
        .filter((f) => /\.(?:js|css|woff2?|svg|png|webp)$/.test(f))
        .map((f) => "/" + f);
      this.emitFile({ type: "asset", fileName: "kt-precache.json", source: JSON.stringify(files) });
    },
  };
}

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
      ktPrecache(),
    ],
  },
});
