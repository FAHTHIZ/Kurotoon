CREATE OR REPLACE VIEW public.series_ready
WITH (security_invoker = on) AS
SELECT s.*
FROM public.series s
WHERE EXISTS (SELECT 1 FROM public.chapters c WHERE c.series_id = s.id);

GRANT SELECT ON public.series_ready TO anon, authenticated;
GRANT ALL ON public.series_ready TO service_role;