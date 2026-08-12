-- ============================================================
-- V1: Enums & Property
-- ============================================================
--
-- THE DEFAULTS CHAIN
--   charge_term_defaults (property IS NULL)  -- global templates, admin only
--        |  an ADMIN explicitly copies a template into a property
--        v
--   charge_term_defaults (property = X)      -- that property's named sets
--        |  copied when a charge term is created
--        v
--   tenancy_charge_term                      -- the deal for one tenancy
--
-- Every hop is a COPY, never a live reference: editing a template must not
-- retroactively redefine deals that are already signed and billing.
--
-- The sets copied into a property ARE that property's whitelist of agreement
-- types. No RV set copied in means no RV agreement can exist there.
--
-- Three kinds of number, deliberately in three tables:
--   charge_term_defaults -> what a new deal STARTS as         (seed)
--   property_fee_cap     -> what the law will not let you exceed (legal ceiling)
--   property_fee_waiver  -> when company policy skips a fee    (business rule)

CREATE TYPE user_type AS ENUM ('AGENT', 'TENANT', 'SYSTEM');
CREATE TYPE operation_type AS ENUM ('INSERT', 'UPDATE', 'DELETE');

-- Defined once as a type rather than repeated as a CHECK across
-- charge_term_defaults, property_fee_cap, property_fee_waiver,
-- lot_allowed_agreement_type, and tenancy_charge_term. A value valid on the term
-- but missing from the cap list would make the cap lookup silently return
-- nothing and bill the fee uncapped.
--
-- The STATUTE is derived from this plus property_state, never stored:
-- MANUFACTURED_HOME_LOT in Washington is RCW 59.20, in Idaho it is something
-- else. Storing '59.20' would bake one state into a column.
CREATE TYPE agreement_type AS ENUM (
    'RESIDENTIAL','MANUFACTURED_HOME_LOT','RV_LOT',
    'TRANSIENT_LODGING','COMMERCIAL','STORAGE','UTILITY_SERVICE'
    );

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
                          property_manager UUID, -- FK to agent added in V3 after agent table exists

    -- note: rent skips the template chain. Seeds lot.target_rent,
    -- which seeds tenancy_charge_term.rent_amount.
                          default_target_rent NUMERIC(12,2) NOT NULL DEFAULT 500 CHECK (default_target_rent >= 0),

                          created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                          deleted_at       TIMESTAMPTZ
);


