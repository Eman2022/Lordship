-- Audit log for tracking all data changes made by agents.
-- Depends on: agent (V3)
CREATE TYPE user_type AS ENUM ('AGENT', 'TENANT', 'SYSTEM');
CREATE TYPE operation_type AS ENUM ('INSERT', 'UPDATE', 'DELETE');

CREATE TABLE audit_log (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    correlation_id UUID NOT NULL, -- groups all rows from one request
    user_id UUID, -- either agent or (in the future) tenant user - NULL for system change
    user_type user_type NOT NULL,
    ip_address VARCHAR(45),
    table_name VARCHAR(60) NOT NULL,
    record_id VARCHAR(60) NOT NULL,
    operation operation_type NOT NULL,
    value_before TEXT,-- before snapshot of changed fields
    value_after TEXT, -- after snapshot of changed fields
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_user  ON audit_log(user_id);