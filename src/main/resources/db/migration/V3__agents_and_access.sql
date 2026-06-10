-- Agents, roles, permissions, and all access control tables.
-- Depends on: person (V1), property (V1)

-- A person who can log in and act within the system (staff, managers, etc).
-- Credentials live here, identity lives in person.
CREATE TABLE agent (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    person_id UUID NOT NULL,
    work_phone VARCHAR(20),
    work_email VARCHAR(120),
    agent_password VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (person_id) REFERENCES person(uuid)
);

CREATE INDEX idx_agent_person_id ON agent(person_id) WHERE deleted_at IS NULL;

-- Ensures no two active agents share the same login email.
CREATE UNIQUE INDEX uq_agent_email_active
    ON agent (work_email) WHERE deleted_at IS NULL;


-- A named role that can be assigned to agents (e.g. "Admin", "Office Staff").
CREATE TABLE agent_role (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    role_name VARCHAR(60) NOT NULL UNIQUE,
    role_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);


-- A discrete action or access right within the system (e.g. "agents:edit").
CREATE TABLE permission (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    permission_name VARCHAR(60) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);


-- Assigns permissions to roles. An agent gains a permission by holding a role that includes it.
CREATE TABLE role_permission (
     uuid UUID PRIMARY KEY DEFAULT uuidv7(),
     role_id UUID NOT NULL,
     permission_id UUID NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     deleted_at TIMESTAMP,
     FOREIGN KEY (role_id) REFERENCES agent_role(uuid),
     FOREIGN KEY (permission_id) REFERENCES permission(uuid)
);

-- Prevents duplicate active assignments of the same permission to the same role.
CREATE UNIQUE INDEX uq_role_permission_active
    ON role_permission (role_id, permission_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_role_permission_role_id ON role_permission(role_id) WHERE deleted_at IS NULL;


-- Assigns a role to an agent. An agent may hold multiple roles.
-- granted_by is self-referential for the bootstrap admin (granted_by = agent_id).
CREATE TABLE granted_role (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id UUID NOT NULL,
    role_id UUID NOT NULL,
    granted_by UUID NOT NULL,
    revoked_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agent(uuid),
    FOREIGN KEY (role_id) REFERENCES agent_role(uuid),
    FOREIGN KEY (granted_by) REFERENCES agent(uuid),
    FOREIGN KEY (revoked_by) REFERENCES agent(uuid)
);

-- Prevents the same role from being granted to the same agent more than once.
CREATE UNIQUE INDEX uq_granted_role_active
    ON granted_role (agent_id, role_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_granted_role_agent_id ON granted_role(agent_id) WHERE deleted_at IS NULL;


-- Explicitly revokes a specific permission from an agent, overriding any roles
-- that would otherwise grant it. Denial always wins. Records who issued the denial.
CREATE TABLE denied_permission (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    denied_by UUID NOT NULL,
    denial_removed_by UUID, -- who undid the permission denial?
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,   -- soft delete = denial lifted; should be audited
    FOREIGN KEY (agent_id) REFERENCES agent(uuid),
    FOREIGN KEY (denial_removed_by) REFERENCES agent(uuid),
    FOREIGN KEY (permission_id) REFERENCES permission(uuid),
    FOREIGN KEY (denied_by) REFERENCES agent(uuid)
);

-- Prevents the same permission from being denied to the same agent more than once.
CREATE UNIQUE INDEX uq_denied_permission_active
    ON denied_permission (agent_id, permission_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_denied_permission_agent_id ON denied_permission(agent_id) WHERE deleted_at IS NULL;

-- Assigns an agent to a specific property. Tracks who made the assignment.
CREATE TABLE agent_property_assignment (
    uuid UUID PRIMARY KEY DEFAULT uuidv7(),
    agent_id UUID NOT NULL,
    property_code VARCHAR(5) NOT NULL,
    assigned_by UUID NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agent(uuid),
    FOREIGN KEY (property_code) REFERENCES property(property_code),
    FOREIGN KEY (assigned_by) REFERENCES agent(uuid)
);

CREATE INDEX idx_assignment_agent    ON agent_property_assignment(agent_id)      WHERE removed_at IS NULL;
CREATE INDEX idx_assignment_property ON agent_property_assignment(property_code) WHERE removed_at IS NULL;