-- ============================================================
-- V1: Enums & Property
-- ============================================================

CREATE TYPE user_type AS ENUM ('AGENT', 'TENANT', 'SYSTEM');
CREATE TYPE operation_type AS ENUM ('INSERT', 'UPDATE', 'DELETE');


-- ── Property ─────────────────────────────────────────────────────────────────

CREATE TABLE property (
                          uuid             UUID PRIMARY KEY DEFAULT uuidv7(),
                          property_code    VARCHAR(255),
                          property_name    TEXT NOT NULL,
                          property_address TEXT NOT NULL,
                          property_city    VARCHAR(255),
                          property_state   VARCHAR(2),
                          purchase_date    DATE,
                          year_built       INT,
                          late_fee_rate    NUMERIC(5,4) NOT NULL DEFAULT 0.0150,
                          property_manager UUID,                                  -- FK added in V3 after agent table exists
                          created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                          deleted_at       TIMESTAMPTZ
);


-- ── Globals ─────────────────────────────────────────────────────────────────


CREATE TABLE global_settings (

);