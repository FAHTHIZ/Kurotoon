
ALTER TABLE public.sync_jobs REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE public.sync_jobs;
