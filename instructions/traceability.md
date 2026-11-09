# edu-spring-security

## Task: Traceability

> Every requirement must be traceable to the tests that verify it, and every test to the requirement it
> verifies. A test without a requirement is noise; a requirement without a test is a gap. This task
> is about the *test process*, not about writing more tests.

The requirements live in [`docs/requirements`](../docs/requirements/README.md), one file per `REQ-nnn`.

### Hints

- Tag each test with the requirements it verifies; JUnit tags are repeatable:

  ```java
  @Test
  @Tag("REQ-004")
  @Tag("REQ-005")
  void shouldExposeOnlyHealthAndInfo() { ... }
  ```

- Tags are filters. A tagged suite runs everything for one requirement, from every suite:

  ```groovy
  testSuites {
      tagged 'req004', 'REQ-004'   // → ./gradlew req004Test
  }
  ```

- Test names are read as sentences (`RequirementSentenceGenerator`): the class names the subject, the
  method says what it should do. `InfoEndpointTest.shouldReportActiveProfile` → *"Info endpoint should
  report active profile"* — the same words as the requirement's title.
- Some requirements are rules for *how* tests run (REQ-007, REQ-038: not in production). They have no test
  of their own; they are enforced by `@RunOnProfiles` and verified by running the suite with `TEST_PROFILE=prod`.
- `python3 docs/requirements/generate.py` regenerates the test matrix from the `@Tag`s in the code.

## Requirements

### TRACE-01: Fill in the test matrix

> Open [`docs/requirements/TEST-MATRIX.md`](../docs/requirements/TEST-MATRIX.md) and fill in the *Tested in*
> column for every requirement: suite and test class, `manual`, or empty if it is not tested.

### TRACE-02: Every test verifies a requirement

> Every `@Test` and `@ParameterizedTest` method in every suite carries at least one `@Tag("REQ-nnn")`.

### TRACE-03: Every requirement is tested where it is suggested

> Compare the *Suggested test* column with where the requirement is actually tested. For each mismatch,
> either move/add the test or write down why the suggestion is wrong.

### TRACE-04: Skips are explained

> Run every suite with `TEST_PROFILE=prod` and no server. Every skipped test reports a reason a person
> can act on (which profile, which variable to set, which server was unreachable).

## Definition of Done

- `TEST-MATRIX.md` has no empty *Tested in* cell, or each empty cell is listed as a known gap.
- `generate.py` reports *Every requirement is covered*.
- No test method without a `REQ-nnn` tag.
- `gradle check` output names every skipped test and why.

## Other test aspects worth a task of their own

| Aspect | Question it answers | Where it fits |
|---|---|---|
| Contract (OpenAPI) | Does the API match `/v3/api-docs`? Did a change break clients? | black-box, validate responses against the spec |
| Performance / load | How does login behave with 100 parallel users? | separate tool (Gatling, k6), not in `check` |
| Mutation testing | Would the unit tests notice if the code were wrong? | PIT plugin on `test` |
| Coverage | Which lines no test reaches? | JaCoCo on `test` + `configTest`; a hint, not a goal |
| Dependency scanning | Does a library have a known vulnerability? | OWASP Dependency-Check / `gradle dependencyUpdates` in CI |
| Flakiness | Does a test fail at random? | re-run failing tests once in CI, then fix or quarantine, never ignore |
| Logging | Are passwords or tokens ever written to the log? | config test with an in-memory appender |
