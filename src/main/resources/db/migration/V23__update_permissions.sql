DELETE FROM role_permission
WHERE permission_id = (SELECT uuid FROM permission WHERE permission_name = 'tenancy:view_own');

DELETE FROM permission
WHERE permission_name = 'tenancy:view_own';

INSERT INTO permission (uuid, permission_name) VALUES
    -- Tenants
  --  (uuidv7(), 'tenants:view'),
  --  (uuidv7(), 'tenants:edit'),
  --  (uuidv7(), 'tenants:create'),
  --  (uuidv7(), 'tenants:delete'),

    -- Meters
    (uuidv7(), 'meters:view'),
    (uuidv7(), 'meters:edit'),
    (uuidv7(), 'meters:create'),
    (uuidv7(), 'meters:delete');