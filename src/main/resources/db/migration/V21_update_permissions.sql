DELETE FROM role_permission
WHERE permission_id = (SELECT uuid FROM permission WHERE permission_name = 'tenancy:view_own');

DELETE FROM permission
WHERE permission_name = 'tenancy:view_own';

INSERT INTO permission (uuid, permission_name) VALUES
    -- Tenants
    (uuidv7(), 'tenants:view'),
    (uuidv7(), 'tenants:edit'),
    (uuidv7(), 'tenants:create'),
    (uuidv7(), 'tenants:delete'),

    -- Meters
    (uuidv7(), 'meters:view'),
    (uuidv7(), 'meters:edit'),
    (uuidv7(), 'meters:create'),
    (uuidv7(), 'meters:delete');


-- Admin gets all permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin'
  AND p.permission_name IN ('tenants:view', 'tenants:edit', 'tenants:create', 'tenants:delete',
                            'meters:view', 'meters:edit', 'meters:create', 'meters:delete');

-- Property Manager gets create/update permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Property Manager'
  AND p.permission_name IN ('tenants:view', 'tenants:edit', 'tenants:create', 'tenants:delete',
                            'meters:view', 'meters:edit', 'meters:create', 'meters:delete',
                            'tenancy:view', 'tenancy:edit', 'tenancy:create', 'tenancy:delete');

-- Office Staff gets view only
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Office Staff'
  AND p.permission_name IN ('tenants:view', 'meters:view', 'tenancy:view');