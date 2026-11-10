
DO $$ BEGIN
  CREATE TYPE public.app_role AS ENUM ('admin', 'moderator', 'user');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS public.user_roles (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role public.app_role NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, role)
);

GRANT SELECT ON public.user_roles TO authenticated;
GRANT ALL ON public.user_roles TO service_role;

ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
  CREATE POLICY "Users can view own roles" ON public.user_roles
    FOR SELECT TO authenticated USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE OR REPLACE FUNCTION public.has_role(_user_id uuid, _role public.app_role)
RETURNS boolean
LANGUAGE sql
STABLE SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_roles WHERE user_id = _user_id AND role = _role
  );
$$;

-- Admin helper: enqueue a chapter rescrape (or every chapter in a series).
CREATE OR REPLACE FUNCTION public.admin_enqueue_rescrape(
  _series_id uuid DEFAULT NULL,
  _chapter_id uuid DEFAULT NULL
)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  uid uuid := auth.uid();
  queued integer := 0;
BEGIN
  IF uid IS NULL OR NOT public.has_role(uid, 'admin') THEN
    RAISE EXCEPTION 'admin required';
  END IF;

  IF _chapter_id IS NOT NULL THEN
    INSERT INTO public.sync_jobs (source_id, kind, payload, status, run_after, attempts)
    SELECT c.source_id, 'chapter_pages',
           jsonb_build_object('chapter_id', c.id, 'external_id', c.external_id),
           'pending', now(), 0
    FROM public.chapters c
    LEFT JOIN public.series s ON s.id = c.series_id
    WHERE c.id = _chapter_id;
    GET DIAGNOSTICS queued = ROW_COUNT;

  ELSIF _series_id IS NOT NULL THEN
    INSERT INTO public.sync_jobs (source_id, kind, payload, status, run_after, attempts)
    SELECT s.source_id, 'chapter_pages',
           jsonb_build_object('chapter_id', c.id, 'external_id', c.external_id),
           'pending', now(), 0
    FROM public.chapters c
    JOIN public.series s ON s.id = c.series_id
    WHERE c.series_id = _series_id;
    GET DIAGNOSTICS queued = ROW_COUNT;
  ELSE
    RAISE EXCEPTION 'series_id or chapter_id required';
  END IF;

  RETURN queued;
END;
$$;

GRANT EXECUTE ON FUNCTION public.admin_enqueue_rescrape(uuid, uuid) TO authenticated;
