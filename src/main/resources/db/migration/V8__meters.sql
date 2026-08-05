-- ============================================================
-- V8: Meters
-- ============================================================

CREATE TABLE meter_type (
                            code        TEXT PRIMARY KEY,
                            description TEXT
);

CREATE TABLE meter_measurement (
                           code        TEXT PRIMARY KEY,
                           description TEXT
);

INSERT INTO meter_type (code, description) VALUES
                           ('WATER', 'Water utility meter'),
                           ('ENERGY', 'Energy utility meter');

INSERT INTO meter_measurement (code, description) VALUES
                          ('GALLONS', 'Water measurement'),
                          ('KILOWATTHOURS', 'Energy measurement'),
                          ('CUBICFEET', 'Water measurement');

CREATE TABLE meters (
                        uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
                        meter_id      UUID NOT NULL,
                        title         TEXT,
                        description   VARCHAR(255),
                        serial_number VARCHAR(255) UNIQUE,
                        point_x       DOUBLE PRECISION NOT NULL,
                        point_y       DOUBLE PRECISION NOT NULL,
                        utility_type  TEXT NOT NULL REFERENCES meter_type(code),
                        measurement   TEXT NOT NULL REFERENCES meter_measurement(code),
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
                             meter_type TEXT NOT NULL REFERENCES meter_type(code),
                             meter_measurement TEXT NOT NULL REFERENCES meter_measurement(code),
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

