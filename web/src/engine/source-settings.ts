/**
 * Per-source settings (Aidoku settings.json): a tree of groups and pages
 * containing selects, switches, text fields, multi-selects and steppers.
 * Normalised here; rendered by SourceSettingsSheet; values live in the
 * engine IDB under `settings:<id>` and are pushed to the running source.
 */
export type SettingItem =
  | { kind: "group"; title: string; footer: string; items: SettingItem[] }
  | { kind: "page"; key: string; title: string; items: SettingItem[] }
  | {
      kind: "select";
      key: string;
      title: string;
      values: string[];
      titles: string[];
      def: string;
      requires: Req;
    }
  | {
      kind: "multi";
      key: string;
      title: string;
      values: string[];
      titles: string[];
      def: string[];
      requires: Req;
    }
  | { kind: "switch"; key: string; title: string; subtitle: string; def: boolean; requires: Req }
  | {
      kind: "text";
      key: string;
      title: string;
      placeholder: string;
      def: string;
      secure: boolean;
      requires: Req;
    }
  | {
      kind: "stepper";
      key: string;
      title: string;
      min: number;
      max: number;
      step: number;
      def: number;
      requires: Req;
    }
  | { kind: "link"; title: string; url: string };

/** Show only when another switch is on (`requires`) or off (`requiresFalse`). */
export type Req = { key: string; value: boolean } | null;

type Raw = Record<string, unknown>;
const str = (v: unknown) => (v == null ? "" : String(v));
const strs = (v: unknown) => (Array.isArray(v) ? v.map(str) : []);

function req(r: Raw): Req {
  if (typeof r.requires === "string") return { key: r.requires, value: true };
  if (typeof r.requiresFalse === "string") return { key: r.requiresFalse, value: false };
  return null;
}

export function normalizeSettings(list: unknown): SettingItem[] {
  if (!Array.isArray(list)) return [];
  const out: SettingItem[] = [];
  for (const x of list) {
    if (!x || typeof x !== "object") continue;
    const r = x as Raw;
    const key = str(r.key);
    const title = str(r.title);
    switch (r.type) {
      case "group":
        out.push({
          kind: "group",
          title,
          footer: str(r.footer),
          items: normalizeSettings(r.items),
        });
        break;
      case "page":
        out.push({ kind: "page", key, title, items: normalizeSettings(r.items) });
        break;
      case "select":
      case "segment": {
        const values = strs(r.values ?? r.options);
        if (!key || !values.length) break;
        const titles = strs(r.titles);
        out.push({
          kind: "select",
          key,
          title,
          values,
          titles: titles.length === values.length ? titles : values,
          def:
            typeof r.default === "number"
              ? (values[r.default] ?? values[0]!)
              : str(r.default ?? values[0]),
          requires: req(r),
        });
        break;
      }
      case "multi-select":
      case "multi-single-select": {
        const values = strs(r.values ?? r.options);
        if (!key || !values.length) break;
        const titles = strs(r.titles);
        out.push({
          kind: "multi",
          key,
          title,
          values,
          titles: titles.length === values.length ? titles : values,
          def: strs(r.default),
          requires: req(r),
        });
        break;
      }
      case "switch":
      case "toggle":
        if (key)
          out.push({
            kind: "switch",
            key,
            title,
            subtitle: str(r.subtitle),
            def: !!r.default,
            requires: req(r),
          });
        break;
      case "text":
        if (key)
          out.push({
            kind: "text",
            key,
            title,
            placeholder: str(r.placeholder),
            def: str(r.default),
            secure: !!r.secure,
            requires: req(r),
          });
        break;
      case "stepper":
        if (key)
          out.push({
            kind: "stepper",
            key,
            title,
            min: Number(r.minimumValue ?? 0),
            max: Number(r.maximumValue ?? 100),
            step: Number(r.stepValue ?? 1) || 1,
            def: Number(r.default ?? r.minimumValue ?? 0),
            requires: req(r),
          });
        break;
      case "link":
        if (r.url || r.value) out.push({ kind: "link", title, url: str(r.url ?? r.value) });
        break;
      default:
        break; // login, button, editable-list: not supported on the web yet
    }
  }
  return out;
}

/** Default values for every keyed item, recursively. */
export function settingDefaults(items: SettingItem[]): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const it of items) {
    if (it.kind === "group" || it.kind === "page") Object.assign(out, settingDefaults(it.items));
    else if (it.kind !== "link") out[it.key] = it.def;
  }
  return out;
}

export function visible(it: SettingItem, values: Record<string, unknown>): boolean {
  if (!("requires" in it) || !it.requires) return true;
  return !!values[it.requires.key] === it.requires.value;
}

export function hasSettings(items: SettingItem[]): boolean {
  return items.some((it) =>
    it.kind === "group" || it.kind === "page" ? hasSettings(it.items) : true,
  );
}
