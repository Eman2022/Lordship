-- ============================================================
-- V1: Full schema in final form.
-- Consolidates V1–V24. No ALTER TABLE or renames — this is
-- the resolved end state of all previous migrations.
-- ============================================================


-- ── Enums ────────────────────────────────────────────────────────────────────

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
    property_manager UUID,                                  -- FK added below after agent table
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP
);


-- ── Person ───────────────────────────────────────────────────────────────────

CREATE TABLE person (
    uuid              UUID PRIMARY KEY DEFAULT uuidv7(),
    name_raw          VARCHAR(120),
    name_full         VARCHAR(120),
    birthday          DATE,
    personal_phone    VARCHAR(120),
    personal_email    VARCHAR(120),
    mailing_address   TEXT,
    emergency_contact UUID,
    social            VARCHAR(72),   -- AES-256 encrypted SSN
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    FOREIGN KEY (emergency_contact) REFERENCES person(uuid)
);

CREATE INDEX idx_person_name_full ON person(LOWER(name_full))      WHERE deleted_at IS NULL;
CREATE INDEX idx_person_email     ON person(LOWER(personal_email)) WHERE deleted_at IS NULL;

CREATE TABLE person_relationship (
    uuid         UUID PRIMARY KEY DEFAULT uuidv7(),
    person_1     UUID NOT NULL,
    person_2     UUID NOT NULL,
    relationship VARCHAR(40) NOT NULL,
    strength     NUMERIC(6,4),
    document_id  UUID,
    FOREIGN KEY (person_1) REFERENCES person(uuid),
    FOREIGN KEY (person_2) REFERENCES person(uuid)
);


-- ── Pet ──────────────────────────────────────────────────────────────────────

CREATE TABLE pet (
    uuid       UUID PRIMARY KEY DEFAULT uuidv7(),
    pet_name   VARCHAR(120),
    pet_type   VARCHAR(120),
    pet_breed  VARCHAR(120),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);


-- ── Agent & Access Control ───────────────────────────────────────────────────

CREATE TABLE agent (
    uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
    person_id      UUID NOT NULL,
    work_phone     VARCHAR(20),
    work_email     VARCHAR(120),
    agent_password VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    FOREIGN KEY (person_id) REFERENCES person(uuid)
);

