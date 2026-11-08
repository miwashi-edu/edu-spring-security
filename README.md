# edu-spring-security

## Task: Smoke Test

> Write the first smoke test of the system. A smoke test verifies that the system is up and that its basic, non-functional services respond. Spring Actuator's health endpoint is well suited for this.

Note: `application.properties` closes all actuator endpoints. Solve this permanently in the configuration, not with a workaround for a single run.

### Hints

- Use profile-specific configuration: `application-dev.properties`, `application-stage.properties` and `application-prod.properties`.
- This is a blackbox test suite. It runs against an already running server.
- `SmokeTest` is an abstract superclass that holds the shared setup for all smoke tests.

## Requirements

## Requirements

### SMOKE-01: Health endpoint reports UP

> The health endpoint responds with status `UP`.

```java
@Test
void healthEndpointReportsUp() {
    given()
        .baseUri("http://localhost:8080")
    .when()
        .get("/actuator/health")
    .then()
        .statusCode(200)
        .body("status", equalTo("UP"));
}
```

### SMOKE-02: Health endpoint responds with JSON

> The health endpoint returns its status as JSON.

```java
@Test
void healthEndpointRespondsWithJson() {
    given()
        .baseUri("http://localhost:8080")
    .when()
        .get("/actuator/health")
    .then()
        .statusCode(200)
        .contentType(containsString("json"));
}
```

### SMOKE-03: Health endpoint responds quickly

> The health endpoint responds within 2 seconds.

```java
@Test
void healthEndpointRespondsWithinTwoSeconds() {
    given()
        .baseUri("http://localhost:8080")
    .when()
        .get("/actuator/health")
    .then()
        .statusCode(200)
        .time(lessThan(2000L));
}
```

### SMOKE-04: Info endpoint reports active profile

> The info endpoint is open and reports which profile the server runs in.

```java
@Test
void infoEndpointReportsActiveProfile() {
    given()
        .baseUri("http://localhost:8080")
    .when()
        .get("/actuator/info")
    .then()
        .statusCode(200)
        .body("profile", anyOf(equalTo("dev"), equalTo("stage")));
}
```

### SMOKE-05: Sensitive actuator endpoints stay closed

> Only the health and info endpoints are open; sensitive endpoints such as `env` are not exposed.

```java
@Test
void envEndpointIsNotExposed() {
    given()
        .baseUri("http://localhost:8080")
    .when()
        .get("/actuator/env")
    .then()
        .statusCode(404);
}
```

### SMOKE-06: Unknown paths return 404

> The server answers unknown paths with 404 instead of an error or a redirect.

```java
@Test
void unknownPathReturnsNotFound() {
    given()
        .baseUri("http://localhost:8080")
    .when()
        .get("/this-path-does-not-exist")
    .then()
        .statusCode(404);
}
```

## Definition of Done

- The test runs with `gradle smokeTest`.
- The test passes.
- The suite runs only when the responding server uses the `dev` or `stage` profile.
- The suite is skipped, not failed, when the server runs with any other profile, and reports why it was skipped.
