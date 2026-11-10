// Minimal browser globals for unit tests (Bun test runner).
const store = new Map<string, string>();
const ls = {
  get length() { return store.size; },
  key: (i: number) => [...store.keys()][i] ?? null,
  getItem: (k: string) => store.get(k) ?? null,
  setItem: (k: string, v: string) => void store.set(k, String(v)),
  removeItem: (k: string) => void store.delete(k),
  clear: () => store.clear(),
};
const g = globalThis as Record<string, unknown>;
g.localStorage = ls;
if (!g.window) g.window = globalThis;
