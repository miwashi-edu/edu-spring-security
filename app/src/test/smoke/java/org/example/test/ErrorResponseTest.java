package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Error responses.
 *
 * <p>Requirements: REQ-011 (see {@code docs/requirements}). Not run in prod (REQ-007).
 */
@RunOnProfiles({"dev", "stage"})
public class ErrorResponseTest extends SmokeTest {

    /** REQ-011: no stack traces, exception class names or framework internals in the body. */
    @Test
    @Tag("REQ-011")
    void shouldNotRevealStackTrace() {
        given()
        .when()
                .get("/this-path-does-not-exist")
        .then()
                .body(allOf(
                        not(containsString("\"trace\"")),
                        not(containsString("Exception")),
                        not(containsString("at org.springframework"))));
    }

    /** REQ-011: malformed input is a client error, answered without internals. */
    @Test
    @Tag("REQ-011")
    void shouldNotRevealInternalsOnMalformedRequest() {
        given()
                .contentType("application/json")
                .body("{\"broken\":")
        .when()
                .post(Constants.ENDPOINT_AUTH)
        .then()
                .body(allOf(
                        not(containsString("\"trace\"")),
                        not(containsString("Exception")),
                        not(containsString("at org.springframework"))));
    }
}
