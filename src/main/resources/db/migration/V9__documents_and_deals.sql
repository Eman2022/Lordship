-- ============================================================
-- V9: Documents and Deals
-- ============================================================

CREATE TABLE document_file ( -- WIP
                               uuid         UUID PRIMARY KEY DEFAULT uuidv7(),
                               file_name    TEXT NOT NULL,
                               content_type TEXT NOT NULL,
                               byte_size    BIGINT NOT NULL,
                               sha256       BYTEA NOT NULL,
                               storage_key  TEXT NOT NULL,
                               uploaded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                               uploaded_by  UUID NOT NULL REFERENCES agent(uuid)
);

-- One storage object, one row. Without this a retry or double-submit silently
-- creates a second row pointing at the same blob, and deleting either one
-- orphans the other.
CREATE UNIQUE INDEX document_file_storage_key_uq ON document_file (storage_key);

-- Deliberately NOT unique: the same PDF can legitimately be uploaded against two
-- tenancies. Indexed so duplicates can be found on purpose rather than by accident.
CREATE INDEX document_file_sha256_idx ON document_file (sha256);


CREATE TABLE instrument ( -- WIP
                            uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
                            tenancy        UUID NOT NULL REFERENCES tenancy(uuid),
                            type           TEXT NOT NULL
                                CHECK (type IN ('LEASE','INCREASE_NOTICE','RULES_ADDENDUM','OTHER_ADDENDUM')),

                            status         TEXT NOT NULL DEFAULT 'DRAFT'
                                CHECK (status IN ('DRAFT','GENERATED','SENT','SERVED','APPROVED','ABANDONED')),

                            generated_at   TIMESTAMPTZ,
                            generated_file UUID REFERENCES document_file(uuid),

                            sent_at        TIMESTAMPTZ,
                            sent_by        UUID REFERENCES agent(uuid),

                            served_on      DATE,
                            service_method TEXT CHECK (service_method IN ('PERSONAL','MAIL','POST_AND_MAIL','EMAIL','OTHER')),
                            served_by      UUID REFERENCES agent(uuid),
                            proof_file     UUID REFERENCES document_file(uuid),

                            returned_on    DATE,
                            returned_file  UUID REFERENCES document_file(uuid),

                            note           TEXT,
                            created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                            created_by     UUID NOT NULL REFERENCES agent(uuid),

    -- An instrument cannot be in a non-draft, non-abandoned state without a
    -- generated file attached.
                            CONSTRAINT instrument_generated_has_file CHECK (
                                status = 'DRAFT' OR status = 'ABANDONED' OR generated_file IS NOT NULL
                                ),

    -- generated_file without generated_at leaves you unable to answer "when was
    -- this produced." The two facts always arrive together, so require both.
                            CONSTRAINT instrument_generated_has_timestamp CHECK (
                                generated_file IS NULL OR generated_at IS NOT NULL
                                ),

    -- A row marked served must carry the essential service details: date,
    -- method, and the serving agent. This is what makes a notice period
    -- calculable and a service defensible.
                            CONSTRAINT instrument_served_has_facts CHECK (
                                status <> 'SERVED'
                                    OR (served_on IS NOT NULL AND service_method IS NOT NULL AND served_by IS NOT NULL)
                                ),

    -- 'approved' means the signed document is physically back AND an office
    -- worker has accepted it. Both facts must be present.
                            CONSTRAINT instrument_approved_has_file CHECK (
                                status <> 'APPROVED' OR (returned_on IS NOT NULL AND returned_file IS NOT NULL)
                                ),

    -- The lifecycle is strictly ordered. Without this you can record a document
    -- as returned before it was served.
    -- NOTE: sent_at is deliberately excluded from this comparison. It is a
    -- TIMESTAMPTZ, and casting it to DATE is only STABLE, not IMMUTABLE, so
    -- Postgres will reject it inside a CHECK. Enforce sent -> served ordering
    -- in the service layer.
                            CONSTRAINT instrument_dates_ordered CHECK (
                                returned_on IS NULL OR served_on IS NULL OR returned_on >= served_on
                                ),

    -- Required as the target of the composite FK on tenancy_charge_term below.
    -- Redundant as a key (uuid is already the PK), but Postgres needs a unique
    -- constraint on exactly (uuid, tenancy) to reference that pair.
                            CONSTRAINT instrument_uuid_tenancy_uq UNIQUE (uuid, tenancy)
);

