ALTER TABLE retry_jobs
    ADD COLUMN request_payload TEXT;

UPDATE retry_jobs
SET request_payload = COALESCE((CAST(payload AS jsonb) -> 'data')::text, '{}')
WHERE request_payload IS NULL;

ALTER TABLE retry_jobs
    ALTER COLUMN request_payload SET NOT NULL;
