import { useEffect, useState } from "react";
import { ChevronRight, ExternalLink, Loader2, Minus, Plus } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Sheet } from "./LibraryShelf";
import { setSourceSetting, sourceSettings } from "@/engine/runtime";
import { visible, type SettingItem } from "@/engine/source-settings";

/** Renders a source's settings.json, like Aidoku's source settings screen. */
export function SourceSettingsSheet({
  open,
  onClose,
  sourceId,
  name,
}: {
  open: boolean;
  onClose: () => void;
  sourceId: string;
  name: string;
}) {
  const qc = useQueryClient();
  const [items, setItems] = useState<SettingItem[] | null>(null);
  const [values, setValues] = useState<Record<string, unknown>>({});
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    let live = true;
    sourceSettings(sourceId)
      .then((r) => {
        if (!live) return;
        setItems(r.items);
        setValues(r.values);
      })
      .catch((e) => live && setError(e instanceof Error ? e.message : String(e)));
    return () => {
      live = false;
    };
  }, [open, sourceId]);

  const change = (key: string, value: unknown) => {
    setValues((v) => ({ ...v, [key]: value }));
    setSourceSetting(sourceId, key, value)
      .then(() => qc.invalidateQueries({ queryKey: ["src", sourceId] }))
      .catch((e) => toast.error(e instanceof Error ? e.message : "Could not save setting"));
  };

  return (
    <Sheet open={open} onClose={onClose} title={`${name} settings`}>
      {error ? (
        <p className="text-xs text-destructive">{error}</p>
      ) : !items ? (
        <Loader2 className="mx-auto my-6 h-5 w-5 animate-spin text-muted-foreground" />
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">This source has no settings.</p>
      ) : (
        <div className="space-y-5">
          <Items items={items} values={values} change={change} />
        </div>
      )}
    </Sheet>
  );
}

function Items({
  items,
  values,
  change,
}: {
  items: SettingItem[];
  values: Record<string, unknown>;
  change: (k: string, v: unknown) => void;
}) {
  return (
    <>
      {items
        .filter((it) => visible(it, values))
        .map((it, i) => (
          <Item
            key={("key" in it ? it.key : it.title) + i}
            it={it}
            values={values}
            change={change}
          />
        ))}
    </>
  );
}

const pill = "rounded-full px-3 py-1.5 text-xs font-semibold ring-1";
const on = "bg-primary text-primary-foreground ring-primary";
const off = "bg-[color:var(--surface-2)] text-muted-foreground ring-border";

function Item({
  it,
  values,
  change,
}: {
  it: SettingItem;
  values: Record<string, unknown>;
  change: (k: string, v: unknown) => void;
}) {
  const [openPage, setOpenPage] = useState(false);
  switch (it.kind) {
    case "group":
      return (
        <section className="space-y-3">
          {it.title && (
            <h3 className="text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
              {it.title}
            </h3>
          )}
          <Items items={it.items} values={values} change={change} />
          {it.footer && (
            <p className="text-[11px] leading-relaxed text-muted-foreground">{it.footer}</p>
          )}
        </section>
      );
    case "page":
      return (
        <div className="rounded-xl ring-1 ring-border">
          <button
            onClick={() => setOpenPage((o) => !o)}
            aria-expanded={openPage}
            className="flex w-full items-center justify-between px-3 py-2.5 text-sm font-semibold"
          >
            {it.title}
            <ChevronRight
              className={`h-4 w-4 transition-transform ${openPage ? "rotate-90" : ""}`}
            />
          </button>
          {openPage && (
            <div className="space-y-4 border-t border-border p-3">
              <Items items={it.items} values={values} change={change} />
            </div>
          )}
        </div>
      );
    case "select": {
      const cur = String(values[it.key] ?? it.def);
      return (
        <div>
          <p className="mb-1.5 text-sm">{it.title}</p>
          <div className="flex flex-wrap gap-1.5">
            {it.values.map((v, i) => (
              <button
                key={v}
                aria-pressed={cur === v}
                onClick={() => change(it.key, v)}
                className={`${pill} ${cur === v ? on : off}`}
              >
                {it.titles[i]}
              </button>
            ))}
          </div>
        </div>
      );
    }
    case "multi": {
      const cur = Array.isArray(values[it.key]) ? (values[it.key] as string[]) : it.def;
      return (
        <div>
          <p className="mb-1.5 text-sm">{it.title}</p>
          <div className="flex flex-wrap gap-1.5">
            {it.values.map((v, i) => {
              const sel = cur.includes(v);
              return (
                <button
                  key={v}
                  aria-pressed={sel}
                  onClick={() => change(it.key, sel ? cur.filter((x) => x !== v) : [...cur, v])}
                  className={`${pill} ${sel ? on : off}`}
                >
                  {it.titles[i]}
                </button>
              );
            })}
          </div>
        </div>
      );
    }
    case "switch":
      return (
        <label className="flex cursor-pointer items-center justify-between gap-3 text-sm">
          <span>
            {it.title}
            {it.subtitle && (
              <span className="block text-[11px] text-muted-foreground">{it.subtitle}</span>
            )}
          </span>
          <input
            type="checkbox"
            checked={!!(values[it.key] ?? it.def)}
            onChange={(e) => change(it.key, e.target.checked)}
            className="h-4 w-4 shrink-0 accent-[color:var(--primary)]"
          />
        </label>
      );
    case "text":
      return <TextSetting it={it} value={String(values[it.key] ?? it.def)} change={change} />;
    case "stepper": {
      const cur = Number(values[it.key] ?? it.def);
      const clamp = (n: number) => Math.min(it.max, Math.max(it.min, n));
      return (
        <div className="flex items-center justify-between text-sm">
          {it.title}
          <div className="flex items-center gap-2">
            <button
              aria-label="Decrease"
              onClick={() => change(it.key, clamp(cur - it.step))}
              className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface-2)] ring-1 ring-border"
            >
              <Minus className="h-3.5 w-3.5" />
            </button>
            <span className="w-8 text-center tabular-nums">{cur}</span>
            <button
              aria-label="Increase"
              onClick={() => change(it.key, clamp(cur + it.step))}
              className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface-2)] ring-1 ring-border"
            >
              <Plus className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      );
    }
    case "link":
      return (
        <a
          href={it.url}
          target="_blank"
          rel="noreferrer noopener"
          className="flex items-center justify-between text-sm text-primary"
        >
          {it.title} <ExternalLink className="h-3.5 w-3.5" />
        </a>
      );
  }
}

function TextSetting({
  it,
  value,
  change,
}: {
  it: Extract<SettingItem, { kind: "text" }>;
  value: string;
  change: (k: string, v: unknown) => void;
}) {
  const [v, setV] = useState(value);
  useEffect(() => setV(value), [value]);
  return (
    <div>
      <p className="mb-1.5 text-sm">{it.title}</p>
      <input
        value={v}
        type={it.secure ? "password" : "text"}
        autoCapitalize="off"
        autoCorrect="off"
        onChange={(e) => setV(e.target.value)}
        onBlur={() => v !== value && change(it.key, v)}
        placeholder={it.placeholder}
        className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2.5 text-sm ring-1 ring-border focus:outline-none focus:ring-primary/60"
      />
    </div>
  );
}
