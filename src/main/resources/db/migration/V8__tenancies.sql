-- A tenancy represents the legal rental relationship between a tenant and a lot.
-- Tenants OWN their home but RENT the lot beneath it (RCW 59.20).
-- Tracks rent increase history for 5% annual cap compliance and 90-day notice requirements.
-- Depends on: lot (V7), person (V1)

CREATE TABLE tenancy (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    lot_id UUID NOT NULL,
    primary_tenant_id UUID NOT NULL,
    lease_start DATE NOT NULL,
    lease_end DATE,
    monthly_rent NUMERIC(10,2) NOT NULL,
    rent_last_increased DATE,
    notice_given_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (lot_id) REFERENCES lot(uuid),
    FOREIGN KEY (primary_tenant_id) REFERENCES person(uuid)
);

-- Only one active tenancy per lot at a time
CREATE UNIQUE INDEX uq_tenancy_lot_active
    ON tenancy (lot_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_tenancy_lot_id ON tenancy(lot_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenancy_tenant_id ON tenancy(primary_tenant_id) WHERE deleted_at IS NULL;
