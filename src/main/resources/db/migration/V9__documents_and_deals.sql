-- ============================================================
-- V9: Documents and Deals
-- ============================================================



CREATE TABLE document_file (
                               uuid         UUID PRIMARY KEY DEFAULT uuidv7(),
                               file_name    TEXT NOT NULL,
                               content_type TEXT NOT NULL,
                               byte_size    BIGINT NOT NULL,
                               sha256       BYTEA NOT NULL,
                               storage_key  TEXT NOT NULL,
                               uploaded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                               uploaded_by  UUID NOT NULL REFERENCES agent(uuid)
);


CREATE TABLE instrument (
                            uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
                            tenancy        UUID NOT NULL REFERENCES tenancy(uuid),
                            type           TEXT NOT NULL
                                CHECK (type IN ('lease','increase_notice','rules_addendum','other_addendum')),

                            status         TEXT NOT NULL DEFAULT 'draft'
                                CHECK (status IN ('draft','generated','sent','served','returned','abandoned')),

                            generated_at   TIMESTAMPTZ,
                            generated_file UUID REFERENCES document_file(uuid),

                            sent_at        TIMESTAMPTZ,
                            sent_by        UUID REFERENCES agent(uuid),

                            served_on      DATE,
                            service_method TEXT CHECK (service_method IN ('personal','mail','post_and_mail','email','other')),
                            served_by      UUID REFERENCES agent(uuid),
                            proof_file     UUID REFERENCES document_file(uuid),

                            returned_on    DATE,
                            returned_file  UUID REFERENCES document_file(uuid),

                            note           TEXT,
                            created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                            created_by     UUID NOT NULL REFERENCES agent(uuid),

                            -- Enforce that an instrument cannot be in a non‑draft, non‑abandoned state without a generated file attached.
                            CONSTRAINT instrument_generated_has_file CHECK (
                                status = 'draft' OR status = 'abandoned' OR generated_file IS NOT NULL
                                ),
                            -- Guarantee that a row marked served includes the essential service details: date, method, and the serving agent.
                            CONSTRAINT instrument_served_has_facts CHECK (
                                status <> 'served'
                                    OR (served_on IS NOT NULL AND service_method IS NOT NULL AND served_by IS NOT NULL)
                                ),
                            -- Ensures that when an instrument is marked returned, the return date and the returned document file are recorded.
                            CONSTRAINT instrument_returned_has_file CHECK (
                                status <> 'returned' OR (returned_on IS NOT NULL AND returned_file IS NOT NULL)
                                )
);
CREATE INDEX instrument_tenancy_idx ON instrument (tenancy);


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

                                     late_fee_method   TEXT NOT NULL DEFAULT 'flat'
                                         CHECK (late_fee_method IN ('flat','percent','daily')),
                                     late_fee_amount   NUMERIC(12,2) NOT NULL DEFAULT 60.00 CHECK (late_fee_amount >= 0),
                                     late_fee_max      NUMERIC(12,2) CHECK (late_fee_max IS NULL OR late_fee_max >= 0),

                                     can_charge_water  BOOLEAN NOT NULL DEFAULT FALSE,
                                     can_charge_power  BOOLEAN NOT NULL DEFAULT FALSE,
                                     can_charge_trash  BOOLEAN NOT NULL DEFAULT FALSE,
                                     can_charge_sewer  BOOLEAN NOT NULL DEFAULT FALSE,

                                     status            TEXT NOT NULL DEFAULT 'proposed'
                                         CHECK (status IN ('proposed','pending','active','cancelled','void')),

                                     source            TEXT NOT NULL DEFAULT 'lease'
                                         CHECK (source IN ('lease','increase_notice','rules_addendum',
                                                           'correction','migration')),
                                     source_uuid       UUID REFERENCES instrument(uuid),

                                     activated_on      DATE, -- date status set to 'active'
                                     activated_by      UUID REFERENCES agent(uuid),

                                     ends_on           DATE,                       -- first date this term no longer applies
                                     cancelled_at      TIMESTAMPTZ,
                                     cancelled_by      UUID REFERENCES agent(uuid),
                                     cancel_reason     TEXT,

                                     voided_at         TIMESTAMPTZ,
                                     voided_by         UUID REFERENCES agent(uuid),
                                     void_reason       TEXT,

                                     note              TEXT,
                                     created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     created_by        UUID NOT NULL REFERENCES agent(uuid),

                                     CONSTRAINT term_in_force_needs_paper CHECK (
                                         status NOT IN ('active','cancelled')
                                             OR source IN ('correction','migration')
                                             OR source_uuid IS NOT NULL
                                         ),
                                     CONSTRAINT term_cancel_facts CHECK (
                                         status <> 'cancelled'
                                             OR (ends_on IS NOT NULL AND cancelled_at IS NOT NULL
                                             AND cancel_reason IS NOT NULL AND ends_on > valid_at)
                                         ),
                                     CONSTRAINT term_void_facts CHECK (
                                         status <> 'void' OR (voided_at IS NOT NULL AND void_reason IS NOT NULL)
                                         ),
                                     CONSTRAINT term_ends_only_when_cancelled CHECK (
                                         ends_on IS NULL OR status = 'cancelled'
                                         )
);

CREATE UNIQUE INDEX tenancy_charge_term_in_force_uq
    ON tenancy_charge_term (tenancy, valid_at)
    WHERE status IN ('active','cancelled');

CREATE INDEX tenancy_charge_term_lookup_idx
    ON tenancy_charge_term (tenancy, valid_at DESC)
    WHERE status IN ('active','cancelled');

CREATE INDEX tenancy_charge_term_open_idx
    ON tenancy_charge_term (tenancy)
    WHERE status IN ('proposed','pending');