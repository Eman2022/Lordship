-- Lots: physical spaces within a property that a tenancy occupies.
-- One property has many lots; one lot has many tenancies over time (see V8).
-- Created in dependency order: lot -> lot_map.

-- ── Lot ──────────────────────────────────────────────────────────────
-- PK is uuid: lot_number can be renamed and everything stays tied to the
-- same uuid. Renames are captured by the audit log, so the original name
-- is not stored separately.
CREATE TABLE lot (
                     uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
                     property_id   UUID NOT NULL,
                     lot_number    TEXT NOT NULL,        -- human-facing id; numeric OR lettered (e.g. "DF"); mutable
                     description   TEXT,
                     notes         TEXT,                 -- notes about the LOT, never the tenancy
                     sort_order    INT,                  -- manual ordering for the map/menu view
                     created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                     deleted_at    TIMESTAMP,
                     FOREIGN KEY (property_id) REFERENCES property(uuid)
);

-- NOT unique: real park data has duplicate labels (e.g. two "DF" lots in one
-- park). Duplicates are flagged at the application layer, not rejected here.
CREATE INDEX idx_lot_number_per_property
    ON lot (property_id, LOWER(lot_number)) WHERE deleted_at IS NULL;
CREATE INDEX idx_lot_property ON lot (property_id) WHERE deleted_at IS NULL;

-- ── Lot map geometry ─────────────────────────────────────────────────
-- 1:1 with lot. `vertices` is a JSONB list of Vector2s describing the lot's
-- footprint polygon on the park map: [{"x":12.5,"y":40.0}, ...].
-- Early-stage: intentionally minimal, expand when the map view is built.
CREATE TABLE lot_map (
                         lot_id     UUID PRIMARY KEY,
                         vertices   JSONB NOT NULL,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         FOREIGN KEY (lot_id) REFERENCES lot(uuid)
);

-- ── Permissions ──────────────────────────────────────────────────────
-- V5's "Admin gets all" grant ran before these existed, so grant explicitly.
INSERT INTO permission (uuid, permission_name) VALUES
                                                   (uuidv7(), 'lots:view'),
                                                   (uuidv7(), 'lots:edit'),
                                                   (uuidv7(), 'lots:create'),
                                                   (uuidv7(), 'lots:delete');

INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin' AND p.permission_name LIKE 'lots:%';