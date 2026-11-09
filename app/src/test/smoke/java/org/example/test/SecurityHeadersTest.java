package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Security headers on every response (checked on {@code /actuator/health}, which always exists).
 *
 * <p>Requirements: REQ-012 (see {@code docs/requirements}). Not run in prod (REQ-007).
 */
@RunOnProfiles({"dev", "stage"})
public class SecurityHeadersTest extends SmokeTest {

    private static final String ANY_PAGE = "/actuator/health";

    /** REQ-012: browsers must not guess content types (MIME sniffing). */
    @Test
    @Tag("REQ-012")
    void shouldPreventContentSniffing() {
        given().get(ANY_PAGE).then().header("X-Content-Type-Options", equalToIgnoringCase("nosniff"));
    }

    /** REQ-012: no rendering inside a frame on another site (clickjacking). */
    @Test
    @Tag("REQ-012")
    void shouldPreventFraming() {
        given().get(ANY_PAGE).then().header("X-Frame-Options", anyOf(equalToIgnoringCase("DENY"), equalToIgnoringCase("SAMEORIGIN")));
    }

    /** REQ-012: no version numbers that tell an attacker which exploits to try. */
    @Test
    @Tag("REQ-012")
    void shouldNotRevealServerVersion() {
        given().get(ANY_PAGE).then()
                .header("Server", anyOf(nullValue(), not(matchesPattern(".*\\d+\\.\\d+.*"))))
                .header("X-Powered-By", nullValue());
    }

    /** REQ-012: stage runs on HTTPS; browsers must never fall back to HTTP. */
    @Test
    @Tag("REQ-012")
    @RunOnProfiles("stage")
    void shouldEnforceHTTPSInStage() {
        given().get(ANY_PAGE).then().header("Strict-Transport-Security", containsStringIgnoringCase("max-age"));
    }
}
