import { useRef, useState } from "react";
import { motion } from "framer-motion";
import { Download, Upload, Loader2, HardDriveDownload } from "lucide-react";
import { toast } from "sonner";
import { downloadBackup, parseBackup, restoreBackup } from "@/lib/backup";
import { spring } from "@/lib/motion";

/** Mihon-style "Backup and restore" panel. */
export function BackupCard() {
  const file = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const onExport = async () => {
    setBusy("Creating backup…");
    try {
      await downloadBackup();
      toast.success("Backup saved", { description: "Keep the file somewhere safe." });
    } catch (e) {
      toast.error("Backup failed", { description: e instanceof Error ? e.message : String(e) });
    } finally {
      setBusy(null);
    }
  };

  const onImport = async (f: File) => {
    setBusy("Reading backup…");
    try {
      const b = parseBackup(await f.text());
      const r = await restoreBackup(b, setBusy);
      toast.success("Backup restored", {
        description: `${r.repos} repositories, ${r.sources} sources${r.failed.length ? `, ${r.failed.length} could not be restored` : ""}.`,
      });
    } catch (e) {
      toast.error("Restore failed", { description: e instanceof Error ? e.message : String(e) });
    } finally {
      setBusy(null);
      if (file.current) file.current.value = "";
    }
  };

  return (
    <section className="kt-card rounded-2xl p-4">
      <div className="flex items-center gap-3">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/12 ring-1 ring-primary/20">
          <HardDriveDownload className="h-4 w-4 text-primary" />
        </span>
        <div className="min-w-0">
          <p className="text-sm font-bold">Backup and restore</p>
          <p className="text-[11px] text-muted-foreground">
            Library, progress, history, settings, rewards, repositories and sources in one file.
          </p>
        </div>
      </div>
      <div className="mt-3 grid grid-cols-2 gap-2">
        <motion.button whileTap={{ scale: 0.96 }} transition={spring.tap} disabled={!!busy} onClick={onExport}
          className="inline-flex items-center justify-center gap-1.5 rounded-full bg-primary py-2 text-xs font-bold text-primary-foreground shadow-[var(--shadow-glow)] disabled:opacity-60">
          <Download className="h-3.5 w-3.5" /> Create backup
        </motion.button>
        <motion.button whileTap={{ scale: 0.96 }} transition={spring.tap} disabled={!!busy} onClick={() => file.current?.click()}
          className="inline-flex items-center justify-center gap-1.5 rounded-full bg-[color:var(--surface-2)] py-2 text-xs font-bold ring-1 ring-border disabled:opacity-60">
          <Upload className="h-3.5 w-3.5" /> Restore
        </motion.button>
      </div>
      <input ref={file} type="file" accept="application/json,.json" className="hidden"
        onChange={(e) => { const f = e.currentTarget.files?.[0]; if (f) void onImport(f); }} />
      {busy && (
        <p className="mt-2 flex items-center gap-1.5 text-[11px] text-muted-foreground">
          <Loader2 className="h-3 w-3 animate-spin" /> {busy}
        </p>
      )}
    </section>
  );
}
