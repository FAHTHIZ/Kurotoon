DO $$
DECLARE jid bigint;
BEGIN
  FOR jid IN SELECT jobid FROM cron.job
             WHERE jobname IN ('kurotoon-sync-tick', 'kurotoon-refresh-stale-pages')
  LOOP
    PERFORM cron.unschedule(jid);
  END LOOP;
END $$;

SELECT cron.schedule(
  'kurotoon-sync-tick',
  '* * * * *',
  $cmd$
  SELECT net.http_post(
    url := 'https://project--eef62591-ae69-4f91-bf3b-04bb3e69bd43-dev.lovable.app/api/public/hooks/sync-tick',
    headers := '{"Content-Type":"application/json"}'::jsonb,
    body := '{}'::jsonb,
    timeout_milliseconds := 25000
  );
  $cmd$
);

SELECT cron.schedule(
  'kurotoon-refresh-stale-pages',
  '*/5 * * * *',
  $cmd$
  SELECT net.http_post(
    url := 'https://project--eef62591-ae69-4f91-bf3b-04bb3e69bd43-dev.lovable.app/api/public/hooks/refresh-stale-pages',
    headers := '{"Content-Type":"application/json"}'::jsonb,
    body := '{}'::jsonb,
    timeout_milliseconds := 25000
  );
  $cmd$
);