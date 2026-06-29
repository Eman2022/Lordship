-- Two changes to the transaction table:
-- 1. Replace CHECK (amount > 0) with a constraint that allows negative amounts
--    specifically for BALANCE_ADJUSTMENT type (signed adjustments).
--    All other types must remain positive.
-- 2. Remove the billed column. Immutability is now enforced by billing period
--    finalization: transactions in a closed (past) billing period cannot be deleted.
-- Depends on: V12 (transactions)

ALTER TABLE transaction DROP CONSTRAINT transaction_amount_check;
ALTER TABLE transaction ADD CONSTRAINT transaction_amount_check
    CHECK (amount <> 0 AND (amount > 0 OR type = 'BALANCE_ADJUSTMENT'));

ALTER TABLE transaction DROP COLUMN billed;
