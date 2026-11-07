package org.example;

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

    // API key, sent as a header on every request (not yet required by the backend)
    public static final String HEADER_API_KEY = env("TEST_API_KEY_HEADER", "X-API-Key");
    public static final String API_KEY        = env("TEST_API_KEY", "056dd18b73f3e8f451169f2e412fb9d5d28cb0ab59d8cc3d");

    // Authentication (JWT access token in body + refresh token in cookie)
    public static final String ENDPOINT_AUTH        = env("TEST_AUTH_ENDPOINT", "/api/auth");            // POST {email, password}
    public static final String API_REFRESH          = env("TEST_AUTH_REFRESH_ENDPOINT", "/api/refresh"); // GET, refresh cookie + old bearer
    public static final String COOKIE_REFRESH_TOKEN = env("TEST_AUTH_REFRESH_COOKIE", "refreshToken");
    public static final String JSON_KEY_OBJECT      = env("TEST_AUTH_TOKEN_JSON_PATH", "object");        // JSON path to the access token
    public static final String HEADER_AUTHORIZATION = env("TEST_AUTH_HEADER", "Authorization");
    public static final String BEARER_PREFIX        = env("TEST_AUTH_BEARER_PREFIX", "Bearer ");
    /** Refresh proactively when the access token has less than this many seconds left. */
    public static final long REFRESH_MARGIN_SECONDS = Long.parseLong(env("TEST_AUTH_REFRESH_MARGIN_SECONDS", "30"));

    // Test accounts
    public static final String USER_EMAIL       = env("TEST_USER_EMAIL", "user@example.org");
    public static final String ADMIN_EMAIL      = env("TEST_ADMIN_EMAIL", "admin@example.org");
    public static final String SUPERADMIN_EMAIL = env("TEST_SUPERADMIN_EMAIL", "superadmin@example.org");
    public static final String DEFAULT_PASSWORD = env("TEST_PASSWORD", "password");

    /**
     * @return the value of environment variable {@code name}, or {@code defaultValue} if it is not set
     */
    public static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
