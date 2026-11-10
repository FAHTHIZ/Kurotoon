import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { ArrowDown, ArrowUp, Check, Minus } from "lucide-react";
import { Sheet } from "./LibraryShelf";
import { spring } from "@/lib/motion";
import {
  cycleMulti,
  optionId,
  type FilterState,
  type MultiValue,
  type SortValue,
  type UiFilter,
} from "@/engine/filters";

/** Aidoku-style filter sheet: sort, selects, tag pills, toggles and text. */
export function SourceFilterSheet({
  open,
  onClose,
  filters,
  value,
  onApply,
}: {
  open: boolean;
  onClose: () => void;
  filters: UiFilter[];
  value: FilterState;
  onApply: (state: FilterState) => void;
}) {
  const [draft, setDraft] = useState<FilterState>(value);
  useEffect(() => {
    if (open) setDraft(value);
  }, [open, value]);
  const set = (id: string, v: FilterState[string] | undefined) =>
    setDraft((d) => {
      const n = { ...d };
      if (v === undefined) delete n[id];
      else n[id] = v;
      return n;
    });

  return (
    <Sheet
      open={open}
      onClose={() => {
        setDraft(value);
        onClose();
      }}
      title="Filters"
    >
      <div className="space-y-5 pb-16">
        {filters.map((f) => (
          <Field key={`${f.kind}:${f.id}`} f={f} draft={draft} set={set} />
        ))}
      </div>
      <div className="sticky bottom-0 -mx-4 -mb-4 flex gap-2 border-t border-border bg-[color:var(--surface)] p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]">
        <button
          onClick={() => setDraft({})}
          className="flex-1 rounded-full bg-[color:var(--surface-2)] px-4 py-2.5 text-sm font-semibold ring-1 ring-border"
        >
          Reset
        </button>
        <motion.button
          whileTap={{ scale: 0.96 }}
          transition={spring.tap}
          onClick={() => {
            onApply(draft);
            onClose();
          }}
          className="flex-1 rounded-full bg-[image:var(--gradient-accent)] px-4 py-2.5 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)]"
        >
          Apply
        </motion.button>
      </div>
    </Sheet>
  );
}

const pill = "rounded-full px-3 py-1.5 text-xs font-semibold ring-1 transition-colors";
const off = "bg-[color:var(--surface-2)] text-muted-foreground ring-border";
const on = "bg-primary text-primary-foreground ring-primary";

function Field({
  f,
  draft,
  set,
}: {
  f: UiFilter;
  draft: FilterState;
  set: (id: string, v: FilterState[string] | undefined) => void;
}) {
  const label = (
    <h3 className="mb-2 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
      {f.title}
    </h3>
  );
  switch (f.kind) {
    case "group":
      return (
        <section>
          {label}
          <div className="space-y-4 border-l border-border pl-3">
            {f.filters.map((c) => (
              <Field key={`${c.kind}:${c.id}`} f={c} draft={draft} set={set} />
            ))}
          </div>
        </section>
      );
    case "text":
      return (
        <section>
          {label}
          <input
            value={String(draft[f.id] ?? "")}
            onChange={(e) => set(f.id, e.target.value || undefined)}
            placeholder={f.placeholder || f.title}
            className="w-full rounded-xl bg-[color:var(--surface-2)] px-3 py-2.5 text-sm ring-1 ring-border focus:outline-none focus:ring-primary/60"
          />
        </section>
      );
    case "sort": {
      const cur = (draft[f.id] as SortValue | undefined) ?? f.def;
      return (
        <section>
          {label}
          <div className="flex flex-wrap gap-1.5">
            {f.options.map((o, i) => {
              const sel = cur.index === i;
              return (
                <button
                  key={o}
                  aria-pressed={sel}
                  onClick={() =>
                    set(
                      f.id,
                      sel
                        ? f.canAscend
                          ? { index: i, ascending: !cur.ascending }
                          : cur
                        : { index: i, ascending: false },
                    )
                  }
                  className={`${pill} inline-flex items-center gap-1 ${sel ? on : off}`}
                >
                  {sel &&
                    (cur.ascending ? (
                      <ArrowUp className="h-3 w-3" />
                    ) : (
                      <ArrowDown className="h-3 w-3" />
                    ))}
                  {o}
                </button>
              );
            })}
          </div>
        </section>
      );
    }
    case "select": {
      const cur = (draft[f.id] as number | undefined) ?? f.def;
      return (
        <section>
          {label}
          <div className="flex flex-wrap gap-1.5">
            {f.options.map((o, i) =>
              f.hidden?.[i] && cur !== i ? null : (
                <button
                  key={`${o}-${i}`}
                  aria-pressed={cur === i}
                  onClick={() => set(f.id, i)}
                  className={`${pill} ${cur === i ? on : off}`}
                >
                  {o}
                </button>
              ),
            )}
          </div>
        </section>
      );
    }
    case "multi": {
      const cur = draft[f.id] as MultiValue | undefined;
      return (
        <section>
          {label}
          <div className="flex flex-wrap gap-1.5">
            {f.options.map((o, i) => {
              const oid = optionId(f, i);
              const inc = cur?.inc.includes(oid);
              const exc = cur?.exc.includes(oid);
              return (
                <button
                  key={`${oid}-${i}`}
                  aria-pressed={inc ? "true" : exc ? "mixed" : "false"}
                  aria-label={`${o}${inc ? ", included" : exc ? ", excluded" : ""}`}
                  onClick={() => {
                    const n = cycleMulti(f, cur, oid);
                    set(f.id, n.inc.length || n.exc.length ? n : undefined);
                  }}
                  className={`${pill} inline-flex items-center gap-1 ${inc ? on : exc ? "bg-destructive/15 text-destructive line-through ring-destructive/40" : off}`}
                >
                  {inc && <Check className="h-3 w-3" />}
                  {exc && <Minus className="h-3 w-3" />}
                  {o}
                </button>
              );
            })}
          </div>
          {f.canExclude && (
            <p className="mt-1.5 text-[10px] text-muted-foreground">Tap again to exclude.</p>
          )}
        </section>
      );
    }
    case "check": {
      const cur = (draft[f.id] as boolean | undefined) ?? f.def;
      return (
        <label className="flex cursor-pointer items-center justify-between text-sm">
          {f.title}
          <input
            type="checkbox"
            checked={cur}
            onChange={(e) => set(f.id, e.target.checked)}
            className="h-4 w-4 accent-[color:var(--primary)]"
          />
        </label>
      );
    }
  }
}
