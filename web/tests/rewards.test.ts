import { beforeEach, describe, expect, test } from "bun:test";

beforeEach(() => localStorage.clear());

describe("rewards", async () => {
  const r = await import("@/lib/rewards");
  test("daily open, missions, achievements and the shop", () => {
    const toasts: string[] = [];
    r.setRewardNotifier((t) => toasts.push(t));
    r.claimDailyOpen();
    r.claimDailyOpen(); // only once per day
    expect(toasts.length).toBe(1);
    expect(r.claimMission("read_3")).toBe(false);
    for (let i = 0; i < 3; i++) r.track("chapter_read");
    expect(r.claimMission("read_3")).toBe(true);
    expect(r.claimMission("read_3")).toBe(false);
    const saved = JSON.parse(localStorage.getItem("kt.v2.rewards")!);
    expect(saved.points).toBe(10 + 25 + 30);
    expect(saved.chaptersRead).toBe(3);
    expect(r.buy("sticker_crown")).toEqual({ ok: false, error: "Not enough Kuro Points" });
    expect(r.buy("does_not_exist").ok).toBe(false);
  });
});
