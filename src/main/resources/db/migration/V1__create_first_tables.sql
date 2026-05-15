-- Represents a real-world person. May exist in the system before they have any
-- agent account -- e.g. a resident or contact added manually.
CREATE TABLE person (
                        uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
                        name_raw VARCHAR(120),
                        name_first VARCHAR(60),
                        name_last VARCHAR(60),
                        birthday DATE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        deleted_at TIMESTAMP
);

-- A person who can log in and act within the system (staff, managers, etc).
-- Linked to a person record. Credentials live here, identity lives in person.
CREATE TABLE agent (
                       uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
                       person_id UUID NOT NULL,
                       agent_email VARCHAR(120),
                       agent_password VARCHAR(255),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP,
                       FOREIGN KEY (person_id) REFERENCES person(uuid)
);

-- Ensures no two active agents share the same login email.
CREATE UNIQUE INDEX uq_agent_email_active
    ON agent (agent_email) WHERE deleted_at IS NULL;

-- A named role that can be assigned to agents (e.g. "Manager", "Maintenance").
-- Used with granted_role and role_permission.
CREATE TABLE agent_role (
                            uuid UUID PRIMARY KEY DEFAULT gen_uuid_v7(),
                            role_name VARCHAR(60) NOT NULL UNIQUE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            deleted_at TIMESTAMP
);

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