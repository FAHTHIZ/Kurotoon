import { useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ArrowDown, ArrowUp, Check, ChevronDown, ChevronUp, Pencil, Plus, SlidersHorizontal, Trash2, X } from "lucide-react";
import { MangaCard } from "@/components/MangaCard";
import { useEngine } from "@/engine/store";
import type { SeriesCard } from "@/engine/types";
import {
  addCategory,
  arrangeLibrary,
  deleteCategory,
  getCategories,
  getLibraryView,
  getSeriesCategories,
  moveCategory,
  renameCategory,
  setLibraryView,
  setSeriesCategories,
  unreadCount,
  type LibrarySort,
  type LibraryView,
} from "@/lib/categories";
import { idOf } from "@/lib/local-library";
import { revealUp, spring, staggerContainer } from "@/lib/motion";

const SORTS: Array<{ id: LibrarySort; label: string }> = [
  { id: "added", label: "Date added" },
  { id: "lastRead", label: "Last read" },
  { id: "title", label: "Alphabetical" },
  { id: "unread", label: "Unread count" },
];

/** Mihon-style library: category tabs, sort, filters and unread badges. */
export function LibraryShelf({ items }: { items: SeriesCard[] }) {
  const engine = useEngine();
  const [view, setView] = useState<LibraryView>(getLibraryView);
  const [cats, setCats] = useState<string[]>([]);
  const [sheet, setSheet] = useState(false);
  const [editCats, setEditCats] = useState(false);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    setCats(getCategories());
    setView(getLibraryView());
    const on = () => {
      setCats(getCategories());
      setTick((n) => n + 1);
    };
    window.addEventListener("kt-local-change", on);
    return () => window.removeEventListener("kt-local-change", on);
  }, []);

  const update = (patch: Partial<LibraryView>) => {
    const next = { ...view, ...patch, filter: { ...view.filter, ...(patch.filter ?? {}) } };
    setView(next);
    setLibraryView(next);
  };

  const shown = useMemo(() => arrangeLibrary(items, view), [items, view, tick]);
  const filtersOn = view.filter.unread || view.filter.started || !!view.filter.source;
  const usedSources = useMemo(() => [...new Set(items.map((s) => s.sourceId))], [items]);
  const sourceName = (id: string) => engine.installed.find((s) => s.id === id)?.name ?? id;

  const chips = [
    { id: "__all", label: "All" },
    ...(cats.length ? [{ id: "__default", label: "Default" }] : []),
    ...cats.map((c) => ({ id: c, label: c })),
  ];

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <div className="flex min-w-0 flex-1 gap-1.5 overflow-x-auto pb-0.5">
          {chips.map((c) => (
            <motion.button
              key={c.id}
              whileTap={{ scale: 0.94 }}
              transition={spring.tap}
              onClick={() => update({ category: c.id })}
              className={`shrink-0 rounded-full px-3 py-1.5 text-[11px] font-bold ring-1 transition ${
                view.category === c.id ? "bg-primary text-primary-foreground ring-primary" : "bg-[color:var(--surface)] text-muted-foreground ring-border"
              }`}
            >
              {c.label}
            </motion.button>
          ))}
          <button onClick={() => setEditCats(true)} aria-label="Edit categories"
            className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-[color:var(--surface)] ring-1 ring-border">
            <Pencil className="h-3 w-3" />
          </button>
        </div>
        <motion.button whileTap={{ scale: 0.92 }} transition={spring.tap} onClick={() => setSheet(true)} aria-label="Sort and filter"
          className={`relative grid h-9 w-9 shrink-0 place-items-center rounded-full ring-1 ${filtersOn ? "bg-primary/20 ring-primary/40" : "bg-[color:var(--surface)] ring-border"}`}>
          <SlidersHorizontal className="h-4 w-4" />
          {filtersOn && <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-primary" />}
        </motion.button>
      </div>

      {shown.length === 0 ? (
        <p className="py-8 text-center text-xs text-muted-foreground">Nothing matches this category and filter.</p>
      ) : (
        <motion.div variants={staggerContainer(0.03)} initial="hidden" animate="show" className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {shown.map((s, i) => {
            const u = unreadCount(idOf(s));
            return <MangaCard key={idOf(s)} s={s} index={i} badge={u ? String(u) : undefined} />;
          })}
        </motion.div>
      )}

      <Sheet open={sheet} onClose={() => setSheet(false)} title="Sort & filter">
        <p className="mb-2 text-[11px] font-bold uppercase tracking-widest text-muted-foreground">Filter</p>
        <Toggle label="Unread" on={view.filter.unread} onChange={(v) => update({ filter: { ...view.filter, unread: v } })} />
        <Toggle label="Started" on={view.filter.started} onChange={(v) => update({ filter: { ...view.filter, started: v } })} />
        {usedSources.length > 1 && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            <Chip on={!view.filter.source} onClick={() => update({ filter: { ...view.filter, source: null } })}>All sources</Chip>
            {usedSources.map((id) => (
              <Chip key={id} on={view.filter.source === id} onClick={() => update({ filter: { ...view.filter, source: id } })}>{sourceName(id)}</Chip>
            ))}
          </div>
        )}
        <p className="mb-2 mt-4 text-[11px] font-bold uppercase tracking-widest text-muted-foreground">Sort</p>
        {SORTS.map((s) => (
          <button key={s.id}
            onClick={() => update(view.sort === s.id ? { desc: !view.desc } : { sort: s.id, desc: s.id !== "title" })}
            className="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm hover:bg-[color:var(--surface-2)]">
            <span className={view.sort === s.id ? "font-bold text-primary" : ""}>{s.label}</span>
            {view.sort === s.id && (view.desc ? <ArrowDown className="h-4 w-4 text-primary" /> : <ArrowUp className="h-4 w-4 text-primary" />)}
          </button>
        ))}
      </Sheet>

      <Sheet open={editCats} onClose={() => setEditCats(false)} title="Edit categories">
        <CategoryEditor cats={cats} />
      </Sheet>
    </div>
  );
}

