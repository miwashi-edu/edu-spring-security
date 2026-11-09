package org.example.test;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Common base for all test suites that talk to a running backend over HTTP
 * ({@link SmokeTest}, {@link IntegrationTest}).
 *
 * <p>Configures {@link RestAssured#baseURI} from {@link Constants#BACKEND_URL}
 * (environment variable {@code TEST_BACKEND_URL}, default {@code http://localhost:8080}),
 * so tests only need relative paths. Every request carries the API key header
 * ({@link Constants#HEADER_API_KEY} = {@link Constants#API_KEY}).
 *
 * <p>Provides request specifications for the test accounts in {@link Constants}:
 * <pre>{@code
 * given(user).when().get("/api/profile").then().statusCode(200);
 * given(admin).when().get("/api/admin").then().statusCode(200);
 * given(user).when().get("/api/admin").then().statusCode(403);
 * given().when().get("/api/profile").then().statusCode(401);   // anonymous
 * }</pre>
 * Each user keeps its JWT access token and refresh cookie for the whole JVM run; tokens are
 * injected per request and refreshed automatically on 401 (see {@link AuthenticatedUser}).
 * Test names are shown as spoken requirements ({@link RequirementSentenceGenerator}):
 * {@code ActuatorExposureTest.shouldNotRevealUnusedPaths()} reads
 * "Actuator exposure should not reveal unused paths".
 * Subclasses that require working credentials call {@link #requireAuthentication()} from a
 * {@code @BeforeAll}; all tests in that class are then skipped if login fails.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayNameGeneration(RequirementSentenceGenerator.class)
public abstract class BaseHttpTest {

    /** Base URL of the backend under test. */
    protected static String baseUrl;

    /** Authenticated request specs: use as {@code given(user).when()...then()...}. */
    protected static RequestSpecification user;
    protected static RequestSpecification admin;
    protected static RequestSpecification superAdmin;
    protected static RequestSpecification superUser;

    /** Underlying accounts, e.g. for {@code userAccount.token()} or {@code getEmail()}. */
    protected static AuthenticatedUser userAccount;
    protected static AuthenticatedUser adminAccount;
    protected static AuthenticatedUser superAdminAccount;

    private static boolean initialised;
    private static boolean authOk;
    private static String authError;

    @BeforeAll
    static void setup() {
        if (userAccount != null) {
            return; // already configured in this JVM — keep tokens alive across test classes
        }
        baseUrl = Constants.BACKEND_URL;
        RestAssured.baseURI = baseUrl;
        // API key on every request (plain given(), given(user), login, refresh, retry).
        // A filter with replaceHeader is idempotent, so the header is never duplicated.
        RestAssured.filters((req, res, ctx) -> {
            req.replaceHeader(Constants.HEADER_API_KEY, Constants.API_KEY);
            return ctx.next(req, res);
        });
        RestAssured.config = RestAssured.config()
                //.logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails())
        ;

        userAccount       = new AuthenticatedUser(Constants.USER_EMAIL,       Constants.DEFAULT_PASSWORD);
        adminAccount      = new AuthenticatedUser(Constants.ADMIN_EMAIL,      Constants.DEFAULT_PASSWORD);
        superAdminAccount = new AuthenticatedUser(Constants.SUPERADMIN_EMAIL, Constants.DEFAULT_PASSWORD);

        user       = userAccount.spec();
        admin      = adminAccount.spec();
        superAdmin = superAdminAccount.spec();
        superUser  = superAdmin;

        System.out.println("Backend: " + RestAssured.baseURI);
    }

    /**
     * Logs in all test accounts once per JVM and skips the calling test class if the server
     * is unavailable or any login fails.
     */
    protected static void requireAuthentication() {
        if (!initialised) {
            initialised = true;
            try {
                userAccount.login();
                adminAccount.login();
                superAdminAccount.login();
                authOk = true;
            } catch (Exception | AssertionError e) {
                authOk = false;
                authError = e.toString();
            }
        }
        assumeTrue(authOk, "Server unavailable / authentication failed — skipping all tests: " + authError);
    }
}
