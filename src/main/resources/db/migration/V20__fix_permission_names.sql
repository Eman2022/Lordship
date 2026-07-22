-- Fix permission name mismatches between controller @PreAuthorize annotations and seeded permissions.
--
-- Problems addressed:
--   1. TenancyController used 'tenancies:*' (plural) but V5 only seeded 'tenancy:*' (singular).
--      These were patched manually in dev — this migration formalizes them.
--   2. TenantController used 'tenants:*' permissions that were never seeded at all.
--      The tenants module was added after V5 with no corresponding permission seed.

-- -------------------------------------------------------------------------
-- 1. Tenancies permissions (controller uses plural 'tenancies:*')
-- -------------------------------------------------------------------------

INSERT INTO permission (uuid, permission_name) VALUES
    (uuidv7(), 'tenancies:create'),
    (uuidv7(), 'tenancies:read'),
    (uuidv7(), 'tenancies:update'),
    (uuidv7(), 'tenancies:delete')
ON CONFLICT (permission_name) DO NOTHING;

-- -------------------------------------------------------------------------
-- 2. Tenants permissions (entirely missing — tenants module never seeded)
-- -------------------------------------------------------------------------

INSERT INTO permission (uuid, permission_name) VALUES
    (uuidv7(), 'tenants:create'),
    (uuidv7(), 'tenants:read'),
    (uuidv7(), 'tenants:update'),
    (uuidv7(), 'tenants:delete')
ON CONFLICT (permission_name) DO NOTHING;

-- -------------------------------------------------------------------------
-- 3. Grant all new permissions to Admin
-- -------------------------------------------------------------------------

INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Admin'
AND p.permission_name IN (
    'tenancies:create', 'tenancies:read', 'tenancies:update', 'tenancies:delete',
    'tenants:create', 'tenants:read', 'tenants:update', 'tenants:delete'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.uuid AND rp.permission_id = p.uuid AND rp.deleted_at IS NULL
);

-- -------------------------------------------------------------------------
-- 4. Grant tenancies/tenants read+create+update to Office Staff
-- -------------------------------------------------------------------------

INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Office Staff'
AND p.permission_name IN (
    'tenancies:create', 'tenancies:read', 'tenancies:update',
    'tenants:create', 'tenants:read', 'tenants:update'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.uuid AND rp.permission_id = p.uuid AND rp.deleted_at IS NULL
);

-- -------------------------------------------------------------------------
-- 5. Grant tenancies/tenants read to Property Manager
-- -------------------------------------------------------------------------

INSERT INTO role_permission (uuid, role_id, permission_id)
SELECT uuidv7(), r.uuid, p.uuid
FROM agent_role r, permission p
WHERE r.role_name = 'Property Manager'
AND p.permission_name IN (
    'tenancies:read', 'tenancies:update',
    'tenants:read', 'tenants:create', 'tenants:update'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.uuid AND rp.permission_id = p.uuid AND rp.deleted_at IS NULL
);