function CategoryEditor({ cats }: { cats: string[] }) {
  const [name, setName] = useState("");
  const [renaming, setRenaming] = useState<string | null>(null);
  return (
    <div className="space-y-2">
      <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); if (addCategory(name)) setName(""); }}>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="New category"
          className="min-w-0 flex-1 rounded-full bg-[color:var(--surface-2)] px-4 py-2 text-sm outline-none ring-1 ring-border focus:ring-primary" />
        <button type="submit" className="grid h-9 w-9 place-items-center rounded-full bg-primary text-primary-foreground" aria-label="Add category">
          <Plus className="h-4 w-4" />
        </button>
      </form>
      {cats.length === 0 && <p className="px-1 text-xs text-muted-foreground">No categories yet. Series without a category appear under Default.</p>}
      {cats.map((c, i) => (
        <div key={c} className="flex items-center gap-1.5 rounded-xl bg-[color:var(--surface)] p-2 ring-1 ring-border">
          {renaming === c ? (
            <input autoFocus defaultValue={c}
              onBlur={(e) => { renameCategory(c, e.currentTarget.value); setRenaming(null); }}
              onKeyDown={(e) => { if (e.key === "Enter") { renameCategory(c, e.currentTarget.value); setRenaming(null); } }}
              className="min-w-0 flex-1 rounded-lg bg-black/30 px-2 py-1 text-sm outline-none" />
          ) : (
            <span className="min-w-0 flex-1 truncate px-1 text-sm font-semibold">{c}</span>
          )}
          <IconBtn label="Move up" disabled={i === 0} onClick={() => moveCategory(c, -1)}><ChevronUp className="h-3.5 w-3.5" /></IconBtn>
          <IconBtn label="Move down" disabled={i === cats.length - 1} onClick={() => moveCategory(c, 1)}><ChevronDown className="h-3.5 w-3.5" /></IconBtn>
          <IconBtn label="Rename" onClick={() => setRenaming(c)}><Pencil className="h-3.5 w-3.5" /></IconBtn>
          <IconBtn label="Delete" onClick={() => deleteCategory(c)}><Trash2 className="h-3.5 w-3.5" /></IconBtn>
        </div>
      ))}
    </div>
  );
}

