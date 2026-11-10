CREATE OR REPLACE FUNCTION public.list_genres()
RETURNS TABLE(genre text, count bigint)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public
AS $$
  SELECT g AS genre, COUNT(*)::bigint AS count
  FROM (SELECT unnest(genres) AS g FROM public.series) t
  WHERE g IS NOT NULL AND g <> ''
  GROUP BY g
  ORDER BY count DESC, g ASC;
$$;