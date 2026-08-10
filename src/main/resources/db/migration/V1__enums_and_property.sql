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
                          property_manager UUID,                                  -- FK added in V3 after agent table exists


                            -- DEFAULTS inherit from global settings and get used by tenancy_charge_term
                          default_car_fee           NUMERIC(12,2) NOT NULL DEFAULT 0     CHECK (default_car_fee >= 0),
                          default_allowed_cars      INT           NOT NULL DEFAULT 2     CHECK (default_allowed_cars >= 0),
                          default_pet_fee           NUMERIC(12,2) NOT NULL DEFAULT 0     CHECK (default_pet_fee >= 0),
                          default_allowed_pets      INT           NOT NULL DEFAULT 2     CHECK (default_allowed_pets >= 0),

                          default_rent_due_day      INT           NOT NULL DEFAULT 1     CHECK (default_rent_due_day BETWEEN 1 AND 28),
                          default_grace_period_days INT           NOT NULL DEFAULT 15    CHECK (default_grace_period_days >= 0),

                          default_late_fee_method   TEXT          NOT NULL DEFAULT 'FLAT'
                              CHECK (default_late_fee_method IN ('FLAT','PERCENT','DAILY')),
                          default_late_fee_amount   NUMERIC(12,2) NOT NULL DEFAULT 60.00 CHECK (default_late_fee_amount >= 0),
                          default_late_fee_max      NUMERIC(12,2) CHECK (default_late_fee_max IS NULL OR default_late_fee_max >= 0),

                          default_water_method      TEXT          NOT NULL DEFAULT 'NONE'
                              CHECK (default_water_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                          default_water_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (default_water_flat_amount >= 0),
                          default_power_method      TEXT          NOT NULL DEFAULT 'NONE'
                              CHECK (default_power_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                          default_power_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (default_power_flat_amount >= 0),
                          default_sewer_method      TEXT          NOT NULL DEFAULT 'NONE'
                              CHECK (default_sewer_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                          default_sewer_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (default_sewer_flat_amount >= 0),
                          default_trash_method      TEXT          NOT NULL DEFAULT 'NONE'
                              CHECK (default_trash_method IN ('NONE','FLAT','RUBS')),
                          default_trash_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (default_trash_flat_amount >= 0),

                          -- caps and restrictions
                          late_fee_cap_flat    NUMERIC(12,2) CHECK (late_fee_cap_flat IS NULL OR late_fee_cap_flat >= 0),
                          late_fee_cap_percent NUMERIC(5,4)  CHECK (late_fee_cap_percent IS NULL OR late_fee_cap_percent >= 0), -- percentage of rent
                          late_fee_cap_source  TEXT,

                          created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                          deleted_at       TIMESTAMPTZ
);



CREATE TABLE global_settings (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    singleton BOOLEAN NOT NULL DEFAULT TRUE, -- allows enforcing status as singleton
    default_car_fee NUMERIC(12,2) NOT NULL DEFAULT 65.0 CHECK (default_car_fee >= 0),     -- two pets, 45 per violation
    default_allowed_cars INT NOT NULL DEFAULT 2         CHECK (default_allowed_cars >= 0),
    default_pet_fee NUMERIC(12,2) NOT NULL DEFAULT 45.0 CHECK (default_pet_fee >= 0),
    default_allowed_pets INT NOT NULL DEFAULT 2         CHECK (default_allowed_pets >= 0),

    default_rent_due_day INT NOT NULL DEFAULT 1         CHECK (default_rent_due_day BETWEEN 1 AND 28),
    default_grace_period_days INT NOT NULL DEFAULT 5    CHECK (default_grace_period_days >= 0),

    default_late_fee_method TEXT NOT NULL DEFAULT 'FLAT' CHECK (default_late_fee_method IN ('FLAT','PERCENT','DAILY')),
    default_late_fee_amount NUMERIC(12,2) NOT NULL DEFAULT 65.0 CHECK (default_late_fee_amount >= 0),
    default_late_fee_max NUMERIC(12,2) NOT NULL DEFAULT 65.0    CHECK (default_late_fee_max >= 0),

    default_water_method TEXT NOT NULL DEFAULT 'NONE'           CHECK (default_water_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
    default_water_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0  CHECK (default_water_flat_amount >= 0),

    default_power_method TEXT NOT NULL DEFAULT 'NONE'           CHECK (default_power_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
    default_power_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0  CHECK (default_power_flat_amount >= 0),

    default_sewer_method TEXT NOT NULL DEFAULT 'NONE'           CHECK (default_power_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
    default_sewer_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0  CHECK (default_sewer_flat_amount >= 0),

    default_trash_method TEXT NOT NULL DEFAULT 'NONE'           CHECK (default_trash_method IN ('NONE','FLAT','RUBS')),
    default_trash_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0  CHECK (default_trash_flat_amount >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,  -- FK added in V3 after agent table exists

    CONSTRAINT global_settings_singleton_true CHECK (singleton),
    CONSTRAINT global_settings_singleton_uq   UNIQUE (singleton)
);


INSERT INTO global_settings DEFAULT VALUES;