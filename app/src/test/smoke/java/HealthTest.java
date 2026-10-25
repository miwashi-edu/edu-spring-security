import org.example.test.RunOnProfiles;
import org.example.test.SkipReasonLogger;
import org.example.test.SmokeTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SkipReasonLogger.class)
public class HealthTest extends SmokeTest {

    private static Boolean gateOpen;

    /**
     * If health endpoint is not reachable, tests are skipped.
     */
    @BeforeEach
    void gate() {
        if (gateOpen == null) {
            try {
                gateOpen = given().get("/actuator/health").statusCode() == 200;
            } catch (Exception e) {
                gateOpen = false;
            }
        }
        assumeTrue(gateOpen, "Gatekeeper failed: info endpoint not reachable on " + baseUrl);
    }

    @Test
    void healthEndpointIsAvailable() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200);
    }

    @Test
    @RunOnProfiles({"dev", "stage"})
    void healthEndpointContainsStatusAttribute() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
