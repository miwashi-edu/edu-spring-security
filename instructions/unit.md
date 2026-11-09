# edu-spring-security

## Task: Unit Test

> Write the first unit tests of the system. A unit test verifies one class in complete isolation:
> no Spring context, no server, no network, no file system. Collaborators are replaced by mocks.
> Unit tests are the **default white-box suite**: they live in `src/test/unit`, see `main`, and run with
> the standard Gradle task `test`.

The class under test is a password policy (later used by the password change endpoint, REQ-033).
Write the tests first, then the class, until all tests are green.

Note: the plugin needs **no configuration** for this suite. Applying `net.miwashi.test-suites` already
moves the `test` source set to `src/test/unit` and puts `src/test/common` on its classpath.

### Hints

- This is a white-box suite: tests may import `org.example.*` and call classes directly.
- `UnitTest` (in `src/test/common`) is an abstract superclass that tags the test `unit`, enables
  Mockito (`@Mock`, `@InjectMocks`) and turns test names into spoken requirements.
- Do **not** start Spring. No `@SpringBootTest`, no `SpringApplication`, no `@Autowired`.
  If a test needs Spring to run, it belongs in the [configuration](config.md) suite.
- Production code: `app/src/main/java/org/example/security/PasswordPolicy.java` with a collaborator
  interface `CommonPasswords` (`boolean contains(String password)`).
- Test boundaries, not just "typical" values: 11, 12, 128 and 129 characters.
- `ProfileContext` and the `*ProfileTest` classes currently in `src/test/unit` start the whole application.
  They are not unit tests; move them in the [configuration](config.md) task.

```java
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private final CommonPasswords commonPasswords;

    public PasswordPolicy(CommonPasswords commonPasswords) {
        this.commonPasswords = commonPasswords;
    }

    /** @return the rules the password breaks, empty if it is accepted */
    public List<String> violations(String email, String password) { ... }
}
```

## Requirements

### UNIT-01: Policy accepts a strong password

> A password of allowed length, not common and not equal to the email address, has no violations.

```java
class PasswordPolicyTest extends UnitTest {

    @Mock
    private CommonPasswords commonPasswords;

    @InjectMocks
    private PasswordPolicy policy;

    @Test
    @Tag("REQ-033")
    void shouldAcceptStrongPassword() {
        when(commonPasswords.contains(anyString())).thenReturn(false);

        assertTrue(policy.violations("user@example.org", "Correct-Horse-Battery-42").isEmpty());
    }
}
```

### UNIT-02: Policy rejects too short passwords

> Passwords shorter than 12 characters are rejected; exactly 12 is accepted.

```java
@ParameterizedTest(name = "{0} characters")
@Tag("REQ-033")
@CsvSource({"11, false", "12, true"})
void shouldRequireMinimumLength(int length, boolean accepted) {
    assertEquals(accepted, policy.violations("user@example.org", "x".repeat(length)).isEmpty());
}
```

### UNIT-03: Policy rejects too long passwords

> Passwords longer than 128 characters are rejected; exactly 128 is accepted.

```java
@ParameterizedTest(name = "{0} characters")
@Tag("REQ-033")
@CsvSource({"128, true", "129, false"})
void shouldLimitMaximumLength(int length, boolean accepted) {
    assertEquals(accepted, policy.violations("user@example.org", "x".repeat(length)).isEmpty());
}
```

### UNIT-04: Policy rejects common passwords

> A password found in the list of common passwords is rejected. The list is asked exactly once.

```java
@Test
@Tag("REQ-033")
void shouldRejectCommonPassword() {
    when(commonPasswords.contains("password1234")).thenReturn(true);

    assertFalse(policy.violations("user@example.org", "password1234").isEmpty());
    verify(commonPasswords, times(1)).contains("password1234");
}
```

### UNIT-05: Policy rejects the email address as password

> The password must not equal the email address, ignoring case.

```java
@Test
@Tag("REQ-033")
void shouldRejectEmailAsPassword() {
    assertFalse(policy.violations("User@Example.org", "user@example.org").isEmpty());
}
```

### UNIT-06: Policy rejects missing passwords

> `null` and empty passwords are violations, not exceptions.

```java
@ParameterizedTest
@Tag("REQ-033")
@NullAndEmptySource
void shouldRejectMissingPassword(String password) {
    assertFalse(policy.violations("user@example.org", password).isEmpty());
}
```

### UNIT-07: Policy names every broken rule

> A password that breaks several rules reports all of them, so the user can fix them in one go.

```java
@Test
@Tag("REQ-033")
void shouldReportAllViolations() {
    when(commonPasswords.contains("short")).thenReturn(true);

    assertEquals(2, policy.violations("user@example.org", "short").size()); // too short + common
}
```

## Definition of Done

- The tests run with `gradle test` (no extra Gradle configuration).
- The tests pass.
- All test classes extend `UnitTest`, and no test starts Spring or opens a network connection.
- The whole suite runs in a few seconds; a single test class in well under one second.
- The `test` task contains no test that starts the application (`*ProfileTest` moved to `configTest`).
- Every test carries the requirement it verifies (`@Tag("REQ-033")`).

## Further unit test targets (later in the course)

| Class | What to test in isolation |
|---|---|
| Token service | issues a JWT with `sub`, `roles`, `iat`, `exp`; lifetime ≤ 15 minutes; rejects tampered tokens |
| Refresh token store | rotation, reuse detection revokes the family (mock the clock) |
| Login throttle | lock after N failures, unlock after time window (inject a `Clock`, never `Thread.sleep`) |
| `ProfileCondition` (common) | enabled without annotation, disabled with a reason on another profile (mock `ExtensionContext`) |
| `RequirementSentenceGenerator` (common) | `HealthEndpointTest` → "Health endpoint", acronyms such as `JSON` kept |
