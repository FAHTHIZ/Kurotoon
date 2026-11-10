-- Chapter comments with optional sticker, plus sticker reactions on comments
CREATE TABLE public.chapter_comments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  chapter_id uuid NOT NULL REFERENCES public.chapters(id) ON DELETE CASCADE,
  user_id uuid NOT NULL,
  body text,
  sticker_id uuid REFERENCES public.stickers(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT body_or_sticker CHECK (
    (body IS NOT NULL AND length(btrim(body)) > 0) OR sticker_id IS NOT NULL
  )
);
CREATE INDEX idx_chapter_comments_chapter ON public.chapter_comments(chapter_id, created_at DESC);

ALTER TABLE public.chapter_comments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Comments public read" ON public.chapter_comments FOR SELECT USING (true);
CREATE POLICY "Comments insert own" ON public.chapter_comments FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Comments delete own" ON public.chapter_comments FOR DELETE USING (auth.uid() = user_id);

-- Validate the user actually owns the sticker they post
CREATE OR REPLACE FUNCTION public.validate_comment_sticker()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NEW.sticker_id IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM public.user_stickers
      WHERE user_id = NEW.user_id AND sticker_id = NEW.sticker_id
    ) THEN
      RAISE EXCEPTION 'You do not own this sticker';
    END IF;
  END IF;
  RETURN NEW;
END; $$;

CREATE TRIGGER trg_validate_comment_sticker
BEFORE INSERT ON public.chapter_comments
FOR EACH ROW EXECUTE FUNCTION public.validate_comment_sticker();

-- Sticker reactions on comments
CREATE TABLE public.comment_reactions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  comment_id uuid NOT NULL REFERENCES public.chapter_comments(id) ON DELETE CASCADE,
  user_id uuid NOT NULL,
  sticker_id uuid NOT NULL REFERENCES public.stickers(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (comment_id, user_id, sticker_id)
);
CREATE INDEX idx_comment_reactions_comment ON public.comment_reactions(comment_id);

ALTER TABLE public.comment_reactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Reactions public read" ON public.comment_reactions FOR SELECT USING (true);
CREATE POLICY "Reactions insert own" ON public.comment_reactions FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Reactions delete own" ON public.comment_reactions FOR DELETE USING (auth.uid() = user_id);

CREATE OR REPLACE FUNCTION public.validate_reaction_sticker()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM public.user_stickers
    WHERE user_id = NEW.user_id AND sticker_id = NEW.sticker_id
  ) THEN
    RAISE EXCEPTION 'You do not own this sticker';
  END IF;
  RETURN NEW;
END; $$;

CREATE TRIGGER trg_validate_reaction_sticker
BEFORE INSERT ON public.comment_reactions
FOR EACH ROW EXECUTE FUNCTION public.validate_reaction_sticker();