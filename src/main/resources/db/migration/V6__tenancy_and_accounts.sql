-- ============================================================
-- V6: Tenancy
-- ============================================================

CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE tenancy ( -- needs added: lease anniversary
                         uuid                  UUID PRIMARY KEY DEFAULT uuidv7(),
                         lot_id                UUID NOT NULL,
                         start_date            DATE,
                         end_date              DATE,
                         no_personal_checks    BOOLEAN NOT NULL DEFAULT FALSE,
                         no_partial_payments   BOOLEAN NOT NULL DEFAULT FALSE,
                         accept_payments       BOOLEAN NOT NULL DEFAULT TRUE,
                         exempt_from_late_fees BOOLEAN NOT NULL DEFAULT FALSE,
                         created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                         deleted_at            TIMESTAMPTZ,
                         FOREIGN KEY (lot_id) REFERENCES lot(uuid)
);

CREATE UNIQUE INDEX uix_tenancy_lot_active
    ON tenancy(lot_id) WHERE end_date IS NULL AND deleted_at IS NULL;

CREATE TRIGGER trg_tenancy_updated_at
    BEFORE UPDATE ON tenancy
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE tenant (
                        uuid       UUID PRIMARY KEY DEFAULT uuidv7(),
                        tenancy_id UUID NOT NULL,
                        person_id  UUID NOT NULL,
                        start_date DATE,
                        end_date   DATE,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        deleted_at TIMESTAMPTZ,
                        FOREIGN KEY (person_id)  REFERENCES person(uuid),
                        FOREIGN KEY (tenancy_id) REFERENCES tenancy(uuid)
);


-- ── Account ───────────────────────────────────────────────────────────────────

CREATE TABLE account (
                         uuid            UUID PRIMARY KEY DEFAULT uuidv7(),
                         tenancy_id      UUID NOT NULL,
                         account_status  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                         balance_cached  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
                         autopay_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                         notes           TEXT,
                         created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                         deleted_at      TIMESTAMPTZ,
                         FOREIGN KEY (tenancy_id) REFERENCES tenancy(uuid)
);

CREATE UNIQUE INDEX uq_account_tenancy_active
    ON account(tenancy_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_account_tenancy_id ON account(tenancy_id) WHERE deleted_at IS NULL;


-- ── Transaction ───────────────────────────────────────────────────────────────

CREATE TABLE transaction ( --TODO:  needs nullable field: a uuid for the batch billing process
                             uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
                             account_id     UUID NOT NULL REFERENCES account(uuid),
                             type           VARCHAR(50) NOT NULL,
                             amount         NUMERIC(14,2) NOT NULL,
                             description    TEXT,
                             billing_period DATE NOT NULL,
                             posted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                             deleted_at     TIMESTAMPTZ,
                             CONSTRAINT transaction_amount_check
                                 CHECK (amount <> 0 AND (amount > 0 OR type = 'BALANCE_ADJUSTMENT'))
);

CREATE INDEX idx_transaction_account_id     ON transaction(account_id);
CREATE INDEX idx_transaction_billing_period ON transaction(billing_period);