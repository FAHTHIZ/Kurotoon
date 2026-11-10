CREATE TABLE public.repositories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name text NOT NULL,
  url text,
  kind text NOT NULL DEFAULT 'source',
  description text,
  icon_url text,
  enabled boolean NOT NULL DEFAULT true,
  builtin boolean NOT NULL DEFAULT false,
  last_synced_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, url)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.repositories TO authenticated;
GRANT ALL ON public.repositories TO service_role;
ALTER TABLE public.repositories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own repositories" ON public.repositories FOR ALL TO authenticated
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE TABLE public.repo_sources (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  repo_id uuid NOT NULL REFERENCES public.repositories(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  code text NOT NULL,
  name text NOT NULL,
  lang text NOT NULL DEFAULT 'en',
  base_url text,
  version text,
  nsfw boolean NOT NULL DEFAULT false,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (repo_id, code)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.repo_sources TO authenticated;
GRANT ALL ON public.repo_sources TO service_role;
ALTER TABLE public.repo_sources ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own repo sources" ON public.repo_sources FOR ALL TO authenticated
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE TABLE public.repo_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  repo_id uuid NOT NULL REFERENCES public.repositories(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  collection_name text NOT NULL DEFAULT 'Collection',
  series_ref text NOT NULL,
  title text NOT NULL,
  cover_url text,
  position integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (repo_id, collection_name, series_ref)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.repo_items TO authenticated;
GRANT ALL ON public.repo_items TO service_role;
ALTER TABLE public.repo_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own repo items" ON public.repo_items FOR ALL TO authenticated
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE INDEX idx_repo_sources_repo ON public.repo_sources(repo_id);
CREATE INDEX idx_repo_items_repo ON public.repo_items(repo_id);
CREATE INDEX idx_repositories_user ON public.repositories(user_id);

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = now(); RETURN NEW; END; $$
LANGUAGE plpgsql SET search_path = public;

CREATE TRIGGER update_repositories_updated_at BEFORE UPDATE ON public.repositories
  FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_repo_sources_updated_at BEFORE UPDATE ON public.repo_sources
  FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();