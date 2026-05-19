
CREATE TABLE property (
    property_code VARCHAR(5) PRIMARY KEY,
    property_name TEXT NOT NULL,
    property_address TEXT NOT NULL,
    property_city VARCHAR(255),
    property_state VARCHAR(2),
    purchase_date DATE,
    deleted_at TIMESTAMP,
    year_built INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- Represents a real-world person. May exist in the system before they have any
-- agent account OR are attached to a tenancy-- e.g. a resident or contact added manually.
CREATE TABLE person (
    uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
    name_raw VARCHAR(120),
    name_first VARCHAR(60),
    name_last VARCHAR(60),
    birthday DATE,
    personal_phone VARCHAR(120),
    personal_email VARCHAR(20),
    primary_property VARCHAR(5),   -- Either the place of their tenancy, residence, or primary agent location
    mailing_address TEXT,
    emergency_contact UUID,
    social VARCHAR(9),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (emergency_contact) REFERENCES person(uuid),
    FOREIGN KEY (primary_property) REFERENCES property(property_code)
);

CREATE INDEX idx_person_name_last ON person(LOWER(name_last)) WHERE deleted_at IS NULL;
CREATE INDEX idx_person_name_first ON person(LOWER(name_first)) WHERE deleted_at IS NULL;
CREATE INDEX idx_person_email ON person(LOWER(personal_email)) WHERE deleted_at IS NULL;

CREATE TABLE pet (
    uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
    pet_name VARCHAR(120),
    pet_type VARCHAR(120), -- dog or cat or bird...
    pet_breed VARCHAR(120),
    deleted_at TIMESTAMP,
    owner UUID,
    FOREIGN KEY (owner) REFERENCES person(uuid)
);

CREATE INDEX idx_pet_owner ON pet(owner) WHERE deleted_at IS NULL;

-- A person who can log in and act within the system (staff, managers, etc).
-- Linked to a person record. Credentials live here, identity lives in person.
CREATE TABLE agent (
   uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
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

-- A named role that can be assigned to agents (e.g. "Manager", "Maintenance").
-- Used with granted_role and role_permission.
CREATE TABLE agent_role (
    uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
    role_name VARCHAR(60) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- assigning an agent to a specific property
CREATE TABLE agent_property_assignment (
    uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
    agent_id UUID NOT NULL,
    property_code VARCHAR(5) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP,
    assigned_by UUID NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agent(uuid),
    FOREIGN KEY (property_code) REFERENCES property(property_code),
    FOREIGN KEY (assigned_by) REFERENCES agent(uuid)
);
CREATE INDEX idx_assignment_agent ON agent_property_assignment(agent_id) WHERE removed_at is NULL;
CREATE INDEX idx_assignment_property ON agent_property_assignment(property_code) WHERE removed_at is NULL;


-- A discrete action or access right within the system (e.g. "agents:edit").
-- Used with role_permission and denied_permission.
CREATE TABLE permission (
    uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
    permission_name VARCHAR(60) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Assigns permissions to roles. An agent gains a permission by holding a role
-- that includes it. Used with agent_role and permission.
CREATE TABLE role_permission (
    uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
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
-- Looking up all role_permissions for a given role
CREATE INDEX idx_role_permission_role_id ON role_permission(role_id) WHERE deleted_at IS NULL;


-- Assigns a role to an agent. An agent may hold multiple roles.
-- Used with agent and agent_role.
CREATE TABLE granted_role (
    uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
    agent_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agent(uuid),
    FOREIGN KEY (role_id) REFERENCES agent_role(uuid)
);

-- Prevents the same role from being granted to the same agent more than once.
CREATE UNIQUE INDEX uq_granted_role_active
    ON granted_role (agent_id, role_id) WHERE deleted_at IS NULL;
-- Looking up all granted_roles for an agent
CREATE INDEX idx_granted_role_agent_id ON granted_role(agent_id) WHERE deleted_at is NULL;


-- Explicitly revokes a specific permission from an agent, overriding any roles
-- that would otherwise grant it. Records who issued the denial for audit purposes.
-- Used with agent and permission.
CREATE TABLE denied_permission (
   uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
   agent_id UUID NOT NULL,
   permission_id UUID NOT NULL,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   denied_by UUID NOT NULL,
   deleted_at TIMESTAMP,
   FOREIGN KEY (agent_id) REFERENCES agent(uuid),
   FOREIGN KEY (permission_id) REFERENCES permission(uuid),
   FOREIGN KEY (denied_by) REFERENCES agent(uuid)
);

-- Prevents the same permission from being denied to the same agent more than once.
CREATE UNIQUE INDEX uq_denied_permission_active
    ON denied_permission (agent_id, permission_id) WHERE deleted_at IS NULL;
-- Looking up all denied_permissions for an agent
CREATE INDEX idx_denied_permission_agent_id ON denied_permission(agent_id) WHERE deleted_at IS NULL;

CREATE TABLE audit_log (
   uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
   agent_id UUID NOT NULL,
   ip_address VARCHAR(45),
   table_name VARCHAR(60) NOT NULL,
   record_id VARCHAR(60) NOT NULL,
   operation VARCHAR(10) NOT NULL,
   changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   delta JSONB,
   FOREIGN KEY (agent_id) REFERENCES agent(uuid)
);

CREATE INDEX idx_audit_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_agent ON audit_log(agent_id);

-- Seed data: core permissions available for assignment to roles.
INSERT INTO permission (uuid, permission_name) VALUES
-- Agents
(gen_uuid_v7(), 'agents:view'),
(gen_uuid_v7(), 'agents:edit'),

-- Permissions
(gen_uuid_v7(), 'permissions:view'),
(gen_uuid_v7(), 'permissions:assign'),
(gen_uuid_v7(), 'permissions:deny'),

-- Roles
(gen_uuid_v7(), 'roles:view'),
(gen_uuid_v7(), 'roles:edit'),

-- Pets
(gen_uuid_v7(), 'pets:view'),
(gen_uuid_v7(), 'pets:edit'),

-- Properties
(gen_uuid_v7(), 'properties:view'),
(gen_uuid_v7(), 'properties:edit'),

-- Property assignments
(gen_uuid_v7(), 'assignments:view'),
(gen_uuid_v7(), 'assignments:assign'),  -- assign an agent to a property
(gen_uuid_v7(), 'assignments:remove'),  -- remove an agent from a property

-- Person
(gen_uuid_v7(), 'persons_ssn:view'),
(gen_uuid_v7(), 'persons_ssn:edit'),
(gen_uuid_v7(), 'persons:view'),
(gen_uuid_v7(), 'persons:edit'),

-- Audit Log
(gen_uuid_v7(), 'audit:view'),          -- see the audit trail
(gen_uuid_v7(), 'audit:view_own');      -- agents can only see their own history

-- Create some default, basic roles
INSERT INTO agent_role (uuid, role_name) VALUES
    (gen_uuid_v7(), 'Admin'),
    (gen_uuid_v7(), 'Office Staff'),
    (gen_uuid_v7(), 'Property Manager');

-- Give all permissions to Admin
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT gen_uuid_v7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin';

-- Give most permissions to Office Staff
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT gen_uuid_v7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Office Staff'
AND p.permission_name IN (
    'agents:view',
    'agents:edit',
    'persons:view',
    'persons:edit',
    'pets:view',
    'pets:edit',
    'properties:view',
    'properties:edit',
    'assignments:view',
    'assignments:assign',
    'assignments:remove',
    'audit:view',
    'audit:view_own'
);

-- Property Manager
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT gen_uuid_v7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Property Manager'
  AND p.permission_name IN (
    'persons:view',
    'pets:view',
    'properties:view',
    'assignments:view',
    'audit:view_own'
    );