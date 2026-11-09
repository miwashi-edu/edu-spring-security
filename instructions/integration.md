# edu-spring-security

## Task: Integration Test

> Write black-box tests that verify **features work end to end** over HTTP: logging in, reaching the
> areas a role is allowed to, refreshing tokens and logging out, with the real security filter chain
> and the real token storage behind them. Where smoke asks "is it up?", integration asks
> "does it do what the user needs?".

Note: integration tests use the test accounts `user`, `admin` and `superadmin`. They must exist in
`dev` and `stage`, and must **not** exist in `prod` (REQ-037). Create them in profile-specific
configuration or a profile-specific data loader, never in code that also runs in production.

### Hints

- This is a blackbox suite (`blackbox 'integration'`, already declared). It runs against an already
  running server and cannot see `main`.
- `IntegrationTest` is an abstract superclass. Before the first test it logs in all test accounts; if
  the server is down or a login fails, the **whole class is skipped**, not failed — an integration test
  is not the place to find out that login is broken (that is the [security](security.md) suite).
- Start a request as a user instead of building headers by hand:

  ```java
  given()             // anonymous
  given(user)         // ROLE_USER
  given(admin)        // ROLE_ADMIN
  given(superAdmin)   // ROLE_SUPERADMIN
  ```

  `AuthenticatedUser` keeps a JWT access token and a refresh-token cookie per account for the whole
  JVM run, adds the current token to every request, and refreshes it before it expires or after a `401`.
- Tests that end a session (logout) must use their **own** `AuthenticatedUser` instance, so the shared
  `user`, `admin` and `superAdmin` stay logged in for the other tests.
- `AuthenticatedUser` handles login and refresh internally, but has no public `refresh()` or `logout()`
  yet. Add them (`logout()` posts to `Constants.API_LOGOUT` with the refresh cookie and a CSRF token,
  see `Csrf`) as part of this task; INT-03 and INT-04 need them.
- Run with `TEST_PROFILE` set; integration tests are `@RunOnProfiles({"dev", "stage"})`.

## Requirements

### INT-01: Users log in and are recognised

> After login, `GET /api/me` returns the email address and roles of the logged-in account (REQ-016, REQ-025).

```java
@RunOnProfiles({"dev", "stage"})
class CurrentUserTest extends IntegrationTest {

    @Test
    @Tag("REQ-016")
    @Tag("REQ-025")
    void shouldRecogniseLoggedInUser() {
        given(user)
        .when()
            .get("/api/me")
        .then()
            .statusCode(200)
            .body("email", equalTo(Constants.USER_EMAIL))
            .body("roles", hasItem("USER"));
    }
}
```

### INT-02: Roles reach exactly their areas

> Every role reaches its own area and the areas below it, and nothing above (REQ-032).

| | `/api/user` | `/api/admin` | `/api/superadmin` |
|---|:-:|:-:|:-:|
| user | 200 | 403 | 403 |
| admin | 200 | 200 | 403 |
| superadmin | 200 | 200 | 200 |

```java
@ParameterizedTest(name = "{0} GET {1} → {2}")
@Tag("REQ-032")
@CsvSource({
        "USER,       /api/user,       200",
        "USER,       /api/admin,      403",
        "USER,       /api/superadmin, 403",
        "ADMIN,      /api/admin,      200",
        "ADMIN,      /api/superadmin, 403",
        "SUPERADMIN, /api/superadmin, 200",
})
void shouldRestrictAreasByRole(Role role, String path, int status) {
    given(role.spec())
    .when()
        .get(path)
    .then()
        .statusCode(status);
}
```

### INT-03: Expired access tokens are refreshed transparently

> A client holding an expired access token and a valid refresh cookie gets a new access token without
> logging in again (REQ-027).

```java
@Test
@Tag("REQ-027")
void shouldRefreshExpiredAccessToken() {
    String before = userAccount.token();

    userAccount.refresh();

    assertNotEquals(before, userAccount.token());
    given(user).when().get("/api/me").then().statusCode(200);
}
```

### INT-04: Logout ends the session

> After logout, the refresh token no longer works (REQ-031).

```java
@Test
@Tag("REQ-031")
void shouldEndSessionOnLogout() {
    var own = new AuthenticatedUser(Constants.USER_EMAIL, Constants.DEFAULT_PASSWORD);
    own.login();

    own.logout();

    assertThrows(AssertionError.class, own::refresh);
}
```

### INT-05: Suite skips when the server is unavailable

> With no server running (or wrong credentials), every integration test is reported as **skipped**
> with the reason, and the build is not red.

```shell
TEST_BACKEND_URL=http://localhost:9 ./gradlew integrationTest
# → integrationTest: SUCCESS (n tests, 0 passed, 0 failed, n skipped)
```

## Definition of Done

- The tests run with `gradle integrationTest`.
- The tests pass against `bootRunDev` and `bootRunStage`.
- Every test class extends `IntegrationTest` and uses `given(user)` / `given(admin)` / `given(superAdmin)`
  instead of building `Authorization` headers itself.
- The suite is skipped, not failed, when the server is unavailable, when login fails, or when
  `TEST_PROFILE` is `prod`, and reports why.
- Tests can run in any order and any subset; no test logs out a shared account.

## Going further

- Once the API stores data: create → read → update → delete as one user, and verify another user
  cannot read it (ownership is an integration rule, not a unit rule).
- Validate response bodies against a JSON schema with `matchesJsonSchemaInClasspath("schemas/me.json")`
  (`json-schema-validator` is already a dependency); put schemas in `src/test/integration/resources`.
- Measure: add `.time(lessThan(500L))` to the user-facing calls and see which ones are slow in `stage`.
