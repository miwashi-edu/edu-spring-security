# edu-spring-security

## Instructions

> Every file in this folder is one task. Do them in order: each suite builds on the base classes and
> habits from the one before. Every task has the same layout: **Task**, **Hints**, **Requirements**
> (each with an id and a sample test) and **Definition of Done**.

| # | Task | Box | Source folder | Gradle task | Base class | Needs a running server |
|:-:|---|---|---|---|---|:-:|
| 1 | [Unit](unit.md) | White | `src/test/unit` | `test` | `UnitTest` | no |
| 2 | [Configuration](config.md) | White | `src/test/config` | `configTest` | `ConfigTest` | no, starts its own |
| 3 | [Smoke](smoke.md) | Black | `src/test/smoke` | `smokeTest` | `SmokeTest` | yes |
| 4 | [Integration](integration.md) | Black | `src/test/integration` | `integrationTest` | `IntegrationTest` | yes |
| 5 | [Security](security.md) | Black | `src/test/security` | `securityTest` | `SecurityTest` | yes |
| 6 | [Regression](regression.md) | Tag | all suites | `regressionTest` | `RegressionTest` (interface) | depends on the tagged tests |
| 7 | [Traceability](traceability.md) | – | all suites | – | – | – |

## White-box or black-box

The plugin `net.miwashi.test-suites` decides what a suite is allowed to see, so the classpath enforces
the kind of test, not your discipline.

```text
                 ┌──────────── main (App, application*.yaml) ───────────┐
                 │                                                      │
 whitebox ──►  unit (test) · config                 sees main + common + test dependencies
 blackbox ──►  smoke · integration · security       sees common + test dependencies — NOT main
                 │                                                      │
                 └──────────── common (base classes, helpers) ──────────┘
```

| | White-box | Black-box |
|---|---|---|
| Knows the code | yes, may import `org.example.*` | no, only HTTP |
| Starts the application | unit: never · config: yes, in the test JVM | never, the server is already running |
| Where it runs | on the developer machine and in the build | against `dev`, `stage` (and partly `prod`) |
| Typical speed | milliseconds (unit), seconds (config) | depends on the network and the server |
| A failure means | the code or the configuration is wrong | the deployed system is wrong |

If a black-box test does not compile because it imports `org.example.App`, the plugin is doing its job.

## Running

```bash
./gradlew test                  # unit — fast, run all the time
./gradlew configTest            # configuration, starts Spring per profile
./gradlew bootRunDev            # in another terminal: a server for the black-box suites
./gradlew smokeTest             # black-box against http://localhost:8080
./gradlew integrationTest
./gradlew securityTest
./gradlew regressionTest        # @Tag("regression") from every suite
./gradlew check                 # everything — black-box suites included, so a server must be up
```

Reports: `app/build/reports/tests/<task>/index.html`.

## Settings shared by all HTTP suites

All settings live in `Constants` (`src/test/common`) and are read from environment variables, so the
same test code runs against any environment without recompiling.

| Variable | Default | Used for |
|---|---|---|
| `TEST_BACKEND_URL` | `http://localhost:8080` | base URI of every black-box request |
| `TEST_PROFILE` | `dev` | the environment the run targets (`dev`, `stage`, `prod`), see `@RunOnProfiles` |
| `TEST_USER_EMAIL`, `TEST_ADMIN_EMAIL`, `TEST_SUPERADMIN_EMAIL`, `TEST_PASSWORD` | `*@example.org` | test accounts (exist in dev/stage only) |
| `TEST_API_KEY` | dev key | sent as `X-API-Key` on every request |

```bash
TEST_PROFILE=stage TEST_BACKEND_URL=https://stage.example.com ./gradlew smokeTest
```

## Rules that hold for every suite

1. **Name tests as spoken requirements.** `HealthEndpointTest.shouldReportUP()` is shown as
   "Health endpoint should report UP" (`RequirementSentenceGenerator`). If the sentence is not a
   requirement, rename the test.
2. **Tag the requirement.** Every test carries the id it verifies: `@Tag("REQ-001")`.
   See [traceability](traceability.md).
3. **One reason to fail.** One behaviour per test; use `@ParameterizedTest` for the same rule on many inputs.
4. **Skip, never fail, on a missing precondition.** Wrong environment → `@RunOnProfiles`; server down or
   login impossible → `assumeTrue(...)`. Red means *the system is wrong*, never *the test could not run*.
   Exception: in the security suite a failed login *is* the finding, so it stays red.
5. **No hard-coded hosts, ports, accounts or secrets** in tests. Use the base class and `Constants`.
6. **Tests are independent.** Any order, any subset, repeatable. Tests that change state clean up after
   themselves or use their own data.
7. **Fix configuration in configuration.** If a test fails because of a setting, change the matching
   `application-<profile>.yaml`, never add a workaround for a single run.
