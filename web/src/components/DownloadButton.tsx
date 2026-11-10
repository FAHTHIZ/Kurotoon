import { ArrowDownToLine, CheckCircle2, Clock, Loader2, AlertCircle } from "lucide-react";
import { toast } from "sonner";
import { seriesId } from "@/engine/ids";
import type { ChapterRow, SeriesCard } from "@/engine/types";
import { cancelDownload, deleteDownload, enqueueDownloads, retryFailed, useDownloads } from "@/lib/downloads";

/** Per-chapter download state + action, like Mihon's chapter row icon. */
export function DownloadButton({ series, chapter }: { series: SeriesCard; chapter: ChapterRow }) {
  const dl = useDownloads();
  const sid = seriesId(series.sourceId, series.key);
  const id = `${sid}|${chapter.key}`;
  const done = dl.downloaded[id];
  const q = dl.queue.find((x) => x.id === id);
  const base = "grid h-12 w-10 shrink-0 place-items-center";

  if (done) {
    return (
      <button className={`${base} text-primary`} aria-label="Delete download"
        onClick={() => { void deleteDownload(sid, chapter.key); toast("Download removed"); }}>
        <CheckCircle2 className="h-4 w-4" />
      </button>
    );
  }
  if (q?.status === "downloading") {
    const pct = q.total ? Math.round((q.done / q.total) * 100) : 0;
    return (
      <span className={`${base} relative text-primary`} aria-label={`Downloading ${pct}%`}>
        <Loader2 className="h-4 w-4 animate-spin" />
        {q.total > 0 && <span className="absolute bottom-1 text-[8px] font-bold tabular-nums">{pct}%</span>}
      </span>
    );
  }
  if (q?.status === "queued") {
    return (
      <button className={`${base} text-muted-foreground`} aria-label="Cancel download" onClick={() => cancelDownload(id)}>
        <Clock className="h-4 w-4" />
      </button>
    );
  }
  if (q?.status === "error") {
    return (
      <button className={`${base} text-amber-400`} aria-label="Retry download" title={q.error} onClick={retryFailed}>
        <AlertCircle className="h-4 w-4" />
      </button>
    );
  }
  return (
    <button className={`${base} text-muted-foreground/60 hover:text-foreground`} aria-label="Download chapter"
      onClick={() => enqueueDownloads(series, [chapter])}>
      <ArrowDownToLine className="h-4 w-4" />
    </button>
  );
}
