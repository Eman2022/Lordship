-- Seed data: core permissions, default roles, and role-permission assignments.
-- Depends on: agent_role, permission, role_permission (V3)

-- Core permissions
INSERT INTO permission (uuid, permission_name) VALUES
    -- Agents
    (uuidv7(), 'agents:view'),
    (uuidv7(), 'agents:edit'),
    (uuidv7(), 'agents:view_own'),
    (uuidv7(), 'agents:edit_own'),
    (uuidv7(), 'agents:create'),
    (uuidv7(), 'agents:delete'),

    -- Permissions
    (uuidv7(), 'permissions:view'),
    (uuidv7(), 'permissions:assign'),
    (uuidv7(), 'permissions:deny'),

    -- Roles
    (uuidv7(), 'agent_roles:view'),
    (uuidv7(), 'agent_roles:edit'),
    (uuidv7(), 'agent_roles:create'),
    (uuidv7(), 'agent_roles:delete'),

    -- Pets
    (uuidv7(), 'pets:view'),
    (uuidv7(), 'pets:edit'),
    (uuidv7(), 'pets:create'),
    (uuidv7(), 'pets:delete'),

    -- Properties
    (uuidv7(), 'properties:view'),
    (uuidv7(), 'properties:edit'),
    (uuidv7(), 'properties:create'),
    (uuidv7(), 'properties:delete'),

    -- Property assignments
    (uuidv7(), 'assignments:view'),
    (uuidv7(), 'assignments:assign'),  -- assign an agent to a property
    (uuidv7(), 'assignments:remove'),  -- remove an agent from a property

    -- Persons
    (uuidv7(), 'persons_ssn:view'),
    (uuidv7(), 'persons_ssn:edit'),
    (uuidv7(), 'persons:view'),
    (uuidv7(), 'persons:edit'),
    (uuidv7(), 'persons:view_own'), -- note: just because they have these two permissions
    (uuidv7(), 'persons:edit_own'), -- doesn't mean that things can be completely freely edited
    (uuidv7(), 'persons:create'),
    (uuidv7(), 'persons:delete'),

    -- Audit log
    (uuidv7(), 'audit:view'),          -- see the full audit trail
    (uuidv7(), 'audit:view_own'),      -- agents can only see their own history

    -- Tenancy
    (uuidv7(), 'tenancy:view'),
    (uuidv7(), 'tenancy:edit'),
    (uuidv7(), 'tenancy:view_own'),
    (uuidv7(), 'tenancy:create'),
    (uuidv7(), 'tenancy:delete');

-- Default roles
INSERT INTO agent_role (uuid, role_name) VALUES
    (uuidv7(), 'Admin'),
    (uuidv7(), 'Office Staff'),
    (uuidv7(), 'Unassigned'),
    (uuidv7(), 'Property Manager');


-- Admin gets all permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin';


-- Office Staff: internal ops, no property lifecycle or agent/role management
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Office Staff'
AND p.permission_name IN (
    'agents:view',
    'agents:view_own',
    'agents:edit_own',
    'permissions:view',
    'persons_ssn:view',
    'persons:view',
    'persons:edit',
    'persons:create',
    'persons:view_own',
    'persons:edit_own',
    'pets:view',
    'pets:edit',
    'pets:create',
    'pets:delete',
    'properties:view',
    'assignments:view',
    'audit:view',
    'audit:view_own'
);


-- Property Manager: operational access scoped to their work, no structural changes
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Property Manager'
AND p.permission_name IN (
    'agents:view_own',
    'agents:edit_own',
    'persons:view',
    'persons:edit',
    'persons:create',
    'persons:view_own',
    'persons:edit_own',
    'pets:view',
    'pets:edit',
    'pets:create',
    'pets:delete',
    'properties:view',
    'assignments:view',
    'audit:view_own'
);