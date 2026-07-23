-- Moves operational flags from account to tenancy.
-- These flags describe rules about the lease relationship, not the financial account.
-- Depends on: account (V9), tenancy (V8), account_flags (V11), V14, V15

ALTER TABLE tenancy
    ADD COLUMN no_personal_checks   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN no_partial_payments  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN accept_payments      BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN exempt_from_late_fees BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE account
    DROP COLUMN no_personal_checks,
    DROP COLUMN no_partial_payments,
    DROP COLUMN accept_payments,
    DROP COLUMN exempt_from_late_fees;
