-- ============================================================
-- V7: Vehicles & Pets
-- ============================================================

CREATE TABLE vehicle (
                         uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
                         tenancy_uuid  UUID NOT NULL,
                         make          VARCHAR(100),
                         model         VARCHAR(100),
                         year          INT,
                         plate_number  VARCHAR(20) NOT NULL,
                         plate_state   VARCHAR(2),
                         color         VARCHAR(50),
                         notes         TEXT,
                         created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at    TIMESTAMP,
                         FOREIGN KEY (tenancy_uuid)  REFERENCES tenancy(uuid)
);

CREATE TABLE vehicle_policy (
                                uuid               UUID PRIMARY KEY DEFAULT uuidv7(),
                                property_uuid      UUID NOT NULL UNIQUE,
                                free_vehicle_limit INT NOT NULL DEFAULT 2,
                                extra_vehicle_fee  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
                                notes              TEXT,
                                created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (property_uuid) REFERENCES property(uuid)
);

CREATE INDEX idx_vehicle_tenancy  ON vehicle(tenancy_uuid)  WHERE deleted_at IS NULL;



-- ── Pet ──────────────────────────────────────────────────────────────────────

CREATE TABLE pet (
                     uuid       UUID PRIMARY KEY DEFAULT uuidv7(),
                     pet_name   VARCHAR(120),
                     pet_type   VARCHAR(120),
                     pet_breed  VARCHAR(120),
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                     deleted_at TIMESTAMP
);