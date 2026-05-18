
CREATE TABLE property (
                            property_code VARCHAR(5) PRIMARY KEY,
                            property_name TEXT NOT NULL,
                            property_address TEXT NOT NULL,
                            property_city VARCHAR(255),
                            property_state VARCHAR(2),
                            purchase_date DATE,
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
                        personal_phone VARCHAR(20),
                        personal_email VARCHAR(20),
                        primary_property VARCHAR(5),   -- Either the place of their tenancy, residence, or primary agent location
                        mailing_address TEXT,
                        emergency_contact UUID,
                        social VARCHAR(9),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        deleted_at TIMESTAMP,
                        FOREIGN KEY (emergency_contact) REFERENCES person(uuid),
                        FOREIGN KEY (primary_property) REFERENCES property(property_code),
);

CREATE TABLE pet (
  uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
    pet_name VARCHAR(120),
    pet_type VARCHAR(120), -- dog or cat or bird...
    pet_breed VARCHAR(120),
    owner UUID,
    FOREIGN KEY (owner) REFERENCES person(uuid)
);
CREATE INDEX idx_pet_owner ON pet(owner);


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

--CREATE TABLE MANAGER_OF...?

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
CREATE INDEX idx_assignment_agent ON assignment(agent_id);
CREATE INDEX idx_assignment_property ON assignment(property_code);


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

-- Seed data: core permissions available for assignment to roles.
INSERT INTO permission (uuid, permission_name) VALUES
                                                   (gen_uuid_v7(), 'roles:view'),
                                                   (gen_uuid_v7(), 'roles:edit'),
                                                   (gen_uuid_v7(), 'agents:view'),
                                                   (gen_uuid_v7(), 'agents:edit'),
                                                   (gen_uuid_v7(), 'persons:view'),
                                                   (gen_uuid_v7(), 'persons:edit');

CREATE TABLE audit_log (
                           uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
                           agent_id UUID NOT NULL,
                           ip_address VARCHAR(45),
                           table_name VARCHAR(60) NOT NULL,
                           record_id UUID NOT NULL,
                           operation VARCHAR(10) NOT NULL,
                           changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           delta JSONB,
                           FOREIGN KEY (agent_id) REFERENCES agent(uuid)
);

CREATE INDEX idx_audit_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_agent ON audit_log(agent_id);