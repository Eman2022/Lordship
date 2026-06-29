-- Adds exempt_from_late_fees flag to account.
-- When true, the billing engine must not apply late fees to this account.
-- Defaults to false — late fees apply to all accounts by default.
-- Depends on: account (V9)

ALTER TABLE account
    ADD COLUMN exempt_from_late_fees BOOLEAN NOT NULL DEFAULT FALSE;
