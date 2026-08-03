-- ============================================================
-- V5: Lots
-- ============================================================

CREATE TABLE lot (
                     uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
                     property_id   UUID NOT NULL,
                     lot_number    TEXT NOT NULL,        -- human-facing id; numeric OR lettered (e.g. "DF"); mutable
                     description   TEXT,
                     notes         TEXT,                 -- notes about the LOT, never the tenancy
                     sort_order    INT,                  -- manual ordering for the map/menu view
                     created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                     deleted_at    TIMESTAMPTZ,
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
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         FOREIGN KEY (lot_id) REFERENCES lot(uuid)
);