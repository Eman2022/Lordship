-- Account permissions seeded into the permission table.
-- Assigns accounts:view and accounts:edit to Admin and Office Staff roles.
-- Depends on: permission, agent_role, role_permission (V3), seed data (V5)

INSERT INTO permission (uuid, permission_name) VALUES
    (uuidv7(), 'accounts:view'),
    (uuidv7(), 'accounts:edit');

-- Admin gets all permissions (including the two new ones)
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin'
  AND p.permission_name IN ('accounts:view', 'accounts:edit');

-- Office Staff gets both account permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Office Staff'
  AND p.permission_name IN ('accounts:view', 'accounts:edit');

-- Property Manager gets view only
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Property Manager'
  AND p.permission_name = 'accounts:view';
