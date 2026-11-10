REVOKE EXECUTE ON FUNCTION public.claim_sync_jobs(int) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.claim_sync_jobs(int) TO service_role;