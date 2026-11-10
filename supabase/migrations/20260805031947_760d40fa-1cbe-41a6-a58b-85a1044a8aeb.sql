-- Creator + contributor + economy foundation

ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS trust_level text NOT NULL DEFAULT 'new',
  ADD COLUMN IF NOT EXISTS creator_name text,
  ADD COLUMN IF NOT EXISTS banner_url text,
  ADD COLUMN IF NOT EXISTS is_creator boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS contribution_xp integer NOT NULL DEFAULT 0;

-- ============ Creator works ============
CREATE TABLE IF NOT EXISTS public.creator_works (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL,
  title text NOT NULL,
  slug text NOT NULL UNIQUE,
  synopsis text NOT NULL DEFAULT '',
  cover_url text,
  tags text[] NOT NULL DEFAULT '{}',
  atmosphere text,
  status text NOT NULL DEFAULT 'ongoing',
  visibility text NOT NULL DEFAULT 'draft',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.creator_works TO authenticated;
GRANT SELECT ON public.creator_works TO anon;
GRANT ALL ON public.creator_works TO service_role;
ALTER TABLE public.creator_works ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "public reads published works" ON public.creator_works;
CREATE POLICY "public reads published works" ON public.creator_works FOR SELECT USING (visibility = 'published');
DROP POLICY IF EXISTS "owners read own works" ON public.creator_works;
CREATE POLICY "owners read own works" ON public.creator_works FOR SELECT TO authenticated USING (auth.uid() = owner_id);
DROP POLICY IF EXISTS "owners write own works" ON public.creator_works;
CREATE POLICY "owners write own works" ON public.creator_works FOR INSERT TO authenticated WITH CHECK (auth.uid() = owner_id);
DROP POLICY IF EXISTS "owners update own works" ON public.creator_works;
CREATE POLICY "owners update own works" ON public.creator_works FOR UPDATE TO authenticated USING (auth.uid() = owner_id) WITH CHECK (auth.uid() = owner_id);
DROP POLICY IF EXISTS "owners delete own works" ON public.creator_works;
CREATE POLICY "owners delete own works" ON public.creator_works FOR DELETE TO authenticated USING (auth.uid() = owner_id);

CREATE TABLE IF NOT EXISTS public.creator_chapters (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id uuid NOT NULL REFERENCES public.creator_works(id) ON DELETE CASCADE,
  owner_id uuid NOT NULL,
  number numeric NOT NULL,
  title text,
  pages jsonb NOT NULL DEFAULT '[]'::jsonb,
  published boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.creator_chapters TO authenticated;
GRANT SELECT ON public.creator_chapters TO anon;
GRANT ALL ON public.creator_chapters TO service_role;
ALTER TABLE public.creator_chapters ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "public reads published chapters" ON public.creator_chapters;
CREATE POLICY "public reads published chapters" ON public.creator_chapters FOR SELECT USING (published = true);
DROP POLICY IF EXISTS "owners read own chapters" ON public.creator_chapters;
CREATE POLICY "owners read own chapters" ON public.creator_chapters FOR SELECT TO authenticated USING (auth.uid() = owner_id);
DROP POLICY IF EXISTS "owners insert own chapters" ON public.creator_chapters;
CREATE POLICY "owners insert own chapters" ON public.creator_chapters FOR INSERT TO authenticated WITH CHECK (auth.uid() = owner_id);
DROP POLICY IF EXISTS "owners update own chapters" ON public.creator_chapters;
CREATE POLICY "owners update own chapters" ON public.creator_chapters FOR UPDATE TO authenticated USING (auth.uid() = owner_id) WITH CHECK (auth.uid() = owner_id);
DROP POLICY IF EXISTS "owners delete own chapters" ON public.creator_chapters;
CREATE POLICY "owners delete own chapters" ON public.creator_chapters FOR DELETE TO authenticated USING (auth.uid() = owner_id);

-- ============ Contributions ============
CREATE TABLE IF NOT EXISTS public.contributions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  kind text NOT NULL,
  target_ref text NOT NULL,
  target_title text,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  note text,
  status text NOT NULL DEFAULT 'pending',
  reviewed_by uuid,
  reviewed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE ON public.contributions TO authenticated;
GRANT ALL ON public.contributions TO service_role;
ALTER TABLE public.contributions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "users read own contributions" ON public.contributions;
CREATE POLICY "users read own contributions" ON public.contributions FOR SELECT TO authenticated USING (auth.uid() = user_id OR public.has_role(auth.uid(), 'moderator') OR public.has_role(auth.uid(), 'admin'));
DROP POLICY IF EXISTS "users create contributions" ON public.contributions;
CREATE POLICY "users create contributions" ON public.contributions FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "mods review contributions" ON public.contributions;
CREATE POLICY "mods review contributions" ON public.contributions FOR UPDATE TO authenticated USING (public.has_role(auth.uid(), 'moderator') OR public.has_role(auth.uid(), 'admin'));

-- ============ Shop ============
CREATE TABLE IF NOT EXISTS public.shop_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text NOT NULL UNIQUE,
  name text NOT NULL,
  description text NOT NULL DEFAULT '',
  kind text NOT NULL,
  price integer NOT NULL DEFAULT 0,
  rarity text NOT NULL DEFAULT 'common',
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT ON public.shop_items TO anon, authenticated;
GRANT ALL ON public.shop_items TO service_role;
ALTER TABLE public.shop_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "shop items are public" ON public.shop_items;
CREATE POLICY "shop items are public" ON public.shop_items FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.user_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  item_id uuid NOT NULL REFERENCES public.shop_items(id) ON DELETE CASCADE,
  acquired_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, item_id)
);
GRANT SELECT ON public.user_items TO authenticated;
GRANT ALL ON public.user_items TO service_role;
ALTER TABLE public.user_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "users read own items" ON public.user_items;
CREATE POLICY "users read own items" ON public.user_items FOR SELECT TO authenticated USING (auth.uid() = user_id);

