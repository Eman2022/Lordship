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

-- note: the same PDF can legitimately be connected to two different tenancies. Indexed so duplicates can be found
CREATE INDEX document_file_sha256_idx ON document_file (sha256);


-- ── Document templates ───────────────────────────────────────────────────────
-- Global and admin-only, and not coupled to standard_terms: a document is the
-- instrument that explains a deal, not part of it. Edits here reach every
-- future render. Renders already made are frozen on instrument.

CREATE TABLE document_template (
                                   uuid            UUID PRIMARY KEY DEFAULT uuidv7(),
                                   name            TEXT NOT NULL,
                                   agreement_type  agreement_type NOT NULL, -- must match the term's type at generate
                                   instrument_type instrument_type NOT NULL,
                                   version         INT NOT NULL DEFAULT 1, -- bumped by the service on any clause change
                                   note            TEXT,
                                   created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   created_by      UUID NOT NULL REFERENCES agent(uuid),
                                   deleted_at      TIMESTAMPTZ
);


CREATE TABLE template_clause (
                                 uuid        UUID PRIMARY KEY DEFAULT uuidv7(),
                                 template    UUID NOT NULL REFERENCES document_template(uuid),
                                 section     TEXT, -- sub-document within the packet; the unit a property excludes
                                 ordinal     NUMERIC(10,4) NOT NULL,
                                 clause_key  TEXT, -- stable identity across versions, e.g. RENT_AND_FEES
                                 title       TEXT,
                                 body        TEXT, -- tokens only, never a literal amount
                                 required    BOOLEAN NOT NULL DEFAULT FALSE, -- a required clause cannot be excluded
                                 statute_ref TEXT, -- the citation this clause exists to satisfy
                                 note        TEXT,
                                 created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 created_by  UUID NOT NULL REFERENCES agent(uuid),
                                 deleted_at  TIMESTAMPTZ
);


-- Which documents a property may use. A reference, not a copy: edits to the
-- template reach the property. This is also the whitelist the office worker
-- picks from at generate time.
CREATE TABLE property_document (
                                   uuid              UUID PRIMARY KEY DEFAULT uuidv7(),
                                   property          UUID NOT NULL REFERENCES property(uuid),
                                   document_template UUID NOT NULL REFERENCES document_template(uuid),
                                   note              TEXT,
                                   created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   created_by        UUID NOT NULL REFERENCES agent(uuid),
                                   deleted_at        TIMESTAMPTZ
);


-- Property customization of an assigned document. EXCLUDE drops a non-required
-- global clause, ADD contributes a clause this property authored. No REPLACE:
-- a global clause body is never rewritten locally.
CREATE TABLE property_document_clause (
                                  uuid              UUID PRIMARY KEY DEFAULT uuidv7(),
                                  property_document UUID NOT NULL REFERENCES property_document(uuid),
                                  action            TEXT NOT NULL CHECK (action IN ('EXCLUDE','ADD')),
                                  clause            UUID REFERENCES template_clause(uuid), -- set for EXCLUDE, null for ADD
                                  ordinal           NUMERIC(10,4), -- shares the coordinate space with template_clause.ordinal
                                  title             TEXT,
                                  body              TEXT,
                                  note              TEXT,
                                  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  created_by        UUID NOT NULL REFERENCES agent(uuid),
                                  deleted_at        TIMESTAMPTZ
);


