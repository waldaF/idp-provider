BEGIN;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS citext;

-- Account Types
CREATE TABLE IF NOT EXISTS account_types (
                                             id          SERIAL PRIMARY KEY,
                                             name        VARCHAR(50) UNIQUE NOT NULL,
                                             created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             created_by  VARCHAR(100),
                                             modified_by VARCHAR(100)
);

-- Accounts
CREATE TABLE IF NOT EXISTS accounts (
                                        id              SERIAL PRIMARY KEY,
                                        guid            UUID        NOT NULL DEFAULT gen_random_uuid(),
                                        account_type_id INTEGER     NOT NULL REFERENCES account_types (id),
                                        email           CITEXT UNIQUE,
                                        service_name        TEXT UNIQUE,
                                        client_secret_hash  TEXT,
                                        is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
                                        created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        created_by      VARCHAR(100),
                                        modified_by     VARCHAR(100),

                                        CONSTRAINT ux_accounts_public_uuid UNIQUE (guid),
                                        CONSTRAINT ck_account_identifier CHECK (
                                            (account_type_id = 1 AND email IS NOT NULL AND service_name IS NULL)
                                                OR
                                            (account_type_id = 2 AND service_name IS NOT NULL AND email IS NULL)
                                            )
);

-- Index for account lookups via type
CREATE INDEX IF NOT EXISTS ix_accounts_type_id ON accounts (account_type_id);

-- Roles
CREATE TABLE IF NOT EXISTS roles (
                                     id          SERIAL PRIMARY KEY,
                                     name        VARCHAR(50) NOT NULL UNIQUE,
                                     created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     created_by  VARCHAR(100),
                                     modified_by VARCHAR(100)
);

-- Account Roles (Many-to-Many)
CREATE TABLE IF NOT EXISTS account_roles (
                                             account_id  INTEGER     NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
                                             role_id     INTEGER     NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
                                             created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             created_by  VARCHAR(100),
                                             modified_by VARCHAR(100),
                                             PRIMARY KEY (account_id, role_id)
);

-- Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id           SERIAL PRIMARY KEY,
                                              account_id   INTEGER     NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
                                              token_value  TEXT        NOT NULL UNIQUE,
                                              user_agent   TEXT,
                                              expires_at   TIMESTAMPTZ NOT NULL,
                                              revoked      BOOLEAN     NOT NULL DEFAULT FALSE,
                                              issued_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                              updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Optimized lookup for /refresh endpoint (Only looks at non-revoked tokens)
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_active_lookup
    ON refresh_tokens (token_value)
    WHERE revoked = FALSE;

-- Speeds up /logout and "Revoke all sessions" for a specific user
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_account_id
    ON refresh_tokens (account_id);

-- Speeds up background cleanup tasks (Deleting old/revoked tokens)
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_cleanup
    ON refresh_tokens (expires_at, revoked);

-- --- SEED DATA ---
INSERT INTO account_types (id, name, created_by)
VALUES (1, 'USER', 'SYSTEM'), (2, 'SERVICE', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

INSERT INTO roles (id, name, created_by)
VALUES (1, 'READ', 'SYSTEM'), (2, 'WRITE', 'SYSTEM'), (3, 'ADMIN', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

COMMIT;