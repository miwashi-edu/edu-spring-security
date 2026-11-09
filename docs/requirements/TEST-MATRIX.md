# Test matrix

All requirements on one page. For each requirement, write in **Tested in** where QA tests it:
the suite (*Unit*, *Smoke*, *Security*, *Integration*) and the test class, e.g.
`Smoke: HealthEndpointTest`. Write *manual* if it is only checked by hand, and leave it empty if
it is not tested at all; those are the gaps.

**Suggested test** is where the requirement is best tested; the first suggestion is the main one,
a second one complements it. Do the tests QA actually runs match the suggestion?

| Type | Knows the code? | Group | Tests that … |
|---|---|---|---|
| White-box | yes, runs inside the application | Unit | one class or rule in isolation (validator, token service) |
| White-box | yes | Configuration | the application is configured right per profile (exposed endpoints, defaults) |
| Black-box | no, only HTTP against a running server | Smoke | the deployed server is up and basically works; fast, run after every deploy |
| Black-box | no | Integration | features work end to end (login, refresh, logout with real storage) |
| Black-box | no | Security | the server resists misuse and attacks (forged tokens, CSRF, brute force) |
| – | – | Test process | a rule for how tests are run, not a test of its own |

Compare your answers with the generated matrix in [README.md](README.md#test-matrix).

| Code | Caption | Requirement | Suggested test | Tested in |
|---|---|---|---|---|
| [REQ-001](REQ-001.md) | Health endpoint should report UP | The health endpoint responds with status `UP`. | Black-box · Smoke |  |
| [REQ-002](REQ-002.md) | Health endpoint should respond with JSON | The health endpoint returns its status as JSON. | Black-box · Smoke |  |
| [REQ-003](REQ-003.md) | Health endpoint should respond within two seconds | The health endpoint responds within 2 seconds. | Black-box · Smoke |  |
| [REQ-004](REQ-004.md) | Info endpoint should report active profile | The info endpoint is open and reports which profile the server runs in. | Black-box · Smoke<br>White-box · Configuration |  |
| [REQ-005](REQ-005.md) | Actuator exposure should not reveal unused paths | Only the health and info endpoints are open; sensitive endpoints such as `env` are not exposed. | White-box · Configuration<br>Black-box · Security |  |
| [REQ-006](REQ-006.md) | Unknown path should return not found | The server answers unknown paths with 404 instead of an error or a redirect. | Black-box · Smoke |  |
| [REQ-007](REQ-007.md) | Smoke tests should not run in production | The smoke suite must not run against production, except REQ-001 (health endpoint should report `UP`), since the health endpoint may be public even in production (later protected by an API key). | Test process (no test) |  |
| [REQ-008](REQ-008.md) | Server should default to the prod profile | When the server is started without a profile, it runs as `prod`, the most restrictive configuration. | White-box · Configuration |  |
| [REQ-009](REQ-009.md) | Server should run with the profile it is started with | The server supports the profiles `dev`, `stage` and `prod`, and runs with the one it is started with. | White-box · Configuration |  |
| [REQ-010](REQ-010.md) | Production should expose only the health endpoint | In production only the health endpoint is public; info and all other actuator endpoints are closed. | White-box · Configuration |  |
| [REQ-011](REQ-011.md) | Error responses should not reveal internal details | Error responses never contain stack traces, exception class names or framework internals — not even in dev. | Black-box · Security<br>White-box · Configuration |  |
| [REQ-012](REQ-012.md) | Responses should include security headers | Every response carries headers that protect browsers, and reveals no server versions. | Black-box · Security |  |
| [REQ-013](REQ-013.md) | Protected API should reject anonymous requests | The API under `/api` (except login, refresh, logout, certificate login and CSRF) requires authentication. | Black-box · Smoke<br>Black-box · Security |  |
| [REQ-014](REQ-014.md) | Login endpoint should be available | The login endpoint exists and answers bad credentials with 401 — not 404 or 500. | Black-box · Smoke |  |
| [REQ-015](REQ-015.md) | Unknown origins should not get cross-origin access | Browsers on foreign sites are not allowed to call the API (CORS). | Black-box · Security<br>White-box · Configuration |  |
| [REQ-016](REQ-016.md) | Users should log in with email and password | User, admin and superadmin log in with email and password and receive an access token in the JSON body and a refresh token cookie. | Black-box · Integration<br>Black-box · Security |  |
| [REQ-017](REQ-017.md) | Login should not reveal whether an account exists | Wrong password and unknown account get the same answer (no user enumeration). | Black-box · Security |  |
| [REQ-018](REQ-018.md) | Login should reject malformed requests | Missing fields and broken JSON are rejected; credentials are never accepted in a URL. | White-box · Unit<br>Black-box · Security |  |
| [REQ-019](REQ-019.md) | Authentication responses should not be cached | Responses carrying tokens are never stored by browsers or proxies. | Black-box · Security |  |
| [REQ-020](REQ-020.md) | Server should not create sessions | Authentication is token based; the server keeps no HTTP session. | White-box · Configuration<br>Black-box · Security |  |
| [REQ-021](REQ-021.md) | Access token should be a signed JWT identifying user and roles | The access token is a signed JWT whose subject is the user's email and whose `roles` claim holds the user's role. | White-box · Unit<br>Black-box · Security |  |
| [REQ-022](REQ-022.md) | Access token should be short-lived | A stolen access token is only useful for a short time. | White-box · Unit<br>Black-box · Security |  |
| [REQ-023](REQ-023.md) | Access token should not contain secrets | The JWT payload is readable by anyone, so it contains no password and no refresh token. | White-box · Unit<br>Black-box · Security |  |
| [REQ-024](REQ-024.md) | Protected API should accept only valid bearer tokens | Only an unmodified, server-signed access token in `Authorization: Bearer` is accepted. | Black-box · Security<br>White-box · Unit |  |
| [REQ-025](REQ-025.md) | Current user endpoint should return identity and roles | `GET /api/me` tells the client who is logged in. | Black-box · Integration |  |
| [REQ-026](REQ-026.md) | Refresh token should be kept in a protected cookie | The refresh token lives only in a cookie that scripts cannot read, that is not sent cross-site, and only to the auth endpoints. | Black-box · Security |  |
| [REQ-027](REQ-027.md) | Refresh should issue new tokens and rotate the refresh token | `POST /api/auth/refresh` (cookie + CSRF) returns a new access token and replaces the refresh token. | Black-box · Integration<br>Black-box · Security |  |
| [REQ-028](REQ-028.md) | Reused refresh tokens should revoke the session | A refresh token works once. Reuse means it was stolen, so the whole session is revoked. | Black-box · Security<br>White-box · Unit |  |
| [REQ-029](REQ-029.md) | Refresh should require a valid refresh token | Refresh without cookie, with an unknown value, or with an access token fails. | Black-box · Security |  |
| [REQ-030](REQ-030.md) | Cookie-authenticated requests should require a CSRF token | Refresh and logout (authenticated by cookie) require the CSRF token: `XSRF-TOKEN` cookie repeated in the `X-XSRF-TOKEN` header. | Black-box · Security |  |
| [REQ-031](REQ-031.md) | Logout should end the session | `POST /api/auth/logout` (cookie + CSRF) revokes the refresh token on the server and clears the cookie. | Black-box · Integration<br>Black-box · Security |  |
| [REQ-032](REQ-032.md) | Roles should restrict access to their areas | Each area is open to its role and the roles above it: `/api/user` (USER+), `/api/admin` (ADMIN+), `/api/superadmin` (SUPERADMIN). | Black-box · Security<br>White-box · Unit |  |
| [REQ-033](REQ-033.md) | Password changes should follow the password policy | `POST /api/auth/password` requires authentication and the current password, and enforces length 12–128, no common passwords, not containing the email, different from the current one (NIST SP 800-63B). | White-box · Unit<br>Black-box · Integration |  |
| [REQ-034](REQ-034.md) | Repeated failed logins should be throttled | After 5 failed logins for an account, further attempts get `429` with `Retry-After` — for unknown accounts too. Other accounts are not affected; a successful login resets the counter. | Black-box · Security<br>White-box · Unit |  |
| [REQ-035](REQ-035.md) | Users should log in with their certificate | A user who received a certificate by mail logs in by sending a JWT assertion signed with the certificate's private key (RS256; `iss`=`sub`=email, `aud`=`edu-spring-security`, `exp` ≤ 5 min, unique `jti`). | Black-box · Integration |  |
| [REQ-036](REQ-036.md) | Certificate login should reject invalid assertions | Only an unmodified, fresh, single-use assertion for this service, signed by the registered key of the named user, is accepted. | Black-box · Security<br>White-box · Unit |  |
| [REQ-037](REQ-037.md) | Test accounts should not work in production | The well-known test accounts and passwords do not exist in production. | Black-box · Security |  |
| [REQ-038](REQ-038.md) | Security tests should not run in production | The security suite logs in with test accounts and attacks the server, so it only runs in dev and stage — except REQ-037, which only runs in prod. | Test process (no test) |  |
