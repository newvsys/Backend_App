-- Adds a capture-audit trail to `payments` and a uniqueness guarantee on
-- payment_provider_order_id to prevent duplicate PaymentEO rows being created
-- for the same Razorpay order under concurrent webhook/verify retries.
--
-- Run manually against the target database (no migration framework such as
-- Flyway/Liquibase is wired into this project - see other root-level *.sql
-- scripts for the same convention).

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS capture_status VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS captured_at TIMESTAMP NULL;

-- Deduplicate any pre-existing rows before adding the unique constraint (keeps
-- the earliest row per payment_provider_order_id). Safe to skip if you already
-- know the table has no duplicates.
-- DELETE p1 FROM payments p1
--   INNER JOIN payments p2
--     ON p1.payment_provider_order_id = p2.payment_provider_order_id
--    AND p1.payment_id > p2.payment_id;

ALTER TABLE payments
    ADD CONSTRAINT uq_payments_provider_order_id UNIQUE (payment_provider_order_id);

