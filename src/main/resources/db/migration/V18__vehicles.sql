-- each vehicle is attached to a tenant (person)
CREATE TABLE vehicle (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancy_uuid UUID NOT NULL,
    property_id UUID NOT NULL,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    plate_state VARCHAR(2),
    color VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (tenancy_uuid) REFERENCES person(uuid),
    FOREIGN KEY (property_id) REFERENCES property(uuid)
);

-- Vehicle policy per property: defines the free allowance and extra vehicle fee
CREATE TABLE vehicle_policy (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    property_id uuid,
    free_vehicle_limit INT NOT NULL DEFAULT 2,
    extra_vehicle_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES property(uuid)
);

CREATE INDEX idx_vehicle_tenancy ON vehicle(tenancy_uuid) WHERE deleted_at IS NULL;
CREATE INDEX idx_vehicle_property ON vehicle(property_id) WHERE deleted_at IS NULL;