CREATE INDEX instrument_tenancy_idx ON instrument (tenancy);

-- The office work queue: what paper is out in the field right now.
CREATE INDEX instrument_open_idx
    ON instrument (tenancy)
    WHERE status IN ('GENERATED','SENT','SERVED');


CREATE TABLE tenancy_charge_term (
                                     uuid              UUID PRIMARY KEY DEFAULT uuidv7(),
                                     tenancy           UUID NOT NULL REFERENCES tenancy(uuid),
                                     valid_at          DATE NOT NULL,

                                     rent_amount       NUMERIC(12,2) NOT NULL           CHECK (rent_amount >= 0),
                                     car_fee           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (car_fee >= 0),
                                     allowed_cars      INT           NOT NULL DEFAULT 2 CHECK (allowed_cars >= 0),
                                     pet_fee           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (pet_fee >= 0),
                                     allowed_pets      INT           NOT NULL DEFAULT 2 CHECK (allowed_pets >= 0),

                                     rent_due_day      INT NOT NULL DEFAULT 1 CHECK (rent_due_day BETWEEN 1 AND 28),
                                     grace_period_days INT NOT NULL DEFAULT 5 CHECK (grace_period_days >= 0),

                                     late_fee_method   TEXT NOT NULL DEFAULT 'FLAT'
                                         CHECK (late_fee_method IN ('FLAT','PERCENT','DAILY')),
                                     late_fee_amount   NUMERIC(12,2) NOT NULL DEFAULT 60.00 CHECK (late_fee_amount >= 0),
                                     late_fee_max      NUMERIC(12,2) CHECK (late_fee_max IS NULL OR late_fee_max >= 0),

                                     water_method      TEXT NOT NULL DEFAULT 'NONE'
                                         CHECK (water_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                                     water_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (water_flat_amount >= 0),
                                     power_method      TEXT NOT NULL DEFAULT 'NONE'
                                         CHECK (power_method IN ('NONE','FLAT','RUBS','SUBMETERED')) ,
                                     power_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (power_flat_amount >= 0),
                                     sewer_method      TEXT NOT NULL DEFAULT 'NONE'
                                         CHECK (sewer_method IN ('NONE','FLAT','RUBS','SUBMETERED')) ,
                                     sewer_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (sewer_flat_amount >= 0),
                                     trash_method      TEXT NOT NULL DEFAULT 'NONE'
                                         CHECK (trash_method IN ('NONE','FLAT','RUBS')),
                                     trash_flat_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (trash_flat_amount >= 0),

                                     status            TEXT NOT NULL DEFAULT 'PROPOSED'
                                         CHECK (status IN ('PROPOSED','PENDING','ACTIVE','CANCELLED')),

                                     source            TEXT NOT NULL DEFAULT 'LEASE'
                                         CHECK (source IN ('LEASE','INCREASE_NOTICE','RULES_ADDENDUM','CORRECTION','MIGRATION')),
    -- No single-column FK here: the composite FK below supersedes it.
                                     source_uuid       UUID,

                                     cancelled_at      TIMESTAMPTZ,
                                     cancelled_by      UUID REFERENCES agent(uuid),
                                     cancel_reason     TEXT,
                                     deleted_at        TIMESTAMPTZ, -- soft delete ONLY for charge terms that never generated charges
                                     note              TEXT,
                                     created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     created_by        UUID NOT NULL REFERENCES agent(uuid),

    -- ── Utility method / amount agreement ────────────────────────────
    -- A method and its amount must agree. 'flat' with a zero amount
    -- silently bills nothing. 'rubs' or 'submetered' carrying a leftover
    -- nonzero amount is worse: a future reader cannot tell whether that
    -- number is live or stale. The CASE form forbids both.
                                     CONSTRAINT term_water_amount_matches_method CHECK (
                                         status = 'PROPOSED' OR
                                         CASE WHEN water_method = 'FLAT' THEN water_flat_amount > 0
                                              ELSE water_flat_amount = 0 END
                                         ),
                                     CONSTRAINT term_power_amount_matches_method CHECK (
                                         status = 'PROPOSED' OR
                                         CASE WHEN power_method = 'FLAT' THEN power_flat_amount > 0
                                              ELSE power_flat_amount = 0 END
                                         ),
                                     CONSTRAINT term_sewer_amount_matches_method CHECK (
                                         status = 'PROPOSED' OR
                                         CASE WHEN sewer_method = 'FLAT' THEN sewer_flat_amount > 0
                                              ELSE sewer_flat_amount = 0 END
                                         ),
                                     CONSTRAINT term_trash_amount_matches_method CHECK (
                                         status = 'PROPOSED' OR
                                         CASE WHEN trash_method = 'FLAT' THEN trash_flat_amount > 0
                                              ELSE trash_flat_amount = 0 END
                                         ),

                                        -- late fee max is only for percent and daily late fee types
                                     CONSTRAINT term_late_fee_max_only_when_variable CHECK (
                                         status = 'PROPOSED' OR
                                         late_fee_max IS NULL OR late_fee_method IN ('PERCENT','DAILY')
                                         ),

                                    -- non-migration term needs something on file
                                     CONSTRAINT term_in_force_needs_paper CHECK (
                                         status NOT IN ('ACTIVE','CANCELLED')
                                             OR source = 'MIGRATION'
                                             OR source_uuid IS NOT NULL
                                         ),

                                        -- one term cites one instrument (paper)
                                     CONSTRAINT term_source_same_tenancy
                                         FOREIGN KEY (source_uuid, tenancy) REFERENCES instrument (uuid, tenancy),

                                        -- don't allow canceled without more information
                                     CONSTRAINT term_cancel_fields_only_when_cancelled CHECK (
                                         status = 'CANCELLED'
                                             OR (cancelled_at IS NULL AND cancelled_by IS NULL
                                             AND cancel_reason IS NULL)
                                         ),

                                     CONSTRAINT term_delete_only_before_force CHECK ( -- active/ previously active terms can't be deleted
                                         deleted_at IS NULL OR status IN ('PROPOSED','PENDING')
                                         )
);

-- Two terms cannot take effect for the same tenancy on the same date.
-- This is also what makes retroactive correction possible: cancelling a term
-- drops it out of this index, freeing the date for its replacement.
CREATE UNIQUE INDEX tenancy_charge_term_in_force_uq
    ON tenancy_charge_term (tenancy, valid_at)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- No separate resolver index. The unique index above is a btree on
-- (tenancy, valid_at) with an identical predicate, and Postgres scans it
-- backward to satisfy:
--     WHERE tenancy = ? AND status = 'active' AND deleted_at IS NULL
--       AND valid_at <= ? ORDER BY valid_at DESC LIMIT 1
-- A second index on the same columns would be pure write overhead.

-- The work queue: terms awaiting paper or approval.
CREATE INDEX tenancy_charge_term_open_idx
    ON tenancy_charge_term (tenancy)
    WHERE status IN ('PROPOSED','PENDING') AND deleted_at IS NULL;

-- Answers "what deal did this document produce," for the instrument detail view.
CREATE INDEX tenancy_charge_term_source_idx
    ON tenancy_charge_term (source_uuid)
    WHERE source_uuid IS NOT NULL;