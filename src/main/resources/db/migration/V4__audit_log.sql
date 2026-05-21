-- Audit log for tracking all data changes made by agents.
-- Depends on: agent (V3)

CREATE TABLE audit_log (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id UUID NOT NULL,
    ip_address VARCHAR(45),
    table_name VARCHAR(60) NOT NULL,
    record_id VARCHAR(60) NOT NULL,
    operation VARCHAR(10) NOT NULL, -- e.g. INSERT, UPDATE, DELETE
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delta JSONB,                    -- before/after snapshot of changed fields
    FOREIGN KEY (agent_id) REFERENCES agent(uuid)
);

CREATE INDEX idx_audit_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_agent  ON audit_log(agent_id);