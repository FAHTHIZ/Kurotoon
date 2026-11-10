
-- Chapter unlock ledger
CREATE TABLE public.chapter_unlocks (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  chapter_id uuid NOT NULL,
  cost integer NOT NULL DEFAULT 0,
  unlocked_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE (user_id, chapter_id)
);

CREATE INDEX idx_chapter_unlocks_user ON public.chapter_unlocks(user_id);
CREATE INDEX idx_chapter_unlocks_chapter ON public.chapter_unlocks(chapter_id);

ALTER TABLE public.chapter_unlocks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Own unlocks select"
  ON public.chapter_unlocks FOR SELECT
  USING (auth.uid() = user_id);

-- INSERT/UPDATE/DELETE intentionally only via SECURITY DEFINER RPC

-- Determine if a chapter is among the latest 2 of its series (free)
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
  SELECT COALESCE((SELECT rnk <= 2 FROM ranked WHERE id = _chapter_id), false);
$$;

-- Unlock a chapter using Kuro Points
CREATE OR REPLACE FUNCTION public.unlock_chapter(_chapter_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  uid uuid := auth.uid();
  cost integer := 30;
  bal integer;
  is_free boolean;
  already boolean;
BEGIN
  IF uid IS NULL THEN RAISE EXCEPTION 'auth required'; END IF;

  SELECT public.is_chapter_free(_chapter_id) INTO is_free;
  IF is_free THEN
    RETURN jsonb_build_object('unlocked', true, 'free', true, 'cost', 0);
  END IF;

  SELECT EXISTS (
    SELECT 1 FROM public.chapter_unlocks
    WHERE user_id = uid AND chapter_id = _chapter_id
  ) INTO already;
  IF already THEN
    RETURN jsonb_build_object('unlocked', true, 'already', true, 'cost', 0);
  END IF;

  SELECT credits INTO bal FROM public.profiles WHERE id = uid;
  IF bal IS NULL OR bal < cost THEN
    RAISE EXCEPTION 'insufficient points';
  END IF;

  UPDATE public.profiles SET credits = credits - cost WHERE id = uid;
  INSERT INTO public.chapter_unlocks(user_id, chapter_id, cost) VALUES (uid, _chapter_id, cost);
  INSERT INTO public.transactions(user_id, amount, reason, meta)
    VALUES (uid, -cost, 'chapter_unlock', jsonb_build_object('chapter_id', _chapter_id));

  RETURN jsonb_build_object('unlocked', true, 'cost', cost,
                            'balance', (SELECT credits FROM public.profiles WHERE id = uid));
END;
$$;
