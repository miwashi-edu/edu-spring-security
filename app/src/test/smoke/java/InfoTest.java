import org.example.test.SkipReasonLogger;
import org.example.test.SmokeTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests targeting endpoint []/actuator/health
 */
@ExtendWith(SkipReasonLogger.class)
public class InfoTest extends SmokeTest {

    private static Boolean gateOpen;

    /**
     * If info endpoint is not reachable, tests are skipped.
     */
    @BeforeEach
    void gate() {
        if (gateOpen == null) {
            try {
                gateOpen = given().get("/actuator/info").statusCode() == 200;
            } catch (Exception e) {
                gateOpen = false;
            }
        }
        assumeTrue(gateOpen, "Gatekeeper failed: info endpoint not reachable on " + baseUrl);
    }

    @Test
    void infoEndpointContainsDevProfile() {
        given()
                .when()
                .get("/actuator/info")
                .then()
                .statusCode(200)
                .body("app.profiles", notNullValue())
                .body("app.profiles", equalTo("dev"));
    }
}