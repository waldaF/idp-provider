# idp-provider

A generic JWT identity provider skeleton. Issues RSA-signed JWTs for both browser users (Google OAuth) and backend services (client credentials), and exposes a JWKS endpoint so downstream services can verify tokens independently without calling back to this service on every request.

## Authentication flows

There are two completely separate auth flows depending on the client type.

### `auth.frontend.enabled` — the master switch

This one property decides which of the two flows below are even active:

```yaml
auth:
  frontend:
    enabled: false   # default — backend/service auth only
```

- `false` (default): only the backend-services (M2M) flow runs. The entire frontend stack — `AuthController`, `AccountService`, `RefreshTokenService`, `AuthenticationService`, the Google ID token verifier — is switched off via `@ConditionalOnProperty`, and **the DB/JPA/Flyway autoconfiguration is excluded entirely** (see `FrontendAuthEnvironmentPostProcessor`). In this mode the service needs no Postgres connection at all — it only signs tokens for the `SERVICE` accounts defined in `jwt.services.*`.
- `true`: the frontend flow is also active. This requires a reachable Postgres DB, a configured Google OAuth client, and a `jwt.frontend` key block (see [Enabling frontend auth](#enabling-frontend-browser-auth) below).

### Frontend users (browser)

Users authenticate via Google OAuth. The service validates the Google ID token, looks up or creates the user account, and sets two HttpOnly cookies:

- `access_token` — short-lived (30 min), used on every request
- `refresh_token` — long-lived (10 days), used only to rotate the access token

Tokens are never exposed in the response body. The frontend never touches a JWT directly — it just sends cookies.

```
Browser → POST /api/v1/auth/authenticate  { googleIdToken }
       ← Set-Cookie: access_token=...; refresh_token=...

Browser → POST /api/v1/auth/verify        (cookie sent automatically)
       ← 200 OK / 401 Unauthorized

Browser → POST /api/v1/auth/refresh       (refresh_token cookie)
       ← Set-Cookie: access_token=...; refresh_token=...  (both rotated —
         reusing an old refresh_token is treated as compromise)

Browser → POST /api/v1/auth/logout        (refresh_token cookie)
       ← refresh_token revoked server-side, cookies cleared
```

Roles come from the `account_roles` table, so an admin can promote or demote a user without a deploy.

> **Gotcha**: the `refresh_token` cookie is scoped to path `/api/v1/auth/frontend/refresh`, but the actual endpoint is `/api/v1/auth/refresh`. A gateway/proxy in front of idp-provider is expected to route the `/api/v1/auth/frontend/refresh` path to the real `/api/v1/auth/refresh` endpoint — calling the real endpoint directly without that gateway means the browser won't send the cookie (its path doesn't match).

### Backend services (machine-to-machine)

Services authenticate using a pre-registered service name and client secret. The access token is returned in the response body as a plain string — no cookies involved.

```
Service → POST /api/v1/auth/service/authenticate  { serviceName, clientSecret }
        ← { serviceName, roles, accessToken }

Service → GET  /target-service/some-endpoint
              Authorization: Bearer eyJ...
```

The target service (e.g. example-service) validates the token locally by fetching the public keys from `GET /.well-known/jwks.json`. No round-trip to idp-provider on every request.

Roles come from `jwt.services.<name>.roles` in `application.yaml`, not the DB — a service's authorization level is a deploy-time decision.

There is deliberately no refresh token for service accounts: re-authenticating with the client secret is cheap and non-interactive for a backend service (unlike a browser's Google OAuth login), so there's nothing to amortize. Per RFC 6749 §4.4.3, refresh tokens aren't meant for client_credentials-style M2M auth.

## Authorization model

Roles (`READ`, `WRITE`, `ADMIN`) are embedded as a `roles` claim in every issued JWT. Where those roles come from differs by account type.

### USER accounts — roles in the database

Each user has an individual role stored in the `account_roles` table. An admin can promote or demote a specific user without redeploying anything. The default role on first login is `READ`.

This makes sense for humans: Alice might be `ADMIN`, Bob might be `READ`, and that can change any time based on team decisions.

### SERVICE accounts — roles in configuration

Each backend service has its roles defined in `application.yaml` under `jwt.services.<name>.roles`. All instances of the same service always carry the same role.

```yaml
jwt:
  services:
    backend-service:
      audience:
        - "example-service"   # which downstream service accepts this token
      roles:
        - "READ"               # what it is allowed to do there
      access[0]:
        ...
```

This is intentional: a service's authorization level is an architectural decision. It should go through code review and a deploy, not a silent DB change. The `account_roles` table is not used for SERVICE accounts at all.

## Service account secrets

Client secrets are stored as **Argon2id hashes** in the DB and in Flyway migration scripts. Argon2id is a memory-hard password hashing algorithm — the hash cannot be reversed to recover the original secret. This means:

- The hash in `R__service_accounts.sql` is safe to commit to version control
- Reviewers can see the hash change without knowing the plaintext
- In production, inject the hash via environment variable: `SPRING_FLYWAY_PLACEHOLDERS_<SERVICE>_SECRET_HASH=<hash>`

To generate a hash for a new service secret, run the `Argon2HashGeneratorTest.generateHash()` test in `src/test/java/io/idpprovider/util/`.

## Onboarding a new backend

1. Generate a client secret and hash it (see above)
2. Add an `accounts` insert to `R__service_accounts.sql` with the hashed secret
3. Add the Flyway placeholder to `application.yaml` and `application-test.yaml`
4. Add a `jwt.services.<name>` block to `application.yaml` with `audience`, `roles`, and a fresh RSA key pair
5. Share the plaintext secret with the service team through a secure channel — it is never stored anywhere

This works with `auth.frontend.enabled: false` — a service-only deployment needs nothing from steps 2-3 either, since `SERVICE` accounts live entirely in `application.yaml`, not the `accounts` table. Steps 2-3 only matter once frontend auth (and therefore the DB) is turned on. A full `jwt.services` entry (step 4) looks like this:

```yaml
jwt:
  services:
    my-new-service:                    # service name — also the "serviceName" sent to /api/v1/auth/service/authenticate
      audience:
        - "example-service"            # downstream service(s) this token will be accepted by
      roles:
        - "READ"                       # fixed role for every instance of this service — no DB involved
      client-secret-hash: "$argon2id$v=19$m=24576,t=1,p=1$..."  # from Argon2HashGeneratorTest, never the plaintext
      access[0]:                       # no refresh[] block and no cookie — service tokens are bearer-only
        unit: "HOURS"
        expiration: 1
        key-id: "my-new-service-v1"    # bump to -v2 etc. when rotating keys; JwtProperties binds to the last index
        private-key-pem: "<base64 PKCS8 private key — signs this service's tokens>"
        public-key-pem: "<base64 X509 public key — published at /.well-known/jwks.json>"
```

## Enabling frontend (browser) auth

Frontend auth is off by default (`auth.frontend.enabled: false`). Turning it on brings back the DB/JPA/Flyway layer and the Google OAuth flow described above — it needs four things configured together:

```yaml
auth:
  frontend:
    enabled: true                      # master switch — see "auth.frontend.enabled" above

google:
  configuration:
    client-id: "<id>.apps.googleusercontent.com"  # the Google OAuth client the browser app signs in with
    allowed-domains: "example.com"     # only Google accounts on this domain can authenticate

cors:
  configuration:
    urls: "https://app.example.com"    # browser origin(s) allowed to call this service with credentials

jwt:
  frontend:
    audience:
      - "example-service"              # downstream service(s) the frontend's access token is valid for
    access[0]:
      unit: "MINUTES"
      expiration: 30
      key-id: "frontend-access-v1"
      private-key-pem: "<base64 PKCS8 private key>"
      public-key-pem: "<base64 X509 public key>"
      cookie:
        name: "access_token"
        http-only: true
        secure: true                  # false only ever makes sense over plain HTTP in local dev
        path: "/"
        same-site: "Strict"
    refresh[0]:
      unit: "DAYS"
      expiration: 10
      key-id: "frontend-refresh-v1"
      private-key-pem: "<base64 PKCS8 private key>"
      public-key-pem: "<base64 X509 public key>"
      cookie:
        name: "refresh_token"
        http-only: true
        secure: true
        path: "/api/v1/auth/frontend/refresh"  # see the cookie-path gotcha above — must match what the gateway routes
        same-site: "Strict"
```

You also need a reachable Postgres DB (see "Running locally" below) — with the flag off, there's no `DataSource`/`Flyway` bean at all, so there's nothing to connect to.

## Running locally

**Prerequisites**: Java 25, Maven, Docker

```bash
# Start PostgreSQL
docker compose up -d

# Run the application
mvn spring-boot:run

# Run tests (Testcontainers spins up Postgres automatically)
mvn test
```