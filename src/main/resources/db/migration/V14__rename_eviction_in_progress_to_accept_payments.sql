-- Rename eviction_in_progress to accept_payments and flip the semantic.
-- accept_payments = TRUE means the account accepts payments (normal state).
-- accept_payments = FALSE means payments are blocked (eviction in progress).
-- Default is TRUE so all existing accounts continue accepting payments.
-- Depends on: V11 (account flags)

ALTER TABLE account RENAME COLUMN eviction_in_progress TO accept_payments;
ALTER TABLE account ALTER COLUMN accept_payments SET DEFAULT TRUE;
UPDATE account SET accept_payments = TRUE WHERE accept_payments = FALSE;
