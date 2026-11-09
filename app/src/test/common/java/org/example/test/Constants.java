package org.example.test;

/**
 * Shared settings for HTTP based test suites (smoke, integration, ...).
 *
 * <p>Every setting has a default below and can be overridden by the environment variable
 * named next to it, e.g.
 * <pre>{@code
 * TEST_BACKEND_URL=http://staging.example.com:8080 ./gradlew test
 * }</pre>
 * Environment variables are inherited by the test JVM, so no Gradle configuration is needed.
 * In IntelliJ, set them in the run configuration under <i>Environment variables</i>.
 */
public final class Constants {

    private Constants() {
    }

    // Backend
    public static final String BACKEND_URL = env("TEST_BACKEND_URL", "http://localhost:8080");

    /**
     * Environment the tests run against: {@code dev}, {@code stage} or {@code prod}.
     * Declared by the test run (not asked from the server), see {@code @RunOnProfiles} and REQ-007.
     * Set {@code TEST_PROFILE=prod} when running against production.
     */
    public static final String PROFILE = env("TEST_PROFILE", "dev");

    // API key, sent as a header on every request (not yet required by the backend)
    public static final String HEADER_API_KEY = env("TEST_API_KEY_HEADER", "X-API-Key");
    public static final String API_KEY        = env("TEST_API_KEY", "056dd18b73f3e8f451169f2e412fb9d5d28cb0ab59d8cc3d");

    // Authentication (JWT access token in body + refresh token in cookie), see docs/requirements
    public static final String ENDPOINT_AUTH        = env("TEST_AUTH_ENDPOINT", "/api/auth");                 // POST {email, password}
    public static final String API_REFRESH          = env("TEST_AUTH_REFRESH_ENDPOINT", "/api/auth/refresh"); // POST, refresh cookie + CSRF
    public static final String API_LOGOUT           = env("TEST_AUTH_LOGOUT_ENDPOINT", "/api/auth/logout");   // POST, refresh cookie + CSRF
    public static final String API_PASSWORD         = env("TEST_AUTH_PASSWORD_ENDPOINT", "/api/auth/password"); // POST {currentPassword, newPassword}
    public static final String API_CERTIFICATE      = env("TEST_AUTH_CERTIFICATE_ENDPOINT", "/api/auth/certificate"); // POST {assertion}
    public static final String COOKIE_REFRESH_TOKEN = env("TEST_AUTH_REFRESH_COOKIE", "refreshToken");
    public static final String COOKIE_REFRESH_PATH  = env("TEST_AUTH_REFRESH_COOKIE_PATH", "/api/auth");
    public static final String JSON_KEY_OBJECT      = env("TEST_AUTH_TOKEN_JSON_PATH", "object");             // JSON path to the access token
    public static final String HEADER_AUTHORIZATION = env("TEST_AUTH_HEADER", "Authorization");
    public static final String BEARER_PREFIX        = env("TEST_AUTH_BEARER_PREFIX", "Bearer ");
    /** Refresh proactively when the access token has less than this many seconds left. */
    public static final long REFRESH_MARGIN_SECONDS = Long.parseLong(env("TEST_AUTH_REFRESH_MARGIN_SECONDS", "30"));
    /** Longest accepted access token lifetime (exp - iat). */
    public static final long MAX_ACCESS_TOKEN_SECONDS = Long.parseLong(env("TEST_AUTH_MAX_ACCESS_TOKEN_SECONDS", "900"));
    /** Failed logins allowed before throttling (HTTP 429). */
    public static final int MAX_FAILED_LOGINS = Integer.parseInt(env("TEST_AUTH_MAX_FAILED_LOGINS", "5"));

    // CSRF (double submit: cookie value repeated in header)
    public static final String API_CSRF     = env("TEST_CSRF_ENDPOINT", "/api/csrf");
    public static final String COOKIE_CSRF  = env("TEST_CSRF_COOKIE", "XSRF-TOKEN");
    public static final String HEADER_CSRF  = env("TEST_CSRF_HEADER", "X-XSRF-TOKEN");

    // Protected API
    public static final String API_ME              = env("TEST_API_ME", "/api/me");                 // GET {email, roles}
    public static final String API_USER_AREA       = env("TEST_API_USER_AREA", "/api/user");        // USER, ADMIN, SUPERADMIN
    public static final String API_ADMIN_AREA      = env("TEST_API_ADMIN_AREA", "/api/admin");      // ADMIN, SUPERADMIN
    public static final String API_SUPERADMIN_AREA = env("TEST_API_SUPERADMIN_AREA", "/api/superadmin"); // SUPERADMIN

    // Password policy
    public static final int PASSWORD_MIN_LENGTH = Integer.parseInt(env("TEST_PASSWORD_MIN_LENGTH", "12"));
    public static final int PASSWORD_MAX_LENGTH = Integer.parseInt(env("TEST_PASSWORD_MAX_LENGTH", "128"));

    // Certificate login: the user signs a short-lived JWT with the private key of their (mailed) certificate
    /** PEM (PKCS#8) private key; {@code classpath:} or a file path. Default is a dev-only test fixture. */
    public static final String CERT_KEY_FILE  = env("TEST_CERT_KEY_FILE", "classpath:certificates/user.key.pem");
    public static final String CERT_EMAIL     = env("TEST_CERT_EMAIL", "user@example.org");
    public static final String CERT_AUDIENCE  = env("TEST_CERT_AUDIENCE", "edu-spring-security");
    public static final long CERT_MAX_ASSERTION_SECONDS = Long.parseLong(env("TEST_CERT_MAX_ASSERTION_SECONDS", "300"));

    // Test accounts (must exist in dev/stage, must NOT exist in prod, see docs/requirements)
    public static final String USER_EMAIL       = env("TEST_USER_EMAIL", "user@example.org");
    public static final String ADMIN_EMAIL      = env("TEST_ADMIN_EMAIL", "admin@example.org");
    public static final String SUPERADMIN_EMAIL = env("TEST_SUPERADMIN_EMAIL", "superadmin@example.org");
    /** Must itself satisfy the password policy (length, not common). */
    public static final String DEFAULT_PASSWORD = env("TEST_PASSWORD", "Correct-Horse-Battery-42");

    /**
     * @return the value of environment variable {@code name}, or {@code defaultValue} if it is not set
     */
    public static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