-- ============ Follows ============
CREATE TABLE IF NOT EXISTS public.creator_follows (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  follower_id uuid NOT NULL,
  creator_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (follower_id, creator_id)
);
GRANT SELECT, INSERT, DELETE ON public.creator_follows TO authenticated;
GRANT SELECT ON public.creator_follows TO anon;
GRANT ALL ON public.creator_follows TO service_role;
ALTER TABLE public.creator_follows ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "follows are public" ON public.creator_follows;
CREATE POLICY "follows are public" ON public.creator_follows FOR SELECT USING (true);
DROP POLICY IF EXISTS "users follow" ON public.creator_follows;
CREATE POLICY "users follow" ON public.creator_follows FOR INSERT TO authenticated WITH CHECK (auth.uid() = follower_id);
DROP POLICY IF EXISTS "users unfollow" ON public.creator_follows;
CREATE POLICY "users unfollow" ON public.creator_follows FOR DELETE TO authenticated USING (auth.uid() = follower_id);

-- ============ Kuro Reels ============
CREATE TABLE IF NOT EXISTS public.reels (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_by uuid,
  series_ref text NOT NULL,
  series_title text NOT NULL,
  chapter_ref text,
  image_url text NOT NULL,
  caption text NOT NULL DEFAULT '',
  atmosphere text,
  likes integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT ON public.reels TO authenticated;
GRANT SELECT ON public.reels TO anon;
GRANT ALL ON public.reels TO service_role;
ALTER TABLE public.reels ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "reels are public" ON public.reels;
CREATE POLICY "reels are public" ON public.reels FOR SELECT USING (true);
DROP POLICY IF EXISTS "signed in users post reels" ON public.reels;
CREATE POLICY "signed in users post reels" ON public.reels FOR INSERT TO authenticated WITH CHECK (auth.uid() = created_by);

CREATE TABLE IF NOT EXISTS public.reel_likes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reel_id uuid NOT NULL REFERENCES public.reels(id) ON DELETE CASCADE,
  user_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (reel_id, user_id)
);
GRANT SELECT, INSERT, DELETE ON public.reel_likes TO authenticated;
GRANT SELECT ON public.reel_likes TO anon;
GRANT ALL ON public.reel_likes TO service_role;
ALTER TABLE public.reel_likes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "reel likes are public" ON public.reel_likes;
CREATE POLICY "reel likes are public" ON public.reel_likes FOR SELECT USING (true);
DROP POLICY IF EXISTS "users like reels" ON public.reel_likes;
CREATE POLICY "users like reels" ON public.reel_likes FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "users unlike reels" ON public.reel_likes;
CREATE POLICY "users unlike reels" ON public.reel_likes FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- ============ Economy RPCs ============
CREATE OR REPLACE FUNCTION public.purchase_shop_item(_code text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  _uid uuid := auth.uid();
  _item public.shop_items;
  _balance integer;
BEGIN
  IF _uid IS NULL THEN RETURN jsonb_build_object('ok', false, 'error', 'not signed in'); END IF;
  SELECT * INTO _item FROM public.shop_items WHERE code = _code;
  IF _item.id IS NULL THEN RETURN jsonb_build_object('ok', false, 'error', 'unknown item'); END IF;
  IF EXISTS (SELECT 1 FROM public.user_items WHERE user_id = _uid AND item_id = _item.id) THEN
    RETURN jsonb_build_object('ok', false, 'error', 'already owned');
  END IF;
  SELECT credits INTO _balance FROM public.profiles WHERE id = _uid;
  IF COALESCE(_balance, 0) < _item.price THEN
    RETURN jsonb_build_object('ok', false, 'error', 'not enough Kuro Points');
  END IF;
  UPDATE public.profiles SET credits = credits - _item.price WHERE id = _uid;
  INSERT INTO public.user_items (user_id, item_id) VALUES (_uid, _item.id);
  INSERT INTO public.transactions (user_id, amount, reason, meta)
    VALUES (_uid, -_item.price, 'shop_purchase', jsonb_build_object('code', _item.code));
  RETURN jsonb_build_object('ok', true, 'balance', COALESCE(_balance,0) - _item.price);
END;
$$;
GRANT EXECUTE ON FUNCTION public.purchase_shop_item(text) TO authenticated;

CREATE OR REPLACE FUNCTION public.submit_contribution(_kind text, _target_ref text, _target_title text, _payload jsonb, _note text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  _uid uuid := auth.uid();
  _today_count integer;
BEGIN
  IF _uid IS NULL THEN RETURN jsonb_build_object('ok', false, 'error', 'not signed in'); END IF;
  SELECT count(*) INTO _today_count FROM public.contributions
    WHERE user_id = _uid AND created_at > now() - interval '1 day';
  IF _today_count >= 20 THEN RETURN jsonb_build_object('ok', false, 'error', 'daily contribution limit reached'); END IF;
  INSERT INTO public.contributions (user_id, kind, target_ref, target_title, payload, note)
    VALUES (_uid, _kind, _target_ref, _target_title, COALESCE(_payload, '{}'::jsonb), _note);
  UPDATE public.profiles SET contribution_xp = contribution_xp + 10, credits = credits + 5 WHERE id = _uid;
  INSERT INTO public.transactions (user_id, amount, reason, meta)
    VALUES (_uid, 5, 'contribution', jsonb_build_object('kind', _kind, 'ref', _target_ref));
  RETURN jsonb_build_object('ok', true);
END;
$$;
GRANT EXECUTE ON FUNCTION public.submit_contribution(text, text, text, jsonb, text) TO authenticated;

CREATE OR REPLACE FUNCTION public.become_creator(_creator_name text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE _uid uuid := auth.uid();
BEGIN
  IF _uid IS NULL THEN RETURN jsonb_build_object('ok', false, 'error', 'not signed in'); END IF;
  UPDATE public.profiles
     SET is_creator = true,
         creator_name = COALESCE(NULLIF(trim(_creator_name), ''), creator_name, username),
         trust_level = CASE WHEN trust_level IN ('new','trusted_user','contributor','trusted_contributor') THEN 'creator' ELSE trust_level END
   WHERE id = _uid;
  RETURN jsonb_build_object('ok', true);
END;
$$;
GRANT EXECUTE ON FUNCTION public.become_creator(text) TO authenticated;

CREATE OR REPLACE FUNCTION public.toggle_reel_like(_reel_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE _uid uuid := auth.uid(); _liked boolean;
BEGIN
  IF _uid IS NULL THEN RETURN jsonb_build_object('ok', false, 'error', 'not signed in'); END IF;
  IF EXISTS (SELECT 1 FROM public.reel_likes WHERE reel_id = _reel_id AND user_id = _uid) THEN
    DELETE FROM public.reel_likes WHERE reel_id = _reel_id AND user_id = _uid;
    UPDATE public.reels SET likes = GREATEST(0, likes - 1) WHERE id = _reel_id;
    _liked := false;
  ELSE
    INSERT INTO public.reel_likes (reel_id, user_id) VALUES (_reel_id, _uid);
    UPDATE public.reels SET likes = likes + 1 WHERE id = _reel_id;
    _liked := true;
  END IF;
  RETURN jsonb_build_object('ok', true, 'liked', _liked);
END;
$$;
GRANT EXECUTE ON FUNCTION public.toggle_reel_like(uuid) TO authenticated;

-- ============ Shop catalogue seed ============
INSERT INTO public.shop_items (code, name, description, kind, price, rarity, payload) VALUES
  ('badge_kuro_scout', 'Kuro Scout', 'Badge for early explorers of the KuroToon library.', 'badge', 150, 'common', '{"icon":"compass"}'),
  ('badge_night_reader', 'Night Reader', 'For those who read past midnight.', 'badge', 400, 'rare', '{"icon":"moon"}'),
  ('badge_aurora', 'Aurora Devotee', 'Aurora-blue aura around your avatar.', 'badge', 900, 'epic', '{"icon":"sparkles"}'),
  ('badge_galaxy_sage', 'Galaxy Sage', 'Purple galaxy shimmer reserved for deep readers.', 'badge', 1800, 'legendary', '{"icon":"star"}'),
  ('badge_kuro_mythic', 'Mythic Kuro', 'The mascot''s own crest. Extremely rare.', 'badge', 5000, 'mythic', '{"icon":"crown"}'),
  ('theme_midnight', 'Midnight Theme', 'Deepest black reading surface with crimson accents.', 'theme', 300, 'common', '{"accent":"crimson"}'),
  ('theme_aurora', 'Aurora Theme', 'Aurora-blue reader accents and glow.', 'theme', 600, 'rare', '{"accent":"aurora"}'),
  ('theme_galaxy', 'Galaxy Theme', 'Galaxy-purple reader accents and glow.', 'theme', 600, 'rare', '{"accent":"galaxy"}'),
  ('theme_gold', 'Soft Gold Theme', 'Warm gold accents for a premium reading mood.', 'theme', 1200, 'epic', '{"accent":"gold"}'),
  ('boost_double_xp', 'Double Points (7 days)', 'Earn double Kuro Points from daily missions for a week.', 'boost', 1500, 'epic', '{"days":7}'),
  ('frame_crimson', 'Crimson Avatar Frame', 'Animated crimson ring around your avatar.', 'frame', 500, 'rare', '{"ring":"crimson"}'),
  ('frame_mythic', 'Mythic Avatar Frame', 'Shifting mythic gradient frame.', 'frame', 3000, 'mythic', '{"ring":"mythic"}')
ON CONFLICT (code) DO NOTHING;