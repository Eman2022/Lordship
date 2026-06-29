-- Rename balance to balance_cached to reflect that this is a computed/cached value
-- not a source of truth. The real balance is derived from the transaction ledger.
-- Depends on: account (V9)

ALTER TABLE account RENAME COLUMN balance TO balance_cached;