CREATE INDEX idx_agent_person_id ON agent(person_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_agent_email_active ON agent(work_email) WHERE deleted_at IS NULL;

CREATE TABLE agent_login_event (
    uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id       UUID NOT NULL,
    occurred_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address     VARCHAR(45),
    browser_client TEXT,
    browserOs      TEXT,
    outcome        SMALLINT NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agent(uuid)
);

CREATE INDEX idx_login_event_agent ON agent_login_event(agent_id, occurred_at DESC);

CREATE TABLE agent_role (
    uuid             UUID PRIMARY KEY DEFAULT uuidv7(),
    role_name        VARCHAR(60) NOT NULL UNIQUE,
    role_description TEXT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP
);

CREATE TABLE permission (
    uuid            UUID PRIMARY KEY DEFAULT uuidv7(),
    permission_name VARCHAR(60) NOT NULL UNIQUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE TABLE role_permission (
    uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    FOREIGN KEY (role_id)       REFERENCES agent_role(uuid),
    FOREIGN KEY (permission_id) REFERENCES permission(uuid)
);

CREATE UNIQUE INDEX uq_role_permission_active
    ON role_permission(role_id, permission_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_role_permission_role_id ON role_permission(role_id) WHERE deleted_at IS NULL;

CREATE TABLE granted_role (
    uuid       UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id   UUID NOT NULL,
    role_id    UUID NOT NULL,
    granted_by UUID NOT NULL,
    revoked_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (agent_id)   REFERENCES agent(uuid),
    FOREIGN KEY (role_id)    REFERENCES agent_role(uuid),
    FOREIGN KEY (granted_by) REFERENCES agent(uuid),
    FOREIGN KEY (revoked_by) REFERENCES agent(uuid)
);

CREATE UNIQUE INDEX uq_granted_role_active
    ON granted_role(agent_id, role_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_granted_role_agent_id ON granted_role(agent_id) WHERE deleted_at IS NULL;

CREATE TABLE denied_permission (
    uuid              UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id          UUID NOT NULL,
    permission_id     UUID NOT NULL,
    denied_by         UUID NOT NULL,
    denial_removed_by UUID,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    FOREIGN KEY (agent_id)          REFERENCES agent(uuid),
    FOREIGN KEY (permission_id)     REFERENCES permission(uuid),
    FOREIGN KEY (denied_by)         REFERENCES agent(uuid),
    FOREIGN KEY (denial_removed_by) REFERENCES agent(uuid)
);

CREATE UNIQUE INDEX uq_denied_permission_active
    ON denied_permission(agent_id, permission_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_denied_permission_agent_id ON denied_permission(agent_id) WHERE deleted_at IS NULL;

CREATE TABLE agent_property_assignment (
    uuid        UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id    UUID NOT NULL,
    property_id UUID NOT NULL,
    assigned_by UUID NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at  TIMESTAMP,
    FOREIGN KEY (agent_id)    REFERENCES agent(uuid),
    FOREIGN KEY (property_id) REFERENCES property(uuid),
    FOREIGN KEY (assigned_by) REFERENCES agent(uuid)
);

CREATE INDEX idx_assignment_agent    ON agent_property_assignment(agent_id)    WHERE removed_at IS NULL;
CREATE INDEX idx_assignment_property ON agent_property_assignment(property_id) WHERE removed_at IS NULL;
CREATE UNIQUE INDEX uq_active_assignment
    ON agent_property_assignment(agent_id, property_id) WHERE removed_at IS NULL;

-- property_manager FK deferred until after agent table exists
ALTER TABLE property
    ADD CONSTRAINT fk_property_manager FOREIGN KEY (property_manager) REFERENCES agent(uuid);


-- ── Audit Log ────────────────────────────────────────────────────────────────

CREATE TABLE audit_log (
    uuid           UUID PRIMARY KEY DEFAULT uuidv7(),
    correlation_id UUID NOT NULL,
    user_id        UUID,
    user_type      user_type NOT NULL,
    ip_address     VARCHAR(45),
    table_name     VARCHAR(60) NOT NULL,
    record_id      VARCHAR(60) NOT NULL,
    operation      operation_type NOT NULL,
    value_before   TEXT,
    value_after    TEXT,
    changed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_user   ON audit_log(user_id);


-- ── Property Info ─────────────────────────────────────────────────────────────

CREATE TABLE property_contact (
    uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
    property_id   UUID NOT NULL,
    person_uuid   UUID NOT NULL,
    contact_phone VARCHAR(120),
    contact_email VARCHAR(120),
    description   TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES property(uuid),
    FOREIGN KEY (person_uuid) REFERENCES person(uuid)
);

CREATE TABLE property_link (
    uuid        UUID PRIMARY KEY DEFAULT uuidv7(),
    property_id UUID NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    url         TEXT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES property(uuid)
);

CREATE INDEX idx_property_contact_property ON property_contact(property_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_property_contact_person   ON property_contact(person_uuid) WHERE deleted_at IS NULL;
CREATE INDEX idx_property_link_property    ON property_link(property_id)    WHERE deleted_at IS NULL;


-- ── Lots ─────────────────────────────────────────────────────────────────────

CREATE TABLE lot_type (
    code        CHAR(3) PRIMARY KEY,
    label       TEXT NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT
);

INSERT INTO lot_type (code, label, sort_order) VALUES
    ('RCC', 'On Contract',        1),
    ('REN', 'Rental',             2),
    ('REC', 'RV',                 3),
    ('VCI', 'Vacant Iron',        4),
    ('VAL', 'Vacant Lot',         5),
    ('VRV', 'Vacant RV Lot',      6),
    ('RSB', 'Rental Stick Built', 7),
    ('VSB', 'Vacant Stick Built', 8),
    ('OOC', 'Owner Occupied',     9),
    ('EXT', 'Extra Lot',         10);

CREATE TABLE lot (
    uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
    property_id   UUID NOT NULL,
    lot_number    TEXT NOT NULL,
    lot_type_code CHAR(3),
    description   TEXT,
    notes         TEXT,
    sort_order    INT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    FOREIGN KEY (property_id)   REFERENCES property(uuid),
    FOREIGN KEY (lot_type_code) REFERENCES lot_type(code)
);

CREATE INDEX idx_lot_number_per_property
    ON lot(property_id, LOWER(lot_number)) WHERE deleted_at IS NULL;
CREATE INDEX idx_lot_property ON lot(property_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_lot_type     ON lot(lot_type_code);

CREATE TABLE lot_map (
    lot_id     UUID PRIMARY KEY,
    vertices   JSONB NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lot_id) REFERENCES lot(uuid)
);


-- ── Tenancy ───────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE tenancy (
    uuid                  UUID PRIMARY KEY DEFAULT uuidv7(),
    lot_id                UUID NOT NULL,
    start_date            DATE,
    end_date              DATE,
    no_personal_checks    BOOLEAN NOT NULL DEFAULT FALSE,
    no_partial_payments   BOOLEAN NOT NULL DEFAULT FALSE,
    accept_payments       BOOLEAN NOT NULL DEFAULT TRUE,
    exempt_from_late_fees BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMP,
    FOREIGN KEY (lot_id) REFERENCES lot(uuid)
);

CREATE UNIQUE INDEX uix_tenancy_lot_active
    ON tenancy(lot_id) WHERE end_date IS NULL AND deleted_at IS NULL;

CREATE TRIGGER trg_tenancy_updated_at
    BEFORE UPDATE ON tenancy
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE tenant (
    uuid       UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancy_id UUID NOT NULL,
    person_id  UUID NOT NULL,
    start_date DATE,
    end_date   DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (person_id)  REFERENCES person(uuid),
    FOREIGN KEY (tenancy_id) REFERENCES tenancy(uuid)
);


-- ── Account ───────────────────────────────────────────────────────────────────

CREATE TABLE account (
    uuid            UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancy_id      UUID NOT NULL,
    account_status  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    balance_cached  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    autopay_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    FOREIGN KEY (tenancy_id) REFERENCES tenancy(uuid)
);

CREATE UNIQUE INDEX uq_account_tenancy_active
    ON account(tenancy_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_account_tenancy_id ON account(tenancy_id) WHERE deleted_at IS NULL;


-- ── Transaction ───────────────────────────────────────────────────────────────

CREATE TABLE transaction (
    uuid           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id     UUID NOT NULL REFERENCES account(uuid),
    type           VARCHAR(50) NOT NULL,
    amount         NUMERIC(14,2) NOT NULL,
    description    TEXT,
    billing_period DATE NOT NULL,
    posted_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    CONSTRAINT transaction_amount_check
        CHECK (amount <> 0 AND (amount > 0 OR type = 'BALANCE_ADJUSTMENT'))
);

CREATE INDEX idx_transaction_account_id     ON transaction(account_id);
CREATE INDEX idx_transaction_billing_period ON transaction(billing_period);


-- ── Vehicles ──────────────────────────────────────────────────────────────────

CREATE TABLE vehicle (
    uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
    tenancy_uuid  UUID NOT NULL,
    property_uuid UUID NOT NULL,
    make          VARCHAR(100),
    model         VARCHAR(100),
    year          INT,
    plate_number  VARCHAR(20) NOT NULL,
    plate_state   VARCHAR(2),
    color         VARCHAR(50),
    notes         TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    FOREIGN KEY (tenancy_uuid)  REFERENCES tenancy(uuid),
    FOREIGN KEY (property_uuid) REFERENCES property(uuid)
);

CREATE TABLE vehicle_policy (
    uuid               UUID PRIMARY KEY DEFAULT uuidv7(),
    property_uuid      UUID NOT NULL UNIQUE,
    free_vehicle_limit INT NOT NULL DEFAULT 2,
    extra_vehicle_fee  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    notes              TEXT,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (property_uuid) REFERENCES property(uuid)
);

CREATE INDEX idx_vehicle_tenancy  ON vehicle(tenancy_uuid)  WHERE deleted_at IS NULL;
CREATE INDEX idx_vehicle_property ON vehicle(property_uuid) WHERE deleted_at IS NULL;


-- ── Meters ────────────────────────────────────────────────────────────────────

CREATE TABLE meter_type (
    code        TEXT PRIMARY KEY,
    description TEXT
);

CREATE TABLE meter_measurement (
    code        TEXT PRIMARY KEY,
    description TEXT
);

CREATE TABLE meters (
    uuid          UUID PRIMARY KEY DEFAULT uuidv7(),
    meter_id      UUID NOT NULL,
    title         TEXT,
    description   VARCHAR(255),
    serial_number VARCHAR(255) UNIQUE,
    point_x       DOUBLE PRECISION NOT NULL,
    point_y       DOUBLE PRECISION NOT NULL,
    utility_type  TEXT NOT NULL REFERENCES meter_type(code),
    measurement   TEXT NOT NULL REFERENCES meter_measurement(code),
    installed_at  DATE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    FOREIGN KEY (meter_id) REFERENCES lot(uuid)
);
