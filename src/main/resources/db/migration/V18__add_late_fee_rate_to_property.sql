-- Adds late_fee_rate to property table.
-- Stored as a decimal multiplier (e.g. 0.0150 = 1.5%).
-- Used by the future billing engine to compute late fees per property.
-- Depends on: property (V1)

ALTER TABLE property
    ADD COLUMN late_fee_rate NUMERIC(5,4) NOT NULL DEFAULT 0.0150;
