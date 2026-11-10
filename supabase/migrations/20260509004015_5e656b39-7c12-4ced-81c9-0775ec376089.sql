
-- 1. Extend profiles
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS credits integer NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS streak integer NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_login_at timestamptz,
  ADD COLUMN IF NOT EXISTS bio text,
  ADD COLUMN IF NOT EXISTS active_badge text;

-- 2. Transactions
CREATE TABLE IF NOT EXISTS public.transactions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  amount integer NOT NULL,
  reason text NOT NULL,
  meta jsonb DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own transactions select" ON public.transactions
  FOR SELECT USING (auth.uid() = user_id);

-- 3. Stickers catalog
CREATE TABLE IF NOT EXISTS public.stickers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  emoji text NOT NULL,
  image_url text,
  price integer NOT NULL DEFAULT 0,
  rarity text NOT NULL DEFAULT 'common',
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.stickers ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Stickers public read" ON public.stickers FOR SELECT USING (true);

-- 4. User stickers
CREATE TABLE IF NOT EXISTS public.user_stickers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  sticker_id uuid NOT NULL REFERENCES public.stickers(id) ON DELETE CASCADE,
  acquired_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id, sticker_id)
);
ALTER TABLE public.user_stickers ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own stickers select" ON public.user_stickers
  FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Own stickers insert" ON public.user_stickers
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- 5. Achievements
CREATE TABLE IF NOT EXISTS public.achievements (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text NOT NULL UNIQUE,
  title text NOT NULL,
  description text,
  icon text,
  reward integer NOT NULL DEFAULT 0
);
ALTER TABLE public.achievements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Achievements public read" ON public.achievements FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.user_achievements (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  achievement_id uuid NOT NULL REFERENCES public.achievements(id) ON DELETE CASCADE,
  unlocked_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id, achievement_id)
);
ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own achievements select" ON public.user_achievements
  FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Own achievements insert" ON public.user_achievements
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- 6. Daily missions
CREATE TABLE IF NOT EXISTS public.daily_missions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text NOT NULL UNIQUE,
  title text NOT NULL,
  description text,
  target integer NOT NULL DEFAULT 1,
  reward integer NOT NULL DEFAULT 10
);
ALTER TABLE public.daily_missions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Daily missions public read" ON public.daily_missions FOR SELECT USING (true);

CREATE TABLE IF NOT EXISTS public.user_missions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  mission_id uuid NOT NULL REFERENCES public.daily_missions(id) ON DELETE CASCADE,
  progress integer NOT NULL DEFAULT 0,
  completed boolean NOT NULL DEFAULT false,
  claimed boolean NOT NULL DEFAULT false,
  day date NOT NULL DEFAULT (now() AT TIME ZONE 'utc')::date,
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id, mission_id, day)
);
ALTER TABLE public.user_missions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own missions select" ON public.user_missions
  FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Own missions insert" ON public.user_missions
  FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Own missions update" ON public.user_missions
  FOR UPDATE USING (auth.uid() = user_id);

