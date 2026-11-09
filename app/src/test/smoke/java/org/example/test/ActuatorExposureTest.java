package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;

/**
 * Actuator exposure: only {@code health} and {@code info} are open.
 *
 * <p>Requirements: REQ-005 (see {@code docs/requirements}).
 * Not run in prod (REQ-007).
 */
@RunOnProfiles({"dev", "stage"})
public class ActuatorExposureTest extends SmokeTest {

    /**
     * REQ-005: Actuator exposure should not reveal unused paths.
     * Sensitive actuator endpoints must answer 404, as if they did not exist.
     */
    @ParameterizedTest(name = "Should not reveal {0}")
    @Tag("REQ-005")
    @ValueSource(strings = {
            "/actuator/env",
            "/actuator/beans",
            "/actuator/configprops",
            "/actuator/mappings",
            "/actuator/loggers",
            "/actuator/metrics",
            "/actuator/threaddump",
            "/actuator/heapdump",
    })
    void shouldNotRevealUnusedPaths(String path) {
        given()
                .when()
                .get(path)
                .then()
                .statusCode(404);
    }

    @ParameterizedTest(name = "Should not reveal <{1}>, ({0}) is closed")
    @Tag("REQ-005")
    @CsvSource({
            "/actuator/env,          Environment",
            "/actuator/beans,        Beans",
            "/actuator/configprops,  Config properties",
            "/actuator/mappings,     Request mappings",
            "/actuator/loggers,      Loggers",
            "/actuator/metrics,      Metrics",
            "/actuator/threaddump,   Thread dump",
            "/actuator/heapdump,     Heap dump",
    })
    void shouldStillNotRevealUnusedPaths(String path, String description) {
        given()
                .baseUri("http://localhost:8080")
                .when()
                .get(path)
                .then()
                .statusCode(404);
    }
}