CREATE TABLE instrument ( -- WIP
                            uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
                            tenancy        UUID NOT NULL REFERENCES tenancy(uuid),
                            type           instrument_type NOT NULL,

                            status         TEXT NOT NULL DEFAULT 'DRAFT'
                                CHECK (status IN ('DRAFT','GENERATED','SENT','SERVED','APPROVED','ABANDONED')),

                            serial         TEXT, -- printed on the paper, assigned at GENERATED
                            amends         UUID REFERENCES instrument(uuid), -- an assumption or addendum reaches back to the original

                            term_start     DATE, -- the period THIS document covers; null on notices and addenda
                            term_months    INT CHECK (term_months > 0), -- 1 for month to month; inherited leases may exceed 12
                            on_expiry      TEXT CHECK (on_expiry IN ('MONTH_TO_MONTH','AUTO_RENEW','TERMINATE')),

                            template          UUID REFERENCES document_template(uuid),
                            template_version  INT, -- the wording actually rendered
                            property_document UUID REFERENCES property_document(uuid), -- which override set applied

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


                            CONSTRAINT instrument_generated_has_file CHECK (
                                status = 'DRAFT' OR status = 'ABANDONED' OR generated_file IS NOT NULL
                                ),

                            CONSTRAINT instrument_generated_has_timestamp CHECK (
                                generated_file IS NULL OR generated_at IS NOT NULL
                                ),

                            CONSTRAINT instrument_generated_has_wording CHECK (
                                generated_file IS NULL
                                    OR (template IS NOT NULL AND template_version IS NOT NULL AND serial IS NOT NULL)
                                ),

                            CONSTRAINT instrument_lease_has_term CHECK (
                                generated_file IS NULL
                                    OR type NOT IN ('LEASE','ASSUMPTION','WAIVER')
                                    OR (term_start IS NOT NULL AND term_months IS NOT NULL)
                                ),

                            CONSTRAINT instrument_served_has_facts CHECK (
                                status <> 'SERVED'
                                    OR (served_on IS NOT NULL AND service_method IS NOT NULL AND served_by IS NOT NULL)
                                ),

                            CONSTRAINT instrument_approved_has_file CHECK (
                                status <> 'APPROVED' OR (returned_on IS NOT NULL AND returned_file IS NOT NULL)
                                -- APPROVED means the signed document is physically back AND an office
                                -- worker has accepted it- (not one or the other)
                                ),

                            CONSTRAINT instrument_dates_ordered CHECK (
                                returned_on IS NULL OR served_on IS NULL OR returned_on >= served_on
                                ),

                            CONSTRAINT instrument_uuid_tenancy_uq UNIQUE (uuid, tenancy)
);

CREATE INDEX instrument_tenancy_idx ON instrument (tenancy);

-- The office work queue: what paper is out in the field right now.
CREATE INDEX instrument_open_idx
    ON instrument (tenancy)
    WHERE status IN ('GENERATED','SENT','SERVED');


-- ── Charge term ──────────────────────────────────────────────────────────────
-- to go into effect two conditions must be met: now() >= valid_at && status = 'ACTIVE'