-- 7. Tips to uploaders
CREATE TABLE IF NOT EXISTS public.tips (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  from_user uuid NOT NULL,
  to_user uuid NOT NULL,
  series_id uuid,
  amount integer NOT NULL CHECK (amount > 0),
  message text,
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.tips ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Tips visible to participants" ON public.tips
  FOR SELECT USING (auth.uid() = from_user OR auth.uid() = to_user);
CREATE POLICY "Tips insert by sender" ON public.tips
  FOR INSERT WITH CHECK (auth.uid() = from_user);

-- 8. award_points RPC (security definer; safe to grant points & log)
CREATE OR REPLACE FUNCTION public.award_points(_user_id uuid, _amount integer, _reason text, _meta jsonb DEFAULT '{}'::jsonb)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  new_balance integer;
BEGIN
  UPDATE public.profiles SET credits = credits + _amount WHERE id = _user_id RETURNING credits INTO new_balance;
  INSERT INTO public.transactions(user_id, amount, reason, meta) VALUES (_user_id, _amount, _reason, _meta);
  RETURN new_balance;
END;
$$;

-- 9. transfer_points (for tips)
CREATE OR REPLACE FUNCTION public.transfer_points(_to_user uuid, _amount integer, _series_id uuid, _message text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  sender uuid := auth.uid();
  bal integer;
BEGIN
  IF sender IS NULL THEN RAISE EXCEPTION 'auth required'; END IF;
  IF _amount <= 0 THEN RAISE EXCEPTION 'invalid amount'; END IF;
  IF sender = _to_user THEN RAISE EXCEPTION 'cannot tip yourself'; END IF;
  SELECT credits INTO bal FROM public.profiles WHERE id = sender;
  IF bal < _amount THEN RAISE EXCEPTION 'insufficient points'; END IF;
  UPDATE public.profiles SET credits = credits - _amount WHERE id = sender;
  UPDATE public.profiles SET credits = credits + _amount WHERE id = _to_user;
  INSERT INTO public.transactions(user_id, amount, reason, meta) VALUES (sender, -_amount, 'tip_sent', jsonb_build_object('to', _to_user, 'series', _series_id));
  INSERT INTO public.transactions(user_id, amount, reason, meta) VALUES (_to_user, _amount, 'tip_received', jsonb_build_object('from', sender, 'series', _series_id));
  INSERT INTO public.tips(from_user, to_user, series_id, amount, message) VALUES (sender, _to_user, _series_id, _amount, _message);
END;
$$;

-- 10. claim_daily_login
CREATE OR REPLACE FUNCTION public.claim_daily_login()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  uid uuid := auth.uid();
  prof public.profiles%ROWTYPE;
  today date := (now() AT TIME ZONE 'utc')::date;
  reward integer := 20;
  new_streak integer := 1;
BEGIN
  IF uid IS NULL THEN RAISE EXCEPTION 'auth required'; END IF;
  SELECT * INTO prof FROM public.profiles WHERE id = uid;
  IF prof.last_login_at IS NOT NULL AND (prof.last_login_at AT TIME ZONE 'utc')::date = today THEN
    RETURN jsonb_build_object('claimed', false, 'streak', prof.streak, 'credits', prof.credits);
  END IF;
  IF prof.last_login_at IS NOT NULL AND (prof.last_login_at AT TIME ZONE 'utc')::date = today - 1 THEN
    new_streak := prof.streak + 1;
  END IF;
  reward := 20 + LEAST(new_streak, 7) * 5;
  UPDATE public.profiles SET credits = credits + reward, streak = new_streak, last_login_at = now() WHERE id = uid;
  INSERT INTO public.transactions(user_id, amount, reason, meta) VALUES (uid, reward, 'daily_login', jsonb_build_object('streak', new_streak));
  RETURN jsonb_build_object('claimed', true, 'streak', new_streak, 'reward', reward);
END;
$$;

-- 11. Seed stickers
INSERT INTO public.stickers (name, emoji, price, rarity) VALUES
  ('Sparkle Eyes', '✨', 0, 'common'),
  ('Heart Burst', '💖', 0, 'common'),
  ('Fire', '🔥', 0, 'common'),
  ('Crying Laugh', '😂', 0, 'common'),
  ('Shocked', '😱', 50, 'rare'),
  ('Sakura', '🌸', 80, 'rare'),
  ('Crown', '👑', 200, 'epic'),
  ('Galaxy', '🌌', 350, 'epic'),
  ('Dragon', '🐉', 500, 'legendary'),
  ('Phoenix', '🔥🦅', 800, 'legendary')
ON CONFLICT DO NOTHING;

-- 12. Seed achievements
INSERT INTO public.achievements (code, title, description, icon, reward) VALUES
  ('first_upload', 'First Upload', 'Upload your first chapter', '📤', 100),
  ('seven_day_reader', '7-Day Reader', 'Read manga 7 days in a row', '📚', 150),
  ('night_reader', 'Night Reader', 'Read between midnight and 4am', '🌙', 50),
  ('hundred_chapters', '100 Chapters Read', 'Read 100 chapters total', '💯', 300),
  ('first_bookmark', 'Collector', 'Bookmark your first series', '🔖', 25),
  ('first_tip', 'Generous Soul', 'Tip your first uploader', '❤️', 50)
ON CONFLICT (code) DO NOTHING;

-- 13. Seed daily missions
INSERT INTO public.daily_missions (code, title, description, target, reward) VALUES
  ('read_5_chapters', 'Read 5 chapters', 'Finish 5 chapters today', 5, 30),
  ('bookmark_one', 'Bookmark a series', 'Add 1 series to your library', 1, 15),
  ('leave_reaction', 'Leave a reaction', 'React with a sticker on any chapter', 1, 10)
ON CONFLICT (code) DO NOTHING;
