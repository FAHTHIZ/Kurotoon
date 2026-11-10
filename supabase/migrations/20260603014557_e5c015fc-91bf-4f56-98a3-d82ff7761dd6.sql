WITH placeholder AS (
  SELECT id FROM public.series WHERE source_id IS NULL
), placeholder_chapters AS (
  SELECT id FROM public.chapters WHERE series_id IN (SELECT id FROM placeholder)
)
DELETE FROM public.pages WHERE chapter_id IN (SELECT id FROM placeholder_chapters);

DELETE FROM public.reading_progress WHERE series_id IN (SELECT id FROM public.series WHERE source_id IS NULL);
DELETE FROM public.bookmarks WHERE series_id IN (SELECT id FROM public.series WHERE source_id IS NULL);
DELETE FROM public.chapter_unlocks WHERE chapter_id IN (SELECT id FROM public.chapters WHERE series_id IN (SELECT id FROM public.series WHERE source_id IS NULL));
DELETE FROM public.chapters WHERE series_id IN (SELECT id FROM public.series WHERE source_id IS NULL);
DELETE FROM public.tips WHERE series_id IN (SELECT id FROM public.series WHERE source_id IS NULL);
DELETE FROM public.series WHERE source_id IS NULL;