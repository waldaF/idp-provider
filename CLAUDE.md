# CLAUDE.md
This file provides guidance to Claude Code when working with code in this repository.

## Service identity

Generic JWT identity provider skeleton. Serves two distinct client types:

- **Browser users** — authenticated via Google OAuth; tokens delivered as HttpOnly cookies; roles stored per-user in the DB.
- **Backend services** — authenticated via client credentials (service name + secret); tokens returned as bearer tokens in the response body; roles defined in `application.yaml` config, not in the DB.

## Build / run / test

**Prerequisites**: Java 25, Maven, Docker (for local DB and tests).

```bash
# Start local PostgreSQL
docker compose up -d

# Run the application (local profile is active by default)
mvn spring-boot:run

# Build JAR
mvn clean package

# Run all tests (requires Docker — Testcontainers spins up Postgres automatically)
mvn test

# Run a single test class
mvn test -Dtest=JwtPropertiesTest

# Build Docker image
docker build -t idp-provider .
```

The `docker` Maven profile (used in `Dockerfile`) strips all YAML config, expecting config to be injected externally at runtime (mount `/app/config/application.yaml` and/or `/app/config/secret.properties` — see `@PropertySource` on `IdpAuthApplication`). Running the image with no mounted config will fail startup validation (`jwt.issuer`, etc. are required) — that's by design, not a bug.

## Architecture

```
controller/   REST endpoints (AuthController, JwksController)
service/      Business logic (AuthenticationService, JwtService, GoogleTokenVerificationService, etc.)
domain/       JPA entities (Account, Role, RefreshToken, AccountType) + Spring Data repositories
dto/          Request/response records
filter/       JwtAuthenticationFilter — validates access token cookie on every request
properties/   @ConfigurationProperties beans (JwtProperties, CorsProperties, GoogleProperties)
config/       Spring config (SecurityConfig, JwtAuthConfiguration)
error/        Exception hierarchy + RestErrorHandler
util/         CookieProvider
```

## API endpoints

All auth endpoints are unauthenticated. All other requests require a valid `access_token` cookie.

**Frontend (browser users)**

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v1/auth/authenticate` | Exchange Google ID token → set access + refresh JWT cookies |
| `POST` | `/api/v1/auth/verify` | Verify access token cookie |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token, issue new cookies |
| `POST` | `/api/v1/auth/logout` | Revoke refresh token, clear cookies |

**Backend services (machine-to-machine)**

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v1/auth/service/authenticate` | Exchange `{serviceName, clientSecret}` → return bearer access token in body |

**Token verification (used by downstream services)**

| Method | Path | Purpose |
|--------|------|---------|
| `GET`  | `/.well-known/jwks.json` | Public RSA key set (1h cache) — downstream services verify tokens locally using these keys |

## Authorization model

Roles (`READ`, `WRITE`, `ADMIN`) are embedded as a `roles` claim in every issued JWT. The source of truth differs by account type:

**USER accounts (frontend)**
Roles are stored in the `account_roles` DB table. Each person can have a different role, and an admin can promote or demote someone without redeploying the service. Default on registration: `READ`.

**SERVICE accounts (backend-to-backend)**
Roles are defined in `jwt.services.<name>.roles` in `application.yaml`. Every instance of the same service carries the same role — this is intentional, because a service's authorization level is an architectural decision that belongs in code review and a deploy, not a runtime DB change. The `account_roles` table is not used for SERVICE accounts.

When onboarding a new backend service, set both `audience` (which downstream service it will call) and `roles` (what it is allowed to do there) together in the same config block:

```yaml
jwt:
  services:
    my-new-service:
      audience:
        - "target-service"
      roles:
        - "READ"
      access[0]:
        ...
```

## JWT key configuration

Two RSA-2048 key pairs (access, refresh) are configured in `application.yaml` as raw Base64 PKCS8/X509. The config supports multiple versioned key entries (`access[0]`, `access[1]`) — `JwtProperties` binds to the last index. Current active key IDs: `test-access-v1` / `test-refresh-v1` (dev placeholders — rotate before any real deployment).

The keys embedded in `application.yaml` are **development keys** — production keys must be injected via environment variables or an external config source when running with the `docker` profile.

## Database

PostgreSQL (`identity_db`). Flyway manages migrations from `src/main/resources/db/migration/`. Local credentials: `test/test`.

Schema highlights:
- `accounts`: two types — `USER` (email-based, Google auth) and `SERVICE` (service_name-based, client credentials)
- `roles` / `account_roles`: role assignments — **only meaningful for USER accounts**; SERVICE account roles come from `application.yaml`
- `refresh_tokens`: stores JWT JTI for rotation; revoked on use or logout

Service account secrets (`client_secret_hash`) are stored as **Argon2id hashes** (`SecurityConfig.passwordEncoder()`, tuned to 24 MiB memory / 1 iteration / 1 thread — memory-hard but latency-tuned, since these are high-entropy generated secrets rather than guessable human passwords). This means the plaintext secret is never stored anywhere — the hash in `application.yaml` (`jwt.services.<name>.client-secret-hash`) is safe to commit and review. To generate a hash for a new service secret, run the `Argon2HashGeneratorTest.generateHash()` test in `src/test/java/io/idpprovider/util/`.

Test-only seed data lives in `src/test/resources/db/migration/` (e.g. `V2.1__insert_test_user.sql`) — it's picked up automatically because both `src/main/resources/db/migration` and `src/test/resources/db/migration` land on the test classpath under the same `classpath:db/migration` Flyway location.

## Non-obvious gotchas

- **`auth.frontend.enabled`**: off by default. When `false`, the entire frontend stack (`AuthController`, `AccountService`, `RefreshTokenService`, `AuthenticationService`, the Google verifier bean) is disabled via `@ConditionalOnProperty`, and `FrontendAuthEnvironmentPostProcessor` excludes DataSource/JPA/Flyway autoconfiguration entirely — the service runs with no DB and only signs tokens for `SERVICE` accounts defined in `jwt.services.*`. Set it to `true` (and provide a reachable Postgres DB + Google OAuth client) to enable browser/user auth.
- **Refresh cookie path mismatch**: the refresh token cookie is set with `path: /api/v1/auth/frontend/refresh`, but the actual refresh endpoint is at `/api/v1/auth/refresh`. A gateway or proxy layer is expected to route the cookie's path to the real endpoint — calling `/api/v1/auth/refresh` directly without that gateway means the browser won't send the cookie.
- **Tests require Docker**: Testcontainers pulls `postgres:16-alpine` on first run (see `BaseIntegrationTest`) — this is independent of the `postgres:18-alpine` image used by `compose.yaml` for local dev. There is no mock or in-memory DB fallback.
- **Google domain restriction**: `GoogleTokenVerificationService` rejects tokens outside the domain(s) configured in `google.configuration.allowed-domains` (`example.com` in the sample config).
- **No `spring-boot-docker-compose` dependency**: it isn't wired up at all — you must start `compose.yaml` manually (`docker compose up -d`) before running the app locally.
