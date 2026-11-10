
ALTER TABLE public.chapters
  ADD COLUMN IF NOT EXISTS last_403_at timestamptz,
  ADD COLUMN IF NOT EXISTS pages_refreshed_at timestamptz;

CREATE INDEX IF NOT EXISTS chapters_refresh_candidates_idx
  ON public.chapters (last_403_at DESC)
  WHERE last_403_at IS NOT NULL;
