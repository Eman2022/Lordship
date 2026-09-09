-- ============================================================
-- V10: Homes
-- ============================================================

CREATE TABLE mobile_home (
         uuid UUID PRIMARY KEY DEFAULT uuidv7(),
         name TEXT,
         lot_id UUID REFERENCES lot(uuid),
         estimated_value NUMERIC(12,2) CHECK (estimated_value >= 0),
         estimated_value_on DATE,
         model_year INT CHECK (model_year BETWEEN 1930 AND 2100),
         make TEXT,
         model TEXT,
         bedroom_count INT CHECK (bedroom_count >= 0),
         bathroom_count NUMERIC(3,1) CHECK (bathroom_count >= 0),
         width  NUMERIC(6,2) CHECK (width > 0),
         length NUMERIC(6,2) CHECK (length > 0),
         dimensions_units TEXT CHECK (dimensions_units IN ('FT','M')) NOT NULL DEFAULT 'FT',
         sections INT CHECK (sections > 0 AND sections < 5),
         condition TEXT CHECK (condition IN ('NEW','EXCELLENT','GOOD','FAIR','POOR','UNINHABITABLE','DEMO')),
         appearance TEXT,           -- a description of the unit "blue paint and windows with wood shutters"
         note TEXT,
         parcel TEXT,               -- optional (not all homes have their own parcels)
         vin TEXT,                  -- optional (not all homes have their own vin). Should be as printed; multisection homes list both
         park_owned BOOLEAN NOT NULL DEFAULT FALSE,
         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
         created_by UUID REFERENCES agent(uuid),
         deleted_at TIMESTAMPTZ,

         CONSTRAINT mobile_home_valuation_needs_value CHECK (
             estimated_value_on IS NULL OR estimated_value IS NOT NULL
        )
);

CREATE INDEX idx_mobile_home_lot
    ON mobile_home(lot_id) WHERE deleted_at IS NULL;


CREATE INDEX idx_mobile_home_vin
    ON mobile_home(UPPER(vin)) WHERE vin IS NOT NULL AND deleted_at IS NULL;