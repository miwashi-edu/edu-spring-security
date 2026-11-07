package org.example.test;

import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for integration tests that communicate with a running application instance.
 *
 * <p><b>Purpose:</b><br>
 * An integration test checks whether different components of an application (such as
 * web controllers, security, services, databases, or external APIs) work together properly
 * in a live or realistic environment.
 *
 * <p><b>How it works:</b><br>
 * The backend URL is configured by {@link BaseHttpTest} (environment variable {@code TEST_BACKEND_URL},
 * default {@code http://localhost:8080}). Before any test runs, all test accounts are logged in;
 * if the server is unavailable or a login fails, every test in the class is skipped.
 *
 * <p><b>How to use it effectively:</b>
 * <pre>{@code
 * public class AdminApiIntegrationTest extends IntegrationTest {
 *     @Test
 *     void adminCanListUsers() {
 *         given(admin).when().get("/api/users").then().statusCode(200);
 *     }
 *
 *     @Test
 *     void userCannotListUsers() {
 *         given(user).when().get("/api/users").then().statusCode(403);
 *     }
 * }
 * }</pre>
 * Target another environment with {@code TEST_BACKEND_URL=http://staging.example.com:8080}.
 */
public abstract class IntegrationTest extends BaseHttpTest {

    @BeforeAll
    static void authenticateAll() {
        requireAuthentication();
    }
}
