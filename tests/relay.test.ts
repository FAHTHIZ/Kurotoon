import { describe, expect, test } from "bun:test";
import { isAllowedCaller, upstreamHeaders, validateTarget } from "@/server/relay";

describe("validateTarget", () => {
  test("accepts public http(s) URLs", () => {
    expect(validateTarget("https://jumpg-webapi.tokyo-cdn.com/api")).toBeInstanceOf(URL);
  });
  test("blocks private, loopback and metadata hosts", () => {
    for (const u of ["http://127.0.0.1/", "http://localhost/", "http://10.0.0.1/", "http://192.168.1.1/", "http://169.254.169.254/", "http://[::1]/"]) {
      const r = validateTarget(u);
      expect(r instanceof URL ? 200 : r.status).toBe(403);
    }
  });
  test("rejects bad input", () => {
    for (const u of [null, "nope", "file:///etc/passwd", "https://u:p@x.com/"]) {
      const r = validateTarget(u);
      expect(r instanceof URL).toBe(false);
    }
  });
  test("blocks unusual ports", () => {
    const r = validateTarget("https://example.com:22/");
    expect(r instanceof URL ? 200 : r.status).toBe(403);
  });
});

describe("upstreamHeaders", () => {
  test("forwards source-set x-proxy-* headers, never the browser's own cookies", () => {
    const req = new Request("https://kt.app/api/relay", { headers: { "x-proxy-referer": "https://site/", cookie: "mine=1", "x-proxy-host": "evil" } });
    const h = upstreamHeaders(req, new URL("https://site/x"));
    expect(h.get("referer")).toBe("https://site/");
    expect(h.get("cookie")).toBeNull();
    expect(h.get("host")).toBeNull();
    expect(h.get("user-agent")).toBeTruthy();
  });
});

describe("isAllowedCaller", () => {
  test("rejects cross-site callers", () => {
    expect(isAllowedCaller(new Request("https://kt.app/api/relay", { headers: { "sec-fetch-site": "cross-site" } }))).toBe(false);
    expect(isAllowedCaller(new Request("https://kt.app/api/relay", { headers: { origin: "https://evil.com" } }))).toBe(false);
    expect(isAllowedCaller(new Request("https://kt.app/api/relay", { headers: { "sec-fetch-site": "same-origin" } }))).toBe(true);
  });
});