CREATE TABLE charge_term_defaults (
                                      uuid     UUID PRIMARY KEY DEFAULT uuidv7(),

    -- NULL = global template. Non-null = this property's own set.
                                      property UUID REFERENCES property(uuid), -- if NULL, only an admin can copy this to a property

    -- What the office worker sees in the picker: "Standard Lot Lease",
    -- "RV Month-to-Month", "Residential - Park Owned Home".
                                      name     TEXT NOT NULL CHECK (length(trim(name)) > 0),

    -- Drives which lease template is generated and which notice rules
    -- apply. Constrained by type, unlike `name`, which is human-facing.
                                      agreement_type agreement_type NOT NULL, -- do not patch

    -- ── The deal terms themselves ────────────────────────────────
    -- DEFAULTs here are load-bearing: they are how the global templates
    -- at the bottom of this file get seeded.
                                      car_fee           NUMERIC(12,2) NOT NULL DEFAULT 65.0 CHECK (car_fee >= 0),
                                      allowed_cars      INT           NOT NULL DEFAULT 2    CHECK (allowed_cars >= 0),
                                      pet_fee           NUMERIC(12,2) NOT NULL DEFAULT 45.0 CHECK (pet_fee >= 0),
                                      allowed_pets      INT           NOT NULL DEFAULT 2    CHECK (allowed_pets >= 0),

                                      rent_due_day      INT           NOT NULL DEFAULT 1    CHECK (rent_due_day BETWEEN 1 AND 28),
                                      grace_period_days INT           NOT NULL DEFAULT 7    CHECK (grace_period_days >= 0),

                                      rule_violation_fee_method TEXT NOT NULL DEFAULT 'FLAT'
                                          CHECK (rule_violation_fee_method IN ('NONE','FLAT')),
                                      rule_violation_fee_amount NUMERIC(12,2) DEFAULT 65 CHECK (rule_violation_fee_amount >= 0),

                                      nsf_fee_method    TEXT          NOT NULL DEFAULT 'FLAT'
                                          CHECK (nsf_fee_method IN ('NONE','FLAT')),
                                      nsf_fee_amount    NUMERIC(12,2) NOT NULL DEFAULT 25.0 CHECK (nsf_fee_amount >= 0),

                                      late_fee_method   TEXT          NOT NULL DEFAULT 'FLAT'
                                          CHECK (late_fee_method IN ('NONE','FLAT')),
                                      late_fee_amount   NUMERIC(12,2) NOT NULL DEFAULT 65.0 CHECK (late_fee_amount >= 0),

                                      water_method      TEXT          NOT NULL DEFAULT 'NONE'
                                          CHECK (water_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                                      water_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (water_flat_amount >= 0),
                                      power_method      TEXT          NOT NULL DEFAULT 'NONE'
                                          CHECK (power_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                                      power_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (power_flat_amount >= 0),
                                      sewer_method      TEXT          NOT NULL DEFAULT 'NONE'
                                          CHECK (sewer_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                                      sewer_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (sewer_flat_amount >= 0),
                                      trash_method      TEXT          NOT NULL DEFAULT 'NONE'
                                          CHECK (trash_method IN ('NONE','FLAT','RUBS')),
                                      trash_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (trash_flat_amount >= 0),

                                      note       TEXT,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      updated_by UUID,  -- FK added in V3 after agent table exists
                                      deleted_at TIMESTAMPTZ,

    -- The same completeness rules the charge term enforces, applied here
    -- so a broken set cannot be saved and then copied into every new deal
    -- at the property. No PROPOSED escape hatch on this table: a defaults
    -- set is either usable or it is not.
                                      CONSTRAINT defaults_term_late_fee_amount_matches_method CHECK (
                                          CASE WHEN late_fee_method = 'FLAT' THEN late_fee_amount > 0
                                               ELSE late_fee_amount = 0 END
                                          ),

                                      CONSTRAINT defaults_water_amount_matches_method CHECK (
                                          CASE WHEN water_method = 'FLAT' THEN water_flat_amount > 0
                                               ELSE water_flat_amount = 0 END
                                          ),
                                      CONSTRAINT defaults_power_amount_matches_method CHECK (
                                          CASE WHEN power_method = 'FLAT' THEN power_flat_amount > 0
                                               ELSE power_flat_amount = 0 END
                                          ),
                                      CONSTRAINT defaults_sewer_amount_matches_method CHECK (
                                          CASE WHEN sewer_method = 'FLAT' THEN sewer_flat_amount > 0
                                               ELSE sewer_flat_amount = 0 END
                                          ),
                                      CONSTRAINT defaults_trash_amount_matches_method CHECK (
                                          CASE WHEN trash_method = 'FLAT' THEN trash_flat_amount > 0
                                               ELSE trash_flat_amount = 0 END
                                          ),
                                      CONSTRAINT defaults_nsf_amount_matches_method CHECK (
                                          CASE WHEN nsf_fee_method IN ('FLAT')
                                                   THEN nsf_fee_amount > 0
                                               ELSE nsf_fee_amount = 0 END
                                          ),
                                      CONSTRAINT defaults_rule_violation_amount_matches_method CHECK (
                                          CASE WHEN rule_violation_fee_method = 'FLAT'
                                          THEN rule_violation_fee_amount IS NOT NULL AND rule_violation_fee_amount > 0
                                          ELSE COALESCE(rule_violation_fee_amount, 0) = 0 END
                                          )
);


-- The picker: every set available at a property. The service filters on
-- `property = :property` and never `IS NULL` -- global rows are visible only on
-- the admin settings screen, never in an office worker's picker.
CREATE INDEX charge_term_defaults_property_idx
    ON charge_term_defaults (property)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX charge_term_defaults_name_uq
    ON charge_term_defaults (property, lower(name)) NULLS NOT DISTINCT
    WHERE deleted_at IS NULL;
-- ── Property fee caps ────────────────────────────────────────────────────────
-- Regulatory ceilings applied at BILLING time, on top of whatever the lease says.
-- A lease naming a $65 late fee at a property whose city caps them at $15 bills $15.
--
-- Rows, not columns: capping N fee types across M agreement types would be
-- 3 x N x M columns on property. Adding a pet fee cap here is one INSERT.
--
-- NO ROW MEANS NO CAP. Absence is the common case, so most properties have zero
-- rows rather than a dozen columns of NULL.
--
-- Caps are really JURISDICTION facts, duplicated per property. Two parks in the
-- same city get identical rows. That is fine at this scale, and this shape means
-- a future jurisdiction table becomes something that POPULATES these rows rather
-- than a redesign.

CREATE TABLE property_fee_cap (
                                  uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
                                  property       UUID NOT NULL REFERENCES property(uuid),
                                  agreement_type agreement_type NOT NULL,

    -- No OTHER: a cap has to be applied by a code path. Billing computes a
    -- late fee, an NSF fee, a pet fee -- there is no function that computes
    -- an "other" fee, so such a row could never be read by anything. An
    -- unusable row is worse than a missing one because it looks like coverage.
                                  fee_type       TEXT NOT NULL
                                      CHECK (fee_type IN ('LATE','NSF','PET','CAR','VIOLATION')),

                                  cap_flat       NUMERIC(12,2) CHECK (cap_flat IS NULL OR cap_flat >= 0),
                                  cap_percent_of_rent NUMERIC(5,4)
                                      CHECK (cap_percent_of_rent IS NULL OR (cap_percent_of_rent >= 0 AND cap_percent_of_rent <= 1)),

    -- NOT NULL on purpose. A cap with no citation is folklore -- someone
    -- remembers hearing $15 and types it in. This column is what answers
    -- "why was I charged $15 when my lease says $65": an RCW section, a
    -- municipal code, a settlement. It is also why business-policy rules
    -- live in property_fee_waiver instead of here, where they would force
    -- "internal policy" into a column that should always name a legal authority.
                                  cap_source     TEXT NOT NULL CHECK (length(trim(cap_source)) > 0),

                                  note           TEXT,
                                  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  created_by     UUID NOT NULL,  -- FK added in V3 after agent table exists
                                  deleted_at     TIMESTAMPTZ,

                                  CONSTRAINT fee_cap_has_a_limit CHECK (cap_flat IS NOT NULL OR cap_percent_of_rent IS NOT NULL)
);

-- Without this, a property can hold two LATE caps for the same agreement type --
-- one $15, one $50 -- and billing picks whichever the planner returns first,
-- silently and differently across runs.
CREATE UNIQUE INDEX property_fee_cap_uq
    ON property_fee_cap (property, agreement_type, fee_type)
    WHERE deleted_at IS NULL;


-- ── Property fee waivers ─────────────────────────────────────────────────────
-- Company policy, not law: skip a fee entirely when the park is above a given
-- occupancy. Kept separate from property_fee_cap so cap_source stays trustworthy
-- as a legal citation, and so a tenant question has separately explainable
-- answers -- "the law caps this" and "we do not charge this right now" are
-- different sentences.
--
-- Billing order: term amount -> apply caps -> apply waivers.
--
-- OCCUPANCY RATE is defined as:
--
--   numerator   = active tenancies whose charge term's agreement_type is one of
--                 RESIDENTIAL, MANUFACTURED_HOME_LOT, RV_LOT, TRANSIENT_LODGING
--   denominator = lots where is_rentable = TRUE AND at least one row in
--                 lot_allowed_agreement_type names one of those same types
--
-- Laundry lots, offices, storage and utility-service rows fall out of the
-- denominator on their own, with no hand-maintained exclusion list.
--
-- Recomputation is expected and fine: pending, unserved charges SHOULD move when
-- the underlying facts move. What must never change is an invoice already issued
-- to a tenant -- that is the freeze boundary on the billing run, not here.
--
-- WATCH: TRANSIENT_LODGING turns over nightly, so a park with transient sites
-- will see occupancy swing week to week and a waiver flip on and off with it.
-- Consider excluding transient from THIS calculation if that proves noisy.

CREATE TABLE property_fee_waiver (
                                     uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
                                     property       UUID NOT NULL REFERENCES property(uuid),

    -- Keyed by agreement type because a single property-level column
    -- would apply the same rule to MH lots, RV spaces, and residential
    -- leases alike.
                                     agreement_type agreement_type NOT NULL,
                                     fee_type       TEXT NOT NULL
                                         CHECK (fee_type IN ('LATE','NSF','PET','CAR','VIOLATION')),

    -- Suppress the fee when occupancy exceeds this rate. Strictly above:
    -- a park sitting exactly at the threshold still bills.
    -- Bounded at 1 because NUMERIC(5,4) would otherwise permit 9.9999.
                                     void_above_occupancy_rate NUMERIC(5,4) NOT NULL
                                         CHECK (void_above_occupancy_rate >= 0 AND void_above_occupancy_rate <= 1),

    -- The policy equivalent of cap_source. An office worker asked to
    -- explain a missing fee needs this on the row.
                                     reason     TEXT NOT NULL CHECK (length(trim(reason)) > 0),

                                     note       TEXT,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     created_by UUID NOT NULL,  -- FK added in V3 after agent table exists
                                     deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX property_fee_waiver_uq
    ON property_fee_waiver (property, agreement_type, fee_type)
    WHERE deleted_at IS NULL;


-- ── Seed the global templates ────────────────────────────────────────────────
-- Starters covering the common cases. An admin edits these during setup and adds
-- others (an RV set, a storage set) as needed.
--
-- These are NOT auto-copied into new properties. A property starts with NO
-- defaults sets, and an admin explicitly copies in only the ones that park
-- actually needs -- otherwise every new park would silently gain an RV
-- agreement whether or not it has a single RV space.
--
-- A consequence worth surfacing in the UI: until an admin copies at least one
-- set in, charge term creation at that property must FAIL LOUDLY ("no charge
-- term defaults configured") rather than falling back to a global. A silent
-- global fallback would reintroduce the whole problem through the back door.
--
-- These are also the only values in the system that can reach a real tenant's
-- ledger without a human having typed them: if an admin clicks past the setup
-- screen, they go live. Everything downstream fails loudly instead.

INSERT INTO charge_term_defaults (property, name, agreement_type)
VALUES (NULL, 'Standard Manufactured Home Lot Lease', 'MANUFACTURED_HOME_LOT'),
       (NULL, 'Standard Residential Lease',           'RESIDENTIAL'),
       (NULL, 'Standard Storage Agreement',           'STORAGE');