-- ============================================================
-- V8: Meters
-- ============================================================

CREATE TYPE meter_type AS ENUM ('WATER', 'ENERGY');
CREATE TYPE meter_measurement AS ENUM ('GALLONS', 'KILOWATTHOURS', 'CUBICFEET');

CREATE TABLE meters (
                        uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
                        meter_id      UUID NOT NULL,
                        title         TEXT,
                        description   VARCHAR(255),
                        serial_number VARCHAR(255) UNIQUE,
                        point_x       DOUBLE PRECISION NOT NULL,
                        point_y       DOUBLE PRECISION NOT NULL,
                        utility_type  meter_type NOT NULL,
                        measurement   meter_measurement NOT NULL,
                        installed_at  DATE,
                        created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                        deleted_at    TIMESTAMPTZ,
                        FOREIGN KEY (meter_id) REFERENCES lot(uuid)
);

CREATE TABLE meter_relationship (
                                    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
                                    parent_meter UUID NOT NULL,
                                    is_master_meter BOOL NOT NULL,
                                    FOREIGN KEY (parent_meter) REFERENCES meters(uuid)
);

CREATE TABLE meter_reads (
                             uuid UUID PRIMARY KEY DEFAULT uuidv7(),
                             targeted_meter UUID NOT NULL,
                             meter_type meter_type NOT NULL,
                             meter_measurement meter_measurement NOT NULL,
                             meter_amount INT NOT NULL,
                             FOREIGN KEY (targeted_meter) REFERENCES meters(uuid)
);

-- Used in the MeterBills package
CREATE TABLE meter_billing (
                               uuid UUID PRIMARY KEY DEFAULT uuidv7(),
                               billed_meter UUID NOT NULL,
                               billing_amount INT NOT NULL,
                               FOREIGN KEY (billed_meter) REFERENCES meters(uuid)
);

ALTER TABLE meters
    ADD COLUMN is_master_meter BOOL NOT NULL;

-- Enforces one meterId for a given meter
CREATE UNIQUE INDEX uix_meters_lot_active
    ON meters (meter_id)
    WHERE deleted_at IS NULL;

-- For creating/using queries
CREATE INDEX idx_meters_utility_type ON meters(utility_type);
CREATE INDEX idx_meters_measurement ON meters(measurement);

