# edu-spring-security

## Task: Regression Test

> A regression test proves that a bug, once fixed, **stays fixed**. Regression is not a folder but a
> label: a regression test lives in the suite that fits it best (unit, config, smoke, ...) and is
> additionally tagged `regression`. One Gradle task runs all of them, across every suite.

### Hints

- Declare a tagged suite in `app/build.gradle`. The plugin registers the task `regressionTest`, which
  collects `@Tag("regression")` tests from **all** suites:

  ```groovy
  testSuites {
      tagged 'regression'
      ...
  }
  ```

- `RegressionTest` (in `src/test/common`) is a marker **interface** annotated with `@Tag("regression")`.
  A class can extend only one base class, but implement any number of interfaces, so it combines with
  every suite: `class ... extends SmokeTest implements RegressionTest`.
- Write the regression test **before** the fix: it must be red on the broken code and green after the fix.
  If it was never red, it does not prove anything.
- Name it after the behaviour, not the ticket (`shouldNotExposeEnvInStage`, not `bug42`); put the
  ticket in a tag: `@Tag("BUG-042")`.

## Requirements

### REG-01: Regression suite exists

> `gradle regressionTest` runs every test tagged `regression`, from every suite, and nothing else.

### REG-02: A fixed configuration bug stays fixed (white-box)

> Bug BUG-001: `application-stage.yaml` used `exposure.include: "*"`, which exposed `env` and
> `heapdump` in stage. The fix is in the configuration; this test keeps it there.

```java
@ActiveProfiles("stage")
class StageExposureRegressionTest extends ConfigTest implements RegressionTest {

    @Test
    @Tag("BUG-001")
    @Tag("REQ-005")
    void shouldNotExposeEnvOrHeapdumpInStage() {
        assertFalse(exposedEndpoints().contains("env"));
        assertFalse(exposedEndpoints().contains("heapdump"));
    }
}
```

### REG-03: A fixed behaviour bug stays fixed (black-box)

> Bug BUG-002: an unknown path under `/api` answered `500` instead of `404`.

```java
@RunOnProfiles({"dev", "stage"})
class UnknownApiPathRegressionTest extends SmokeTest implements RegressionTest {

    @Test
    @Tag("BUG-002")
    @Tag("REQ-006")
    void shouldAnswerUnknownApiPathWithNotFound() {
        given()
        .when()
            .get("/api/this-path-does-not-exist")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(404)));
    }
}
```

## Definition of Done

- The tests run with `gradle regressionTest`.
- The tests pass (start a server first — the task contains black-box tests too).
- Every regression test implements `RegressionTest` and is also run by its own suite's task.
- Every regression test carries the bug id and the requirement it protects.
- Each regression test was red before its fix (show it in the commit history: test first, fix second).
