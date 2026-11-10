-- Sources registry
CREATE TABLE public.sync_sources (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text NOT NULL UNIQUE,
  name text NOT NULL,
  base_url text NOT NULL,
  kind text NOT NULL DEFAULT 'api',
  status text NOT NULL DEFAULT 'active',
  confidence_score int NOT NULL DEFAULT 100,
  rate_limit_per_min int NOT NULL DEFAULT 30,
  config jsonb NOT NULL DEFAULT '{}'::jsonb,
  last_crawled_at timestamptz,
  last_error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

GRANT SELECT ON public.sync_sources TO anon, authenticated;
GRANT ALL ON public.sync_sources TO service_role;
ALTER TABLE public.sync_sources ENABLE ROW LEVEL SECURITY;
CREATE POLICY "sync_sources public read" ON public.sync_sources FOR SELECT USING (true);

-- Sync job queue (Postgres-backed, no Redis needed)
CREATE TABLE public.sync_jobs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  source_id uuid NOT NULL REFERENCES public.sync_sources(id) ON DELETE CASCADE,
  kind text NOT NULL, -- 'discover' | 'series' | 'chapter'
  external_id text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  status text NOT NULL DEFAULT 'pending', -- pending | processing | done | failed
  attempts int NOT NULL DEFAULT 0,
  max_attempts int NOT NULL DEFAULT 5,
  last_error text,
  locked_at timestamptz,
  run_after timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (source_id, kind, external_id)
);

CREATE INDEX idx_sync_jobs_pull ON public.sync_jobs (status, run_after) WHERE status = 'pending';
CREATE INDEX idx_sync_jobs_source ON public.sync_jobs (source_id);

GRANT SELECT ON public.sync_jobs TO authenticated;
GRANT ALL ON public.sync_jobs TO service_role;
ALTER TABLE public.sync_jobs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "sync_jobs authed read" ON public.sync_jobs FOR SELECT TO authenticated USING (true);

-- External-id mapping for idempotent ingest
ALTER TABLE public.series ADD COLUMN IF NOT EXISTS source_id uuid REFERENCES public.sync_sources(id);
ALTER TABLE public.series ADD COLUMN IF NOT EXISTS external_id text;
CREATE UNIQUE INDEX IF NOT EXISTS uq_series_source_external ON public.series (source_id, external_id) WHERE external_id IS NOT NULL;

ALTER TABLE public.chapters ADD COLUMN IF NOT EXISTS external_id text;
CREATE UNIQUE INDEX IF NOT EXISTS uq_chapters_series_external ON public.chapters (series_id, external_id) WHERE external_id IS NOT NULL;

-- Atomic job claim: pulls + locks N pending jobs in one shot
CREATE OR REPLACE FUNCTION public.claim_sync_jobs(_limit int)
RETURNS SETOF public.sync_jobs
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  UPDATE public.sync_jobs
  SET status = 'processing',
      locked_at = now(),
      attempts = attempts + 1,
      updated_at = now()
  WHERE id IN (
    SELECT id FROM public.sync_jobs
    WHERE status = 'pending' AND run_after <= now()
    ORDER BY run_after ASC
    FOR UPDATE SKIP LOCKED
    LIMIT _limit
  )
  RETURNING *;
$$;

-- Seed MangaDex as the first source
INSERT INTO public.sync_sources (code, name, base_url, kind, rate_limit_per_min, config)
VALUES ('mangadex', 'MangaDex', 'https://api.mangadex.org', 'api', 30,
  '{"languages":["en"],"content_rating":["safe","suggestive"]}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- Seed one discovery job to kick off the pipeline
INSERT INTO public.sync_jobs (source_id, kind, external_id, payload)
SELECT id, 'discover', 'latest', '{"offset":0,"limit":20}'::jsonb
FROM public.sync_sources WHERE code = 'mangadex'
ON CONFLICT DO NOTHING;