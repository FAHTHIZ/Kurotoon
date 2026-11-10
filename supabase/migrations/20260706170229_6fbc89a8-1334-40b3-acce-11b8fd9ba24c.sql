
ALTER TABLE public.chapter_integrity_issues
  ADD COLUMN IF NOT EXISTS repair_attempts integer NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_repair_at timestamptz,
  ADD COLUMN IF NOT EXISTS next_repair_after timestamptz;

CREATE INDEX IF NOT EXISTS chapter_integrity_issues_watchdog_idx
  ON public.chapter_integrity_issues (resolved_at, next_repair_after)
  WHERE resolved_at IS NULL;