/** Pick categories for one series (used on the series page). */
export function CategoryPicker({ seriesId, onClose }: { seriesId: string; onClose: () => void }) {
  const [cats] = useState(getCategories);
  const [sel, setSel] = useState<string[]>(() => getSeriesCategories(seriesId));
  const [name, setName] = useState("");
  const [all, setAll] = useState(cats);
  const save = () => {
    setSeriesCategories(seriesId, sel);
    onClose();
  };
  return (
    <div className="space-y-2">
      {all.map((c) => (
        <Toggle key={c} label={c} on={sel.includes(c)} onChange={(v) => setSel(v ? [...sel, c] : sel.filter((x) => x !== c))} />
      ))}
      <form className="flex gap-2 pt-1" onSubmit={(e) => { e.preventDefault(); if (addCategory(name)) { setAll(getCategories()); setSel([...sel, name.trim()]); setName(""); } }}>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="New category"
          className="min-w-0 flex-1 rounded-full bg-[color:var(--surface-2)] px-4 py-2 text-sm outline-none ring-1 ring-border focus:ring-primary" />
        <button type="submit" className="grid h-9 w-9 place-items-center rounded-full bg-[color:var(--surface-2)] ring-1 ring-border" aria-label="Add category">
          <Plus className="h-4 w-4" />
        </button>
      </form>
      <button onClick={save} className="mt-2 w-full rounded-full bg-primary py-2.5 text-sm font-bold text-primary-foreground shadow-[var(--shadow-glow)]">
        Save
      </button>
    </div>
  );
}

function IconBtn({ children, label, onClick, disabled }: { children: React.ReactNode; label: string; onClick: () => void; disabled?: boolean }) {
  return (
    <button aria-label={label} disabled={disabled} onClick={onClick}
      className="grid h-7 w-7 place-items-center rounded-full text-muted-foreground hover:bg-[color:var(--surface-2)] hover:text-foreground disabled:opacity-30">
      {children}
    </button>
  );
}

function Toggle({ label, on, onChange }: { label: string; on: boolean; onChange: (v: boolean) => void }) {
  return (
    <button onClick={() => onChange(!on)} className="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm hover:bg-[color:var(--surface-2)]">
      <span>{label}</span>
      <span className={`grid h-5 w-5 place-items-center rounded-md ring-1 ${on ? "bg-primary text-primary-foreground ring-primary" : "ring-border"}`}>
        {on && <Check className="h-3.5 w-3.5" />}
      </span>
    </button>
  );
}

function Chip({ on, onClick, children }: { on: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button onClick={onClick}
      className={`rounded-full px-3 py-1 text-[11px] font-bold ring-1 ${on ? "bg-primary text-primary-foreground ring-primary" : "bg-[color:var(--surface)] text-muted-foreground ring-border"}`}>
      {children}
    </button>
  );
}

/** Bottom sheet used by the library screens. */
export function Sheet({ open, onClose, title, children }: { open: boolean; onClose: () => void; title: string; children: React.ReactNode }) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-sm md:items-center"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
          <motion.div role="dialog" aria-label={title} variants={revealUp}
            initial={{ y: 40, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 40, opacity: 0 }} transition={spring.soft}
            onClick={(e) => e.stopPropagation()}
            className="kt-card max-h-[80vh] w-full max-w-md overflow-y-auto rounded-t-3xl p-4 pb-[calc(1rem+env(safe-area-inset-bottom))] md:rounded-3xl">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-black">{title}</h2>
              <button onClick={onClose} aria-label="Close" className="grid h-8 w-8 place-items-center rounded-full bg-[color:var(--surface-2)]">
                <X className="h-4 w-4" />
              </button>
            </div>
            {children}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
