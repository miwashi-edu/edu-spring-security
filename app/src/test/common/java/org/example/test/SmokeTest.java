package org.example.test;

/**
 * Base class for smoke test suites that perform high-level health and sanity checks.
 *
 * <p><b>Purpose:</b><br>
 * Smoke testing is a quick, broad check to verify that a deployed application is up, running,
 * and responding on its fundamental endpoints before conducting more exhaustive testing.
 *
 * <p><b>How it works:</b><br>
 * The backend URL is configured by {@link BaseHttpTest} (environment variable {@code TEST_BACKEND_URL},
 * default {@code http://localhost:8080}) and set as RestAssured's base URI, so tests use relative
 * paths. Smoke tests do <i>not</i> require authentication; the {@code user}, {@code admin} and
 * {@code superUser} request specs are still available if needed.
 *
 * <p><b>How to use it effectively:</b>
 * <pre>{@code
 * public class HealthCheckTest extends SmokeTest {
 *     @Test
 *     void testApplicationIsUp() {
 *         given().when().get("/actuator/health").then().statusCode(200);
 *     }
 * }
 * }</pre>
 * Target another environment with {@code TEST_BACKEND_URL=http://smoke-env.example.com:8080}.
 */
public abstract class SmokeTest extends BaseHttpTest {
}
