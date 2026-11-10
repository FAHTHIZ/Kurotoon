CREATE OR REPLACE FUNCTION public.is_chapter_free(_chapter_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  WITH ranked AS (
    SELECT c.id,
           ROW_NUMBER() OVER (PARTITION BY c.series_id ORDER BY c.number DESC) AS rnk
    FROM public.chapters c
    WHERE c.series_id = (SELECT series_id FROM public.chapters WHERE id = _chapter_id)
  )
  SELECT COALESCE((SELECT rnk > 2 FROM ranked WHERE id = _chapter_id), false);
$$;