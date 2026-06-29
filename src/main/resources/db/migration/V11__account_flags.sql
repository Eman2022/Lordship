-- Adds three operational flags to the account table.
-- no_personal_checks: prevent accepting personal checks from this tenant
-- no_partial_payments: prevent accepting partial rent payments
-- eviction_in_progress: when true, the transaction layer must block all new payments
-- Depends on: account (V9)

ALTER TABLE account
    ADD COLUMN no_personal_checks   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN no_partial_payments  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN eviction_in_progress BOOLEAN NOT NULL DEFAULT FALSE;
