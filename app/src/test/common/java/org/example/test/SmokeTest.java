package org.example.test;

import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for smoke test suites that perform high-level health and sanity checks.
 *
 * <p><b>Purpose:</b><br>
 * Smoke testing is a quick, broad check to verify that a deployed application is up, running,
 * and responding on its fundamental endpoints before conducting more exhaustive testing.
 * This class establishes the baseline configuration and target web address (URL) needed to
 * run smoke tests against a live server.
 *
 * <p><b>How it works:</b><br>
 * Prior to executing each smoke test method, this class sets up the {@link #baseUrl} field
 * by reading the system property {@code smoke.baseUrl}. If not provided, it defaults to
 * {@code http://localhost:8080}. Subclasses can then use this URL to issue fast sanity check
 * requests (e.g., verifying {@code /actuator/health} or {@code /actuator/info}).
 *
 * <p><b>How to use it effectively:</b>
 * <ul>
 *   <li>Create a smoke test class by extending this base class:
 *       <pre>{@code
 *       public class HealthCheckTest extends SmokeTest {
 *           @Test
 *           void testApplicationIsUp() {
 *               given().get(baseUrl + "/actuator/health")
 *                      .then()
 *                      .statusCode(200);
 *           }
 *       }
 *       }</pre>
 *   </li>
 *   <li>Target different deployment environments (e.g., CI server, staging cluster) by
 *       supplying the system property during execution:
 *       {@code -Dsmoke.baseUrl=http://smoke-env.example.com:8080}.</li>
 * </ul>
 */
public abstract class SmokeTest {
    /**
     * The base URL of the target application for smoke testing (e.g., {@code "http://localhost:8080"}).
     */
    protected String baseUrl;

    /**
     * Sets up the target application's base URL before each test execution by reading
     * the {@code smoke.baseUrl} system property or falling back to {@code "http://localhost:8080"}.
     */
    @BeforeEach
    void setUpBaseUrl() {
        this.baseUrl = System.getProperty("smoke.baseUrl", "http://localhost:8080");
    }
}