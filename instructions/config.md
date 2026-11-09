# edu-spring-security

## Task: Configuration Test

> Write white-box tests that verify the application is configured correctly **per profile**: which
> profile is active, which actuator endpoints are exposed, what the info endpoint reports, and how
> errors are rendered. These tests start the real application inside the test JVM using
> **Spring Boot testing** (`@SpringBootTest`), so no server needs to be running.

Configuration is code. A misplaced line in `application-prod.yaml` is a security bug, and a smoke test
only finds it after deployment. A configuration test finds it in the build.

Note: the existing `ProfileContext` and `*ProfileTest` classes in `src/test/unit` start the
application by hand with `SpringApplicationBuilder`. Replace them with Spring Boot's test support in
the new suite, and delete them from `src/test/unit`.

### Hints

- Declare a second white-box suite in `app/build.gradle`. The plugin creates `src/test/config`, the
  source set `configTest`, the task `configTest`, and adds it to `check`:

  ```groovy
  testSuites {
      whitebox('config') {
          dependencies {
              implementation 'org.springframework.boot:spring-boot-starter-test'
          }
      }
      blackbox 'smoke'
      blackbox 'security'
      blackbox 'integration'
  }
  ```

  The plugin also creates the alias `configImplementation`, so the dependency may instead go in the
  normal `dependencies { }` block. Only this suite gets Spring Boot test; unit tests stay plain.

- `ConfigTest` is an abstract superclass that holds the shared Spring setup. It must live in
  `src/test/config/java`, **not** in `src/test/common`: common is shared with the black-box suites and
  therefore cannot see `main` (`App`) or `spring-boot-starter-test`.
- Choose the profile with `@ActiveProfiles("dev")` on the subclass. A subclass without
  `@ActiveProfiles` tests the default profile.
- Spring caches one context per distinct configuration. All `dev` tests share one context, so keep the
  configuration of subclasses minimal (no `@MockitoBean`, `@TestPropertySource` or `@DirtiesContext`
  unless needed — each one starts another context).
- Ask the running context, not the YAML files: `WebEndpointsSupplier` knows the endpoints that are
  really exposed, after `exposure` **and** `access` settings are applied.
- `webEnvironment = RANDOM_PORT` starts Tomcat on a free port, so the suite never clashes with a server
  you have running on 8080. Inject it with `@LocalServerPort` when a test needs real HTTP.

```java
package org.example.test;

@Tag("config")
@SpringBootTest(classes = App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayNameGeneration(RequirementSentenceGenerator.class)
public abstract class ConfigTest {

    @Autowired
    protected Environment environment;

    @Autowired
    private WebEndpointsSupplier webEndpoints;

    @Autowired
    private ObjectProvider<InfoEndpoint> infoEndpoint;

    @LocalServerPort
    protected int port;

    protected Set<String> activeProfiles() {
        return Set.of(environment.getActiveProfiles());
    }

    /** Ids of the actuator endpoints reachable over HTTP, e.g. {@code [health, info]}. */
    protected Set<String> exposedEndpoints() {
        return webEndpoints.getEndpoints().stream()
                .map(endpoint -> endpoint.getEndpointId().toString())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Content of {@code /actuator/info}, empty if the endpoint is not available. */
    protected Map<String, Object> info() {
        InfoEndpoint endpoint = infoEndpoint.getIfAvailable();
        return endpoint == null ? Map.of() : endpoint.info();
    }
}
```

```java
@ActiveProfiles("dev")
class DevProfileTest extends ConfigTest { ... }
```

## Requirements

### CONF-01: Server defaults to the prod profile

> Started without a profile, the server runs as `prod`, the most restrictive configuration (REQ-008).

```java
class DefaultProfileTest extends ConfigTest {

    @Test
    @Tag("REQ-008")
    void shouldDefaultToProd() {
        assertEquals(Set.of(), activeProfiles());
        assertArrayEquals(new String[]{"prod"}, environment.getDefaultProfiles());
    }
}
```

### CONF-02: Server runs with the profile it is started with

> The profiles `dev`, `stage` and `prod` exist, and the server runs with the one it is started with (REQ-009).
> Write one subclass per profile.

```java
@ActiveProfiles("stage")
class StageProfileTest extends ConfigTest {

    @Test
    @Tag("REQ-009")
    void shouldBeActive() {
        assertEquals(Set.of("stage"), activeProfiles());
    }
}
```

### CONF-03: Dev and stage expose only health and info

> In `dev` and `stage`, exactly `health` and `info` are exposed; nothing else (REQ-004, REQ-005).

```java
@Test
@Tag("REQ-004")
@Tag("REQ-005")
void shouldExposeOnlyHealthAndInfo() {
    assertEquals(Set.of("health", "info"), exposedEndpoints());
}
```

### CONF-04: Production exposes only the health endpoint

> In `prod` (and with no profile at all) only `health` is exposed (REQ-010).

```java
@ActiveProfiles("prod")
class ProdProfileTest extends ConfigTest {

    @Test
    @Tag("REQ-010")
    void shouldExposeOnlyHealth() {
        assertEquals(Set.of("health"), exposedEndpoints());
    }
}
```

### CONF-05: Info endpoint reports the active profile

> In `dev` and `stage`, `/actuator/info` contains `"profile": "<active profile>"` (REQ-004).

```java
@Test
@Tag("REQ-004")
void shouldReportProfileInInfo() {
    assertEquals("dev", info().get("profile"));
}
```

### CONF-06: Error responses hide internals in every profile

> No profile includes stack traces, exception names or messages in error responses (REQ-011),
> not even `dev`. Test it once in the base class, so every profile subclass inherits the test.

```java
// in ConfigTest
@Test
@Tag("REQ-011")
void shouldNotRevealErrorDetails() {
    assertEquals("never", environment.getProperty("server.error.include-stacktrace", "never"));
    assertEquals("false", environment.getProperty("server.error.include-exception", "false"));
    assertEquals("never", environment.getProperty("server.error.include-message", "never"));
}
```

### CONF-07: Health endpoint answers over real HTTP

> The configuration is not only resolved, it also works: `GET /actuator/health` on the random port
> answers `UP` in every profile (REQ-001). Also in the base class.

```java
// in ConfigTest
@Test
@Tag("REQ-001")
void shouldReportHealthUP() {
    given()
        .port(port)
    .when()
        .get("/actuator/health")
    .then()
        .statusCode(200)
        .body("status", equalTo("UP"));
}
```

## Definition of Done

- The tests run with `gradle configTest`, and `gradle check` includes them.
- The tests pass without a server running, and also while one runs on port 8080.
- Every profile (`dev`, `stage`, `prod`, none) has its own subclass of `ConfigTest`.
- Shared assertions (CONF-06, CONF-07) are written once, in `ConfigTest`, and run for every profile.
- `src/test/unit` no longer contains `ProfileContext` or `*ProfileTest`; `gradle test` starts no Spring context.
- Unit and black-box suites do **not** get `spring-boot-starter-test` on their classpath.

## Going further

| Spring Boot test feature | Use it to test |
|---|---|
| `@WebMvcTest(SomeController.class)` + `MockMvc` | one controller with its security filter chain, without the rest of the app |
| `@MockitoBean` | replace one bean (e.g. a mail sender) inside the context |
| `@TestPropertySource(properties = "...")` | one setting changed, e.g. a shorter token lifetime |
| `ApplicationContextRunner` | auto-configuration rules in milliseconds, without starting Tomcat |
| `@SpringBootTest(properties = "spring.main.lazy-initialization=true")` | faster contexts when only a few beans matter |
