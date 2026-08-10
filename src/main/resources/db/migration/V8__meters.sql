-- ============================================================
-- V8: Meters
-- ============================================================

CREATE TYPE meter_type AS ENUM ('WATER', 'ENERGY');
CREATE TYPE meter_measurement AS ENUM ('GAL', 'KWH', 'CBF');

CREATE TABLE meters (
                        uuid             UUID PRIMARY KEY DEFAULT uuidv7(),
                        meter_id         UUID NOT NULL,
                        title            TEXT,
                        description      VARCHAR(255),
                        serial_number    VARCHAR(255) UNIQUE,
                        point_x          DOUBLE PRECISION NOT NULL,
                        point_y          DOUBLE PRECISION NOT NULL,
                        utility_type     meter_type NOT NULL,
                        measurement      meter_measurement NOT NULL,
                        installed_at     DATE,
                        created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                        deleted_at       TIMESTAMPTZ,
                        is_master_meter  BOOL NOT NULL,
                        FOREIGN KEY (meter_id) REFERENCES lot(uuid)
);

CREATE TABLE meter_relationship (
                                    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
                                    parent_meter UUID NOT NULL,
                                    child_meter UUID,
                                    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
                                    effective_to DATE, -- in case a relationship were to change
                                    FOREIGN KEY (parent_meter) REFERENCES meters(uuid),
                                    FOREIGN KEY (child_meter) REFERENCES meters(uuid),
                                    CHECK (child_meter <> parent_meter)
);

CREATE TABLE meter_reads (
                             uuid UUID PRIMARY KEY DEFAULT uuidv7(),
                             targeted_meter UUID NOT NULL,
                             meter_amount INT NOT NULL,
                             read_at TIMESTAMPTZ NOT NULL,
                             is_estimated BOOLEAN NOT NULL DEFAULT FALSE,
                             rollover_count INT NOT NULL DEFAULT 0,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             FOREIGN KEY (targeted_meter) REFERENCES meters(uuid)
);

-- Used in the MeterBills package
CREATE TABLE meter_billing (
                               uuid             UUID PRIMARY KEY DEFAULT uuidv7(),
                               billed_meter     UUID NOT NULL,
                               billed_amount    INT NOT NULL,   -- charge displayed on a utility bill
                               rate_amount      NUMERIC(10,4) NOT NULL,  -- a calculated rate for water/energy
                               rate_unit        meter_measurement NOT NULL,
                               period_start     DATE NOT NULL,
                               period_end       DATE NOT NULL,
                               created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                               deleted_at       TIMESTAMPTZ,
                               FOREIGN KEY (billed_meter) REFERENCES meters(uuid),
                               CHECK (period_end > period_start)
); -- Establish ability for both RUBS and Submetering

-- Enforces one meterId for a given meter
CREATE UNIQUE INDEX uix_meters_lot_active
    ON meters (meter_id)
    WHERE deleted_at IS NULL;

-- For creating/using queries
CREATE INDEX idx_meters_utility_type ON meters(utility_type);
CREATE INDEX idx_meters_measurement ON meters(measurement);

CREATE UNIQUE INDEX uix_meter_relationship_child_active
    ON meter_relationship (child_meter)
    WHERE effective_to IS NULL;