INSERT INTO render_template (id, name)
VALUES ('b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10', 'invoice-standard')
ON CONFLICT (id) DO NOTHING;

INSERT INTO render_template (id, name)
VALUES ('6c6a1a44-4f0b-4a8a-8b8e-2b1e9c9c2a11', 'shipping-label')
ON CONFLICT (id) DO NOTHING;

INSERT INTO render_template (id, name)
VALUES ('9e2b6f2a-2d8a-4b1a-9f3d-7a1c5e6b8c12', 'certificate-of-completion')
ON CONFLICT (id) DO NOTHING;

-- Backfill jobs created before retry scheduling and processing leases were introduced.
UPDATE job
SET scheduled_at = updated_at
WHERE status = 'QUEUED'
  AND scheduled_at IS NULL;

UPDATE job
SET scheduled_at = updated_at + INTERVAL '10 minutes'
WHERE status = 'PROCESSING'
  AND scheduled_at IS NULL;
