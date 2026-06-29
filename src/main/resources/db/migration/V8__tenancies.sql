-- Correct rent from lease table?
-- Foreign key for accountNumber (V9_Accounts)
-- Lots may have two tenancies in certain circumstances
CREATE TABLE tenancy (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    lot_id UUID NOT NULL,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (lot_id) REFERENCES lot(uuid) -- references lot
);

-- Enforces one tenancyId per lot
CREATE UNIQUE INDEX uix_tenancy_lot_active
    ON tenancy (lot_id)
    WHERE end_date IS NULL AND deleted_at IS NULL;


-- Foreign key for account rent?
-- Add timestamps for tenant?
CREATE TABLE tenant (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancy_id UUID NOT NULL,
    person_id UUID NOT NULL,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (person_id) REFERENCES person(uuid),
    FOREIGN KEY (tenancy_id) REFERENCES tenancy(uuid)
);

-- Prevents duplicate permissions
CREATE UNIQUE INDEX uix_permission_name ON permission(permission_name);


-- Sets an update timestamp when a row information is updated
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tenancy_updated_at
    BEFORE UPDATE ON tenancy
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();