CREATE TABLE tenancy_charge_term (
                                     uuid              UUID PRIMARY KEY DEFAULT uuidv7(),
                                     tenancy           UUID NOT NULL REFERENCES tenancy(uuid),
                                     valid_at          DATE NOT NULL,

                                     agreement_type    agreement_type NOT NULL, -- do not allow editing from patch requests

                                     rate              NUMERIC(12,2) NOT NULL CHECK (rate >= 0), -- COALESCE(lot rate, standard_terms.target_rate)
                                     car_fee           NUMERIC(12,2) NOT NULL CHECK (car_fee >= 0),
                                     allowed_cars      INT           NOT NULL CHECK (allowed_cars >= 0),
                                     cars_max          INT           NOT NULL CHECK (cars_max >= 0),
                                     pet_fee           NUMERIC(12,2) NOT NULL CHECK (pet_fee >= 0),
                                     allowed_pets      INT           NOT NULL CHECK (allowed_pets >= 0),

                                     payment_due_day   INT NOT NULL CHECK (payment_due_day BETWEEN 1 AND 28),
                                     grace_period_days INT NOT NULL CHECK (grace_period_days >= 0),

                                     rule_violation_fee_method TEXT NOT NULL
                                         CHECK (rule_violation_fee_method IN ('NONE','FLAT')),
                                     rule_violation_fee_amount NUMERIC(12,2) CHECK (rule_violation_fee_amount >= 0),

                                     nsf_fee_method    TEXT NOT NULL  -- BANK_FEE and BANK_PLUS_FLAT not implemented
                                         CHECK (nsf_fee_method IN ('NONE','FLAT')),
                                     nsf_fee_amount    NUMERIC(12,2) CHECK (nsf_fee_amount >= 0),

                                     late_fee_method   TEXT NOT NULL
                                         CHECK (late_fee_method IN ('NONE','FLAT', 'PERCENT_OF_RENT')),
                                     late_fee_amount   NUMERIC(12,2) NOT NULL CHECK (late_fee_amount >= 0), -- can be a percent OR a flat rate

                                     water_method      TEXT NOT NULL
                                         CHECK (water_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                                     water_flat_amount NUMERIC(12,2) NOT NULL CHECK (water_flat_amount >= 0),
                                     power_method      TEXT NOT NULL
                                         CHECK (power_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                                     power_flat_amount NUMERIC(12,2) NOT NULL CHECK (power_flat_amount >= 0),
                                     sewer_method      TEXT NOT NULL
                                         CHECK (sewer_method IN ('NONE','FLAT','RUBS','SUBMETERED')),
                                     sewer_flat_amount NUMERIC(12,2) NOT NULL CHECK (sewer_flat_amount >= 0),
                                     trash_method      TEXT NOT NULL
                                         CHECK (trash_method IN ('NONE','FLAT','RUBS')),
                                     trash_flat_amount NUMERIC(12,2) NOT NULL CHECK (trash_flat_amount >= 0),

                                     status            TEXT NOT NULL DEFAULT 'PROPOSED'
                                         CHECK (status IN ('PROPOSED','PENDING','ACTIVE','CANCELLED')),
    -- PROPOSED  - editable, filled in incrementally
    -- PENDING   - submitted- a document is out for signature or service
    -- ACTIVE    - in force from valid_at until a later ACTIVE term supersedes it
    -- CANCELLED - was in force, retracted; excluded from resolution entirely

                                     source            TEXT NOT NULL
                                         CHECK (source IN ('LEASE','INCREASE_NOTICE','ASSUMPTION','ADDENDUM','CORRECTION','MIGRATION')),

                                     source_uuid       UUID,     -- the instrument that produced this deal

                                     terms_template    UUID REFERENCES terms_template(uuid), -- which template seeded the values

                                     batch             UUID,     -- groups one bulk run so it can be reviewed or abandoned together

                                     cancelled_at      TIMESTAMPTZ,
                                     cancelled_by      UUID REFERENCES agent(uuid),
                                     cancel_reason     TEXT,
                                     deleted_at        TIMESTAMPTZ, -- soft delete ONLY for charge terms that never generated charges
                                     note              TEXT,
                                     created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     created_by        UUID NOT NULL REFERENCES agent(uuid),

                                     CONSTRAINT term_source_same_tenancy -- enforces
                                         FOREIGN KEY (source_uuid, tenancy) REFERENCES instrument (uuid, tenancy),

                                     CONSTRAINT term_late_fee_amount_matches_method CHECK (
                                         status = 'PROPOSED' OR
                                         CASE WHEN late_fee_method = 'FLAT' THEN late_fee_amount > 0
                                              ELSE late_fee_amount = 0 END
                                         ),

                                     CONSTRAINT term_rule_violation_amount_matches_method CHECK (
                                         status = 'PROPOSED' OR
                                         CASE WHEN rule_violation_fee_method = 'FLAT'
                                                  THEN rule_violation_fee_amount IS NOT NULL AND rule_violation_fee_amount > 0
                                              ELSE COALESCE(rule_violation_fee_amount, 0) = 0 END
                                         ),

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

                                     CONSTRAINT term_nsf_amount_matches_method CHECK (
                                         status = 'PROPOSED' OR (
                                             CASE WHEN nsf_fee_method = 'FLAT'
                                                      THEN nsf_fee_amount IS NOT NULL AND nsf_fee_amount > 0
                                                  ELSE COALESCE(nsf_fee_amount, 0) = 0 END
                                             )
                                         ),
                                     CONSTRAINT term_in_force_needs_paper CHECK (
                                         status NOT IN ('ACTIVE','CANCELLED')
                                             OR source = 'MIGRATION'
                                             OR source_uuid IS NOT NULL
                                         ),

                                     CONSTRAINT term_cancel_facts CHECK (
                                         status <> 'CANCELLED'
                                             OR (cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL
                                             AND cancel_reason IS NOT NULL)
                                         ),

                                     CONSTRAINT term_cancel_fields_only_when_cancelled CHECK (
                                         status = 'CANCELLED'
                                             OR (cancelled_at IS NULL AND cancelled_by IS NULL
                                             AND cancel_reason IS NULL)
                                         ),

                                     CONSTRAINT term_delete_only_before_force CHECK (
                                         deleted_at IS NULL OR status IN ('PROPOSED','PENDING')
                                         )
);

-- Two terms cannot take effect for the same tenancy on the same date
CREATE UNIQUE INDEX tenancy_charge_term_in_force_uq
    ON tenancy_charge_term (tenancy, valid_at)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;


-- The work queue: terms still being written or out for signature.
CREATE INDEX tenancy_charge_term_open_idx
    ON tenancy_charge_term (tenancy)
    WHERE status IN ('PROPOSED','PENDING') AND deleted_at IS NULL;

-- Answers "what deal did this document produce," for the instrument detail view.
CREATE INDEX tenancy_charge_term_source_idx
    ON tenancy_charge_term (source_uuid)
    WHERE source_uuid IS NOT NULL;
