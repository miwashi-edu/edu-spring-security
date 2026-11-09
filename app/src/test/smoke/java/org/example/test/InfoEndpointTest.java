package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

/**
 * Info endpoint ({@code /actuator/info}).
 *
 * <p>Requirements: REQ-004 (see {@code docs/requirements}).
 * Not run in prod (REQ-007).
 */
@RunOnProfiles({"dev", "stage"})
public class InfoEndpointTest extends SmokeTest {

    /** REQ-004: Info endpoint should report active profile. */
    @Test
    @Tag("REQ-004")
    void shouldReportActiveProfile() {
        given()
        .when()
                .get("/actuator/info")
        .then()
                .statusCode(200)
                .body("profile", anyOf(equalTo("dev"), equalTo("stage")));
    }
}
