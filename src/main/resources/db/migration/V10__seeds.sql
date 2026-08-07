-- ============================================================
-- V2: All seed data — permissions, roles, and role grants.
-- Consolidates V5, V7, V10, V17, V20, V23, V24.
-- Also fixes V24's missing role grants for meters:*.
-- ============================================================


-- ── Permissions ───────────────────────────────────────────────────────────────

INSERT INTO permission (uuid, permission_name) VALUES
    -- Agents
    (uuidv7(), 'agents:view'),
    (uuidv7(), 'agents:edit'),
    (uuidv7(), 'agents:reset_passwords'),
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
    (uuidv7(), 'assignments:assign-all'),
    (uuidv7(), 'assignments:view'),
    (uuidv7(), 'assignments:assign'),
    (uuidv7(), 'assignments:remove'),

    -- Persons
    (uuidv7(), 'persons_ssn:view'),
    (uuidv7(), 'persons_ssn:edit'),
    (uuidv7(), 'persons:view'),
    (uuidv7(), 'persons:edit'),
    (uuidv7(), 'persons:view_own'),
    (uuidv7(), 'persons:edit_own'),
    (uuidv7(), 'persons:create'),
    (uuidv7(), 'persons:delete'),

    -- Audit
    (uuidv7(), 'audit:view'),
    (uuidv7(), 'audit:view_own'),

    -- Tenancy
    (uuidv7(), 'tenancy:view'),
    (uuidv7(), 'tenancy:edit'),
    (uuidv7(), 'tenancy:create'),
    (uuidv7(), 'tenancy:delete'),

    -- Tenants
    (uuidv7(), 'tenants:create'),
    (uuidv7(), 'tenants:view'),
    (uuidv7(), 'tenants:edit'),
    (uuidv7(), 'tenants:delete'),

    -- Lots
    (uuidv7(), 'lots:view'),
    (uuidv7(), 'lots:edit'),
    (uuidv7(), 'lots:create'),
    (uuidv7(), 'lots:delete'),

    -- Accounts
    (uuidv7(), 'accounts:view'),
    (uuidv7(), 'accounts:edit'),

    -- Transactions
    (uuidv7(), 'transactions:view'),
    (uuidv7(), 'transactions:edit'),

    -- Vehicles
    (uuidv7(), 'vehicles:view'),
    (uuidv7(), 'vehicles:edit'),
    (uuidv7(), 'vehicles:create'),
    (uuidv7(), 'vehicles:delete'),

    -- Meters
    (uuidv7(), 'meters:view'),
    (uuidv7(), 'meters:edit'),
    (uuidv7(), 'meters:create'),
    (uuidv7(), 'meters:delete'),

    -- Meter Billing
    (uuidv7(), 'meterbills:view'),
    (uuidv7(), 'meterbills:edit'),
    (uuidv7(), 'meterbills:create'),
    (uuidv7(), 'meterbills:delete'),

    -- charge term
    (uuidv7(), 'charge_term:view'),
    (uuidv7(), 'charge_term:edit'),
    (uuidv7(), 'charge_term:create'),
    (uuidv7(), 'charge_term:delete')
;

-- ── Roles ─────────────────────────────────────────────────────────────────────

INSERT INTO agent_role (uuid, role_name) VALUES
    (uuidv7(), 'Admin'),
    (uuidv7(), 'Office Staff'),
    (uuidv7(), 'Unassigned'),
    (uuidv7(), 'Property Manager');


-- ── Role → Permission grants ──────────────────────────────────────────────────

-- Admin gets all permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin';

-- Office Staff
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
    'assignments:assign-all',
    'audit:view',
    'audit:view_own',
    'accounts:view',
    'accounts:edit',
    'transactions:view',
    'transactions:edit',
    'tenancy:create',
    'tenancy:view',
    'tenancy:edit',
    'tenants:create',
    'tenants:view',
    'tenants:edit',
    'vehicles:view',
    'vehicles:edit',
    'vehicles:create',
    'vehicles:delete',
    'meters:view',
    'meterbills:view'
);

-- Property Manager
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
    'audit:view',
    'audit:view_own',
    'accounts:view',
    'transactions:view',
    'tenancy:view',
    'tenancy:edit',
    'tenants:view',
    'tenants:create',
    'tenants:edit',
    'vehicles:view',
    'meters:view',
    'meters:edit',
    'meterbills:view',
    'meterbills:edit'
);
