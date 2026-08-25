-- ============================================================
-- V5: Lots
-- ============================================================

CREATE TABLE lot ( --TODO: add square footage, also, add a MFH (manufactured home) table to know what exists on each lot
                     uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
                     property_id   UUID NOT NULL,
                     is_rentable   BOOLEAN NOT NULL DEFAULT TRUE,
                     not_rentable_reason TEXT,
                     lot_number    TEXT NOT NULL,        -- human-facing id; numeric OR lettered (e.g. "DF"); mutable
                     lot_address   TEXT,
                     lot_parcel    TEXT, -- optional (not all lots have their own parcels)
                     description   TEXT,
                     notes         TEXT,                 -- notes about the LOT, never the tenancy
                     sort_order    INT NOT NULL,         -- manual ordering for the map/menu view; per-property, 1..N
                     shape_data    JSONB NOT NULL DEFAULT '{"vertices":[[0,0],[47,0],[47,68],[0,68]],"bbox":[0,0,47,68],"centroid":[23.5,34]}'::jsonb,
                     created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                     deleted_at    TIMESTAMPTZ,
                     FOREIGN KEY (property_id) REFERENCES property(uuid),

                     CONSTRAINT lot_not_rentable_has_reason CHECK (
                         is_rentable OR (not_rentable_reason IS NOT NULL AND length(trim(not_rentable_reason)) > 0)
                         ),
                     CONSTRAINT lot_reason_only_when_not_rentable CHECK (
                         is_rentable IS FALSE OR not_rentable_reason IS NULL
                         ),
                     CONSTRAINT lot_shape_has_polygon CHECK (
                             CASE WHEN jsonb_typeof(shape_data -> 'vertices') = 'array'
                             THEN jsonb_array_length(shape_data -> 'vertices') >= 3
                             ELSE false
                             END
                         )
);

CREATE TABLE lot_permissible_agreement_type ( -- shows what agreements are permissible for any given lot
            lot_id         UUID NOT NULL REFERENCES lot(uuid),
            agreement_type agreement_type NOT NULL,
            target_rate    NUMERIC(12,2) CHECK (target_rate >= 0),
            PRIMARY KEY (lot_id, agreement_type)
);

CREATE INDEX idx_lot_number_per_property
    ON lot (property_id, LOWER(lot_number)) WHERE deleted_at IS NULL;

CREATE INDEX idx_lot_sort_order_per_property
    ON lot (property_id, sort_order) WHERE deleted_at IS NULL;