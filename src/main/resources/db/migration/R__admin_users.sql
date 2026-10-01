-- Repeatable migration for admin USER accounts (Google OAuth, no client secret).
-- Re-runs automatically when this file changes (new admin added, role changed, etc.)
-- ON CONFLICT DO NOTHING ensures existing accounts are never modified by a re-run.
--
-- HOW TO ADD A NEW ADMIN:
--   1. Add a block below (INSERT into accounts + account_roles)
--   2. The email must belong to an allowed Google domain (google.configuration.allowed-domains)
--
-- ROLES: READ / WRITE / ADMIN (hierarchy: ADMIN > WRITE > READ)

BEGIN;

-- admin@example.com
INSERT INTO accounts (account_type_id, email, is_active, created_by, modified_by)
VALUES (1, 'admin@example.com', true, 'SYSTEM', 'SYSTEM')
ON CONFLICT (email) DO NOTHING;

INSERT INTO account_roles (account_id, role_id, created_by, modified_by)
SELECT a.id, r.id, 'SYSTEM', 'SYSTEM'
FROM accounts a
         JOIN roles r ON r.name = 'ADMIN'
WHERE a.email = 'admin@example.com'
ON CONFLICT (account_id, role_id) DO NOTHING;

COMMIT;