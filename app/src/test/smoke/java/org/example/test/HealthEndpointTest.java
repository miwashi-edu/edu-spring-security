package org.example.test;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Health endpoint ({@code /actuator/health}).
 *
 * <p>Requirements: REQ-001, REQ-002, REQ-003 (see {@code docs/requirements}).
 * Only REQ-001 runs in prod (REQ-007); the others are restricted to dev and stage.
 */
public class HealthEndpointTest extends SmokeTest {

    private static final String HEALTH = "/actuator/health";

    /** REQ-001: Health endpoint should report UP. Runs in all profiles, including prod. */
    @Test
    @Order(1)
    @Tag("REQ-001")
    void shouldReportUP() {
        given()
        .when()
                .get(HEALTH)
        .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    /** REQ-002: Health endpoint should respond with JSON. */
    @Test
    @Order(2)
    @Tag("REQ-002")
    @RunOnProfiles({"dev", "stage"})
    void shouldRespondWithJSON() {
        given()
        .when()
                .get(HEALTH)
        .then()
                .statusCode(200)
                .contentType(containsString("json"));
    }

    /** REQ-003: Health endpoint should respond within two seconds. */
    @Test
    @Order(3)
    @Tag("REQ-003")
    @RunOnProfiles({"dev", "stage"})
    void shouldRespondWithinTwoSeconds() {
        given()
        .when()
                .get(HEALTH)
        .then()
                .statusCode(200)
                .time(lessThan(2000L));
    }
}
