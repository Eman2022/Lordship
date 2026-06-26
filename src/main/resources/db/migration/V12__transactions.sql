-- Transaction ledger for tenant accounts.
-- Charges are stored as positive amounts; credits and payments are semantically negative
-- (applied via sign logic in the application layer when computing balance).
-- billed = true means the transaction is locked and cannot be deleted or edited.
-- Depends on: account (V9)

CREATE TABLE transaction (
    uuid             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL REFERENCES account(uuid),
    type             VARCHAR(50) NOT NULL,
    amount           NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    description      TEXT,
    billing_period   DATE NOT NULL,
    posted_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    billed           BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP
);

CREATE INDEX idx_transaction_account_id ON transaction(account_id);
CREATE INDEX idx_transaction_billing_period ON transaction(billing_period);
