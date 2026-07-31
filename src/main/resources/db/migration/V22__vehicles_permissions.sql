-- Vehicle permissions
INSERT INTO permission (uuid, permission_name) VALUES
    (uuidv7(), 'vehicles:view'),
    (uuidv7(), 'vehicles:edit'),
    (uuidv7(), 'vehicles:create'),
    (uuidv7(), 'vehicles:delete');

-- Admin gets all vehicle permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin'
  AND p.permission_name IN ('vehicles:view', 'vehicles:edit', 'vehicles:create', 'vehicles:delete');

-- Office Staff gets all vehicle permissions
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Office Staff'
  AND p.permission_name IN ('vehicles:view', 'vehicles:edit', 'vehicles:create', 'vehicles:delete');

-- Property Manager gets view only
INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Property Manager'
  AND p.permission_name = 'vehicles:view';