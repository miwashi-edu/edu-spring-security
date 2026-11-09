# edu-spring-security

## Task: Security Test

> Write black-box tests that verify the server **resists misuse**: forged and expired tokens, missing
> CSRF tokens, foreign origins, guessed passwords, leaking headers and error pages. Where integration
> tests show the happy path works, security tests show the unhappy paths are closed.

Note: security tests send malformed and malicious requests on purpose, and some of them lock accounts.
They run only against `dev` and `stage` (REQ-038), except the test that proves the test accounts do
**not** work in production (REQ-037).

### Hints

- This is a blackbox suite (`blackbox 'security'`, already declared).
- `SecurityTest` is an abstract superclass. Unlike `IntegrationTest`, a failed login does **not** skip
  the tests: login is what is being tested, so it must show up red.
- Use the raw helpers in `SecurityTest` (`login`, `refresh`, `logout`, `certificateLogin`). They do no
  automatic refresh or retry, so the test sees exactly what the server answers.
- `SecurityTest` logs every account in once after each class, which resets the failed-login counters,
  so throttling tests never lock out the other suites.
- Use `unknownEmail()` for negative login tests; it never collides with a real account's counter.
- A good security assertion is usually a negative one: the status code **and** what must *not* be in
  the response (no stack trace, no `Server: Apache-Coyote/1.1`, no cookie without `HttpOnly`).

## Requirements

### SEC-01: Responses carry security headers

> Every response carries browser protection headers and reveals no server version (REQ-012).

```java
@Test
@Tag("REQ-012")
void shouldSendSecurityHeaders() {
    given()
    .when()
        .get("/")
    .then()
        .header("X-Content-Type-Options", "nosniff")
        .header("X-Frame-Options", anyOf(equalTo("DENY"), equalTo("SAMEORIGIN")))
        .header("Server", nullValue());
}
```

### SEC-02: Forged tokens are rejected

> A token with a changed payload, a wrong signature, `alg: none`, or an expired `exp` gets `401` (REQ-024).

```java
@ParameterizedTest(name = "{0}")
@Tag("REQ-024")
@MethodSource("forgedTokens")
void shouldRejectForgedToken(String description, String token) {
    given()
        .header("Authorization", "Bearer " + token)
    .when()
        .get(Constants.API_ME)
    .then()
        .statusCode(401);
}
```

### SEC-03: Cookie-authenticated requests need a CSRF token

> Refresh and logout, which authenticate with a cookie, are refused without a matching CSRF token (REQ-030).

```java
@Test
@Tag("REQ-030")
void shouldRequireCsrfTokenForRefresh() {
    String refreshToken = refreshToken(login(Role.USER));

    given()
        .cookie(Constants.COOKIE_REFRESH_TOKEN, refreshToken)
    .when()
        .post(Constants.API_REFRESH)
    .then()
        .statusCode(403);
}
```

### SEC-04: Login does not reveal whether an account exists

> A wrong password and an unknown account get the same status and the same body (REQ-017).

```java
@Test
@Tag("REQ-017")
void shouldAnswerUnknownAccountLikeWrongPassword() {
    var wrongPassword  = login(Constants.USER_EMAIL, "Wrong-Password-123");
    var unknownAccount = login(unknownEmail(), "Wrong-Password-123");

    assertEquals(wrongPassword.statusCode(), unknownAccount.statusCode());
    assertEquals(wrongPassword.body().asString(), unknownAccount.body().asString());
}
```

### SEC-05: Repeated failed logins are throttled

> After `TEST_AUTH_MAX_FAILED_LOGINS` failures for one account, further attempts get `429` (REQ-034).

### SEC-06: Unknown origins get no CORS access

> A preflight from `https://evil.example.com` gets no `Access-Control-Allow-Origin` header (REQ-015).

### SEC-07: Test accounts do not work in production

> In `prod`, logging in with the test accounts fails (REQ-037). This is the only security test that
> runs against production, and it sends one attempt per account at most.

```java
@RunOnProfiles("prod")
class ProductionAccountsTest extends SecurityTest { ... }
```

## Definition of Done

- The tests run with `gradle securityTest`.
- The tests pass against `bootRunDev` and `bootRunStage`.
- Everything except SEC-07 is skipped, with a reason, when `TEST_PROFILE=prod`.
- Running `securityTest` before `integrationTest` leaves no account locked.
- No test depends on a secret that is only valid in production.

## Going further

- OWASP API Security Top 10: which items does the suite cover, and which are gaps? Add them to
  [traceability](traceability.md) as requirements without tests.
- Fuzz the login body (`@MethodSource` with huge strings, unicode, JSON injection): the answer is always
  `400` or `401`, never `500`.
