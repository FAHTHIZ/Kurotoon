/**
 * Source filters, as Aidoku and Mihon show them on a source's browse screen.
 *
 * Sources declare filters in filters.json (the manifest; string types such as
 * "sort" or "multi-select") or return them from get_filters (numeric
 * FilterType). Both normalise into UiFilter, the form state stays a plain
 * object, and toFilterValues turns it back into the FilterValue[] the runtime
 * encodes for the WASM source. Only values that differ from the defaults are
 * sent, matching Aidoku's behaviour.
 */
import type { Filter, FilterValue } from "./types";

export type UiFilter =
  | { kind: "text"; id: string; title: string; placeholder: string; as: 0 | 1 }
  | {
      kind: "sort";
      id: string;
      title: string;
      options: string[];
      canAscend: boolean;
      def: { index: number; ascending: boolean };
    }
  | {
      kind: "select";
      id: string;
      title: string;
      options: string[];
      ids: string[] | null;
      def: number;
      /** Options hidden by the content safeguard (state stays index-based). */
      hidden?: boolean[];
    }
  | {
      kind: "multi";
      id: string;
      title: string;
      options: string[];
      ids: string[] | null;
      canExclude: boolean;
    }
  | { kind: "check"; id: string; title: string; def: boolean }
  | { kind: "group"; id: string; title: string; filters: UiFilter[] };

export type SortValue = { index: number; ascending: boolean };
export type MultiValue = { inc: string[]; exc: string[] };
export type FilterState = Record<string, string | number | boolean | SortValue | MultiValue>;

type Info = Record<string, unknown>;
const str = (v: unknown) => (v == null ? "" : String(v));
const strs = (v: unknown) => (Array.isArray(v) ? v.map(str) : []);

function idOf(f: Info): string {
  return str(f.id ?? f.name ?? f.title);
}

/** Normalise manifest (string `type`) or runtime (numeric `type`) filters. */
export function normalizeFilters(list: unknown): UiFilter[] {
  if (!Array.isArray(list)) return [];
  const out: UiFilter[] = [];
  for (const raw of list) {
    if (!raw || typeof raw !== "object") continue;
    const f = raw as Info;
    const n = normalizeOne(f);
    if (n) out.push(n);
  }
  return out;
}

function normalizeOne(f: Info): UiFilter | null {
  const t = f.type;
  const title = str(f.title ?? f.name ?? f.id);
  const id = idOf(f);
  const options = strs(f.options);
  const ids = Array.isArray(f.ids) && f.ids.length === options.length ? strs(f.ids) : null;
  switch (t) {
    case "title":
    case 0:
      return {
        kind: "text",
        id: id || "Title",
        title: title || "Title",
        placeholder: str(f.placeholder),
        as: 0,
      };
    case "author":
    case 1:
      return {
        kind: "text",
        id: id || "Author",
        title: title || "Author",
        placeholder: str(f.placeholder),
        as: 1,
      };
    case "text":
    case 7:
      return { kind: "text", id, title, placeholder: str(f.placeholder), as: 0 };
    case "sort":
    case 3: {
      if (!options.length) return null;
      const d = (f.default ?? {}) as Info;
      return {
        kind: "sort",
        id,
        title: title || "Sort",
        options,
        canAscend: !!f.canAscend,
        def: { index: Number(d.index ?? 0) || 0, ascending: !!d.ascending },
      };
    }
    case "select":
    case 2: {
      if (!options.length) return null;
      let def = 0;
      if (typeof f.default === "number") def = f.default;
      else if (typeof f.default === "string")
        def = Math.max(0, (ids ?? options).indexOf(f.default));
      return { kind: "select", id, title, options, ids, def };
    }
    case "multi-select":
    case "genre":
    case 6:
      if (!options.length) {
        // Legacy "genre" entries are single toggles inside a group.
        return { kind: "check", id, title, def: false };
      }
      return { kind: "multi", id, title, options, ids, canExclude: !!f.canExclude };
    case "check":
    case 4:
      return { kind: "check", id, title: str(f.name ?? f.title ?? f.id), def: !!f.default };
    case "group":
    case 5: {
      const filters = normalizeFilters(f.filters);
      return filters.length ? { kind: "group", id, title, filters } : null;
    }
    default:
      return null;
  }
}

/** Runtime `Filter[]` uses the same shapes under numeric types. */
export function normalizeRuntimeFilters(list: Filter[]): UiFilter[] {
  return normalizeFilters(list as unknown[]);
}

