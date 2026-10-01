BEGIN;

-- 1. Insert Test Users
INSERT INTO accounts (account_type_id, email, is_active, created_by, modified_by)
VALUES
    (1, 'writer@test.io', true, 'TEST_SEED', 'TEST_SEED'),
    (1, 'reader@test.io', true, 'TEST_SEED', 'TEST_SEED')
ON CONFLICT (email) DO NOTHING;

-- 2. Assign Roles
-- Admin Role
INSERT INTO account_roles (account_id, role_id, created_by, modified_by)
SELECT a.id, r.id, 'TEST_SEED', 'TEST_SEED'
FROM accounts a, roles r
WHERE a.email = 'admin@test.io' AND r.name = 'ADMIN'
    ON CONFLICT (account_id, role_id) DO NOTHING;

-- Writer Role (e.g., USER or EDITOR)
INSERT INTO account_roles (account_id, role_id, created_by, modified_by)
SELECT a.id, r.id, 'TEST_SEED', 'TEST_SEED'
FROM accounts a, roles r
WHERE a.email = 'writer@test.io' AND r.name = 'WRITE'
    ON CONFLICT (account_id, role_id) DO NOTHING;

-- Reader Role
INSERT INTO account_roles (account_id, role_id, created_by, modified_by)
SELECT a.id, r.id, 'TEST_SEED', 'TEST_SEED'
FROM accounts a, roles r
WHERE a.email = 'reader@test.io' AND r.name = 'READ'
    ON CONFLICT (account_id, role_id) DO NOTHING;

COMMIT;