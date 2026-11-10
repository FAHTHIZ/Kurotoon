
REVOKE ALL ON FUNCTION public.award_points(uuid, integer, text, jsonb) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.transfer_points(uuid, integer, uuid, text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.claim_daily_login() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.transfer_points(uuid, integer, uuid, text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.claim_daily_login() TO authenticated;