function sameSort(a: SortValue, b: SortValue) {
  return a.index === b.index && a.ascending === b.ascending;
}

export function isDefault(f: UiFilter, v: FilterState[string] | undefined): boolean {
  if (v === undefined) return true;
  switch (f.kind) {
    case "text":
      return !String(v).trim();
    case "sort":
      return sameSort(v as SortValue, f.def);
    case "select":
      return v === f.def;
    case "multi": {
      const m = v as MultiValue;
      return !m.inc.length && !m.exc.length;
    }
    case "check":
      return v === f.def;
    case "group":
      return true;
  }
}

export function activeCount(filters: UiFilter[], state: FilterState): number {
  let n = 0;
  for (const f of filters) {
    if (f.kind === "group") n += activeCount(f.filters, state);
    else if (!isDefault(f, state[f.id]))
      n +=
        f.kind === "multi"
          ? (state[f.id] as MultiValue).inc.length + (state[f.id] as MultiValue).exc.length
          : 1;
  }
  return n;
}

export function toFilterValues(filters: UiFilter[], state: FilterState): FilterValue[] {
  const out: FilterValue[] = [];
  for (const f of filters) {
    if (f.kind === "group") {
      const children = toFilterValues(f.filters, state);
      if (children.length) out.push({ type: 5, name: f.id, filters: children } as FilterValue);
      continue;
    }
    const v = state[f.id];
    if (isDefault(f, v)) continue;
    switch (f.kind) {
      case "text":
        out.push({ type: f.as, name: f.id, value: String(v).trim() } as FilterValue);
        break;
      case "sort":
        out.push({ type: 3, name: f.id, value: v as SortValue } as FilterValue);
        break;
      case "select": {
        const i = v as number;
        out.push({ type: 2, name: f.id, value: f.ids?.[i] ?? f.options[i] ?? "" } as FilterValue);
        break;
      }
      case "multi": {
        const m = v as MultiValue;
        out.push({
          type: 6,
          name: f.id,
          value: { included: m.inc, excluded: m.exc },
        } as FilterValue);
        break;
      }
      case "check":
        out.push({ type: 4, name: f.id, value: !!v } as FilterValue);
        break;
    }
  }
  return out;
}

/** Cycle a multi-select option: none → include → exclude (if allowed) → none. */
export function cycleMulti(
  f: Extract<UiFilter, { kind: "multi" }>,
  cur: MultiValue | undefined,
  optionId: string,
): MultiValue {
  const m = { inc: [...(cur?.inc ?? [])], exc: [...(cur?.exc ?? [])] };
  if (m.inc.includes(optionId)) {
    m.inc = m.inc.filter((x) => x !== optionId);
    if (f.canExclude) m.exc.push(optionId);
  } else if (m.exc.includes(optionId)) {
    m.exc = m.exc.filter((x) => x !== optionId);
  } else {
    m.inc.push(optionId);
  }
  return m;
}

export function optionId(f: { options: string[]; ids: string[] | null }, i: number): string {
  return f.ids?.[i] ?? f.options[i] ?? String(i);
}

/** Genre labels treated as explicit; hidden from filter UIs unless adult content is enabled. */
const ADULT_RE =
  /^(adult|smut|hentai|ecchi|mature|yaoi|yuri|doujinshi|erotica|pornographic|sexual violence|18\+)$/i;

export function isAdultOption(label: string): boolean {
  return ADULT_RE.test(label.trim());
}

/**
 * Content safeguard: strip explicit genre options from multi-select filters and
 * explicit checkboxes from groups. Select options are only flagged
 * `hidden` because their state is index-based.
 */
export function hideAdultOptions(filters: UiFilter[]): UiFilter[] {
  return filters.map((f): UiFilter => {
    if (f.kind === "multi") {
      const keep = f.options.map((o, i) => (isAdultOption(o) ? -1 : i)).filter((i) => i >= 0);
      return {
        ...f,
        options: keep.map((i) => f.options[i]!),
        ids: f.ids ? keep.map((i) => f.ids![i]!) : null,
      };
    }
    if (f.kind === "select") {
      return { ...f, hidden: f.options.map((o, i) => i !== f.def && isAdultOption(o)) };
    }
    if (f.kind === "group") {
      return {
        ...f,
        filters: hideAdultOptions(
          f.filters.filter((c) => !(c.kind === "check" && isAdultOption(c.title))),
        ),
      };
    }
    return f;
  });
}
