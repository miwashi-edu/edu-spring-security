package org.example.test;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;

/**
 * Quick checks that the security layer is in place: protected API, login endpoint and CORS.
 * The full behaviour is verified by the {@code security} suite.
 *
 * <p>Requirements: REQ-013, REQ-014, REQ-015 (see {@code docs/requirements}). Not run in prod (REQ-007).
 */
@RunOnProfiles({"dev", "stage"})
public class ApiProtectionTest extends SmokeTest {

    /** REQ-013 */
    @Test
    @Tag("REQ-013")
    void shouldRejectAnonymousRequests() {
        given()
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401);
    }

    /** REQ-014: the login endpoint exists and answers bad credentials with 401 (not 404 or 500). */
    @Test
    @Tag("REQ-014")
    void shouldAnswerLoginAttempts() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "smoke-" + System.nanoTime() + "@example.org", "password", "Not-The-Password-1"))
        .when()
                .post(Constants.ENDPOINT_AUTH)
        .then()
                .statusCode(401);
    }

    /** REQ-015: a CORS preflight from a foreign site must not be allowed. */
    @Test
    @Tag("REQ-015")
    void shouldNotAllowUnknownOrigin() {
        String evil = "https://evil.example.com";

        given()
                .header("Origin", evil)
                .header("Access-Control-Request-Method", "POST")
        .when()
                .options(Constants.ENDPOINT_AUTH)
        .then()
                .header("Access-Control-Allow-Origin", anyOf(nullValue(), not(equalTo(evil))))
                .header("Access-Control-Allow-Origin", anyOf(nullValue(), not(equalTo("*"))));
    }
}
