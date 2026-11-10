
CREATE TABLE public.chapter_integrity_issues (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  chapter_id UUID NOT NULL REFERENCES public.chapters(id) ON DELETE CASCADE,
  series_id UUID NOT NULL REFERENCES public.series(id) ON DELETE CASCADE,
  issue TEXT NOT NULL,                 -- 'no_pages' | 'count_mismatch' | 'missing_page_numbers' | 'duplicate_pages'
  expected_pages INTEGER,
  actual_pages INTEGER,
  missing_numbers INTEGER[] DEFAULT '{}',
  extra_numbers INTEGER[] DEFAULT '{}',
  checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ,
  UNIQUE (chapter_id, issue)
);

GRANT SELECT ON public.chapter_integrity_issues TO anon, authenticated;
GRANT ALL ON public.chapter_integrity_issues TO service_role;

ALTER TABLE public.chapter_integrity_issues ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Integrity issues public read"
  ON public.chapter_integrity_issues FOR SELECT
  USING (true);

CREATE INDEX idx_integrity_open ON public.chapter_integrity_issues (checked_at DESC) WHERE resolved_at IS NULL;
CREATE INDEX idx_integrity_series ON public.chapter_integrity_issues (series_id) WHERE resolved_at IS NULL;

-- Efficient integrity scan: returns per-chapter counts and missing/extra page
-- numbers for a batch of chapters ordered by least-recently-checked.
CREATE OR REPLACE FUNCTION public.scan_chapter_integrity(_limit INTEGER DEFAULT 500)
RETURNS TABLE (
  chapter_id UUID,
  series_id UUID,
  external_id TEXT,
  expected INTEGER,
  actual INTEGER,
  distinct_actual INTEGER,
  missing INTEGER[],
  extra INTEGER[]
)
LANGUAGE sql
STABLE
SET search_path = public
AS $$
  WITH batch AS (
    SELECT c.id, c.series_id, c.external_id, COALESCE(c.page_count, 0) AS expected
    FROM public.chapters c
    LEFT JOIN LATERAL (
      SELECT MAX(checked_at) AS last_checked
      FROM public.chapter_integrity_issues i
      WHERE i.chapter_id = c.id
    ) l ON true
    ORDER BY COALESCE(l.last_checked, 'epoch'::timestamptz) ASC
    LIMIT _limit
  ),
  agg AS (
    SELECT
      b.id AS chapter_id,
      b.series_id,
      b.external_id,
      b.expected,
      COUNT(p.id)::int AS actual,
      COUNT(DISTINCT p.page_number)::int AS distinct_actual,
      COALESCE(array_agg(p.page_number ORDER BY p.page_number)
               FILTER (WHERE p.page_number IS NOT NULL), '{}') AS nums
    FROM batch b
    LEFT JOIN public.pages p ON p.chapter_id = b.id
    GROUP BY b.id, b.series_id, b.external_id, b.expected
  )
  SELECT
    a.chapter_id,
    a.series_id,
    a.external_id,
    a.expected,
    a.actual,
    a.distinct_actual,
    CASE
      WHEN a.expected > 0 THEN
        ARRAY(
          SELECT g FROM generate_series(1, a.expected) g
          WHERE g <> ALL (a.nums)
        )
      ELSE '{}'::int[]
    END AS missing,
    CASE
      WHEN a.expected > 0 THEN
        ARRAY(SELECT n FROM unnest(a.nums) n WHERE n < 1 OR n > a.expected)
      ELSE '{}'::int[]
    END AS extra
  FROM agg a;
$$;
