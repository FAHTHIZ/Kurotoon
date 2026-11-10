/** URL-safe, reversible encoding for source-specific keys (which may contain "/", "?", etc.). */
export function encodeKey(key: string): string {
  const bytes = new TextEncoder().encode(key);
  let bin = "";
  bytes.forEach((b) => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function decodeKey(encoded: string): string {
  try {
    const b64 = encoded.replace(/-/g, "+").replace(/_/g, "/");
    const bin = atob(b64 + "===".slice((b64.length + 3) % 4));
    return new TextDecoder().decode(Uint8Array.from(bin, (c) => c.charCodeAt(0)));
  } catch {
    return encoded;
  }
}

/** Stable library id for a series across the app. */
export function seriesId(sourceId: string, key: string): string {
  return `${sourceId}::${key}`;
}
