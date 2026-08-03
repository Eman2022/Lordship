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