-- One account per tenancy. Tracks billing, balance, and payment status.
-- account_status values: ACTIVE, DELINQUENT, CLOSED
-- Depends on: tenancy (V8)

CREATE TABLE account (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancy_id UUID NOT NULL,
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    balance NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    autopay_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (tenancy_id) REFERENCES tenancy(uuid)
);

-- One active account per tenancy (1:1 enforced at DB level)
CREATE UNIQUE INDEX uq_account_tenancy_active
    ON account (tenancy_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_account_tenancy_id ON account(tenancy_id) WHERE deleted_at IS NULL;