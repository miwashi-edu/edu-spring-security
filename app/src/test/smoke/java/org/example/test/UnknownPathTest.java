package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Unknown paths.
 *
 * <p>Requirements: REQ-006 (see {@code docs/requirements}).
 * Not run in prod (REQ-007).
 */
@RunOnProfiles({"dev", "stage"})
public class UnknownPathTest extends SmokeTest {

    /** REQ-006: Unknown path should return not found (404), not an error or a redirect. */
    @Test
    @Tag("REQ-006")
    void shouldReturnNotFound() {
        given()
                .redirects().follow(false) // a redirect must not be mistaken for the final answer
        .when()
                .get("/this-path-does-not-exist")
        .then()
                .statusCode(404);
    }
}
