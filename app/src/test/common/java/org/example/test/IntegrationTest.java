package org.example.test;

import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for integration tests that communicate with a running application instance.
 *
 * <p><b>Purpose:</b><br>
 * An integration test checks whether different components of an application (such as
 * web controllers, services, databases, or external APIs) work together properly in a live
 * or realistic environment. This class provides the foundational setup required for such tests,
 * specifically by managing the base web address (URL) of the target application.
 *
 * <p><b>How it works:</b><br>
 * Before each test method runs, this class determines the target URL by checking the
 * Java system property {@code integration.baseUrl}. If that property has not been set,
 * it defaults to {@code http://localhost:8080}. Test methods in child classes can then
 * use the protected {@link #baseUrl} field to send HTTP requests to endpoints.
 *
 * <p><b>How to use it effectively:</b>
 * <ul>
 *   <li>Extend this class when writing tests that make real network or HTTP calls against
 *       a running server:
 *       <pre>{@code
 *       public class UserApiIntegrationTest extends IntegrationTest {
 *           @Test
 *           void testGetUser() {
 *               // baseUrl is automatically available (e.g. "http://localhost:8080")
 *               given().get(baseUrl + "/users/1").then().statusCode(200);
 *           }
 *       }
 *       }</pre>
 *   </li>
 *   <li>To test against a different host or environment (such as a staging server or Docker
 *       container), configure the system property when running the build, for example:
 *       {@code -Dintegration.baseUrl=http://staging.example.com:8080}.</li>
 * </ul>
 */
public abstract class IntegrationTest {
    /**
     * The base URL of the running application against which tests will be executed
     * (e.g., {@code "http://localhost:8080"}).
     */
    protected String baseUrl;

    /**
     * Sets up the target application's base URL before each test execution by reading
     * the {@code integration.baseUrl} system property or falling back to {@code "http://localhost:8080"}.
     */
    @BeforeEach
    void setUpBaseUrl() {
        this.baseUrl = System.getProperty("integration.baseUrl", "http://localhost:8080");
    }
}

