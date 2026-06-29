-- Transaction permissions seeded into the permission table.
-- Assigns transactions:view and transactions:edit to roles.
-- Depends on: permission, agent_role, role_permission (V3), seed data (V5)

INSERT INTO permission (uuid, permission_name) VALUES
    (uuidv7(), 'transactions:view'),
    (uuidv7(), 'transactions:edit');

-- Admin gets all permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin'
  AND p.permission_name IN ('transactions:view', 'transactions:edit');

-- Office Staff gets both transaction permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Office Staff'
  AND p.permission_name IN ('transactions:view', 'transactions:edit');

-- Property Manager gets view only
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Property Manager'
  AND p.permission_name = 'transactions:view';
