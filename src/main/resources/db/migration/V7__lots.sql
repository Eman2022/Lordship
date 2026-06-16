-- Lots represent the individual rented land parcels within a property.
-- Tenants OWN their home but RENT the lot beneath it (RCW 59.20).
-- Depends on: property (V1)

CREATE TABLE lot (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    property_code VARCHAR(5) NOT NULL,
    lot_number VARCHAR(20) NOT NULL,
    lot_address TEXT,
    lot_size_sqft NUMERIC(10,2),
    monthly_rent NUMERIC(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (property_code) REFERENCES property(property_code)
);

-- Only one active lot with a given number per property
CREATE UNIQUE INDEX uq_lot_number_property_active
    ON lot (property_code, lot_number) WHERE deleted_at IS NULL;

CREATE INDEX idx_lot_property_code ON lot(property_code) WHERE deleted_at IS NULL;
