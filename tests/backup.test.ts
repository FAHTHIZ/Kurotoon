import { describe, expect, test } from "bun:test";
import { BACKUP_VERSION, backupFileName, parseBackup } from "@/lib/backup";

describe("parseBackup", () => {
  test("accepts a KuroToon backup and filters foreign keys", () => {
    const b = parseBackup(JSON.stringify({
      app: "kurotoon", version: 1, createdAt: "x",
      local: { "kt.v2.bookmarks": "[]", "sb-auth-token": "secret", "kt.v2.rewards": 5 },
      engine: { repos: ["https://a.dev/index.min.json", "javascript:1"], installed: [{ id: "en.a", repoUrl: "https://a.dev/index.min.json", enabled: true }, { bad: 1 }], prefs: { showAdult: false }, settings: {} },
    }));
    expect(Object.keys(b.local)).toEqual(["kt.v2.bookmarks"]);
    expect(b.engine.repos).toEqual(["https://a.dev/index.min.json"]);
    expect(b.engine.installed.length).toBe(1);
  });
  test("rejects other files", () => {
    expect(() => parseBackup("{")).toThrow(/JSON/);
    expect(() => parseBackup(JSON.stringify({ app: "mihon" }))).toThrow(/not a KuroToon/);
    expect(() => parseBackup(JSON.stringify({ app: "kurotoon", version: BACKUP_VERSION + 1 }))).toThrow(/newer/);
  });
  test("file name is filesystem-safe", () => {
    expect(backupFileName(new Date("2026-11-10T08:30:00Z"))).toBe("kurotoon_2026-11-10-08-30.json");
  });
});
