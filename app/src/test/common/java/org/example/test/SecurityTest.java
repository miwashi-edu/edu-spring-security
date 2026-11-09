package org.example.test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Base class for black-box security tests against a running server (suite {@code security}).
 *
 * <p>Unlike {@link IntegrationTest}, a failed login does <i>not</i> skip the tests: logging in is
 * what is being tested, so it must show up red.
 *
 * <p>Helpers perform raw calls (no automatic refresh/retry), so tests see exactly what the server
 * answers. Use {@code given(user)} etc. when a test only needs <i>some</i> authenticated user.
 *
 * <p>After each test class, all test accounts log in successfully once, which resets their
 * failed-login counters (REQ-034), so negative tests never lock out the accounts.
 */
@Tag("security")
public abstract class SecurityTest extends BaseHttpTest {

    @AfterAll
    static void resetFailedLoginCounters() {
        if ("prod".equals(Constants.PROFILE)) {
            return; // test accounts must not exist in prod (REQ-037); extra attempts would only trigger throttling
        }
        for (Role role : Role.values()) {
            try {
                login(role.email(), role.password());
            } catch (Exception ignored) {
                // server down or login not implemented: nothing to reset
            }
        }
    }

    // ------------------------------------------------------------------ auth calls

    protected static Response login(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post(Constants.ENDPOINT_AUTH);
    }

    protected static Response login(Role role) {
        return login(role.email(), role.password());
    }

    /** Access token from a login/refresh response. */
    protected static String accessToken(Response response) {
        return response.path(Constants.JSON_KEY_OBJECT);
    }

    /** Refresh token from a login/refresh response ({@code Set-Cookie}). */
    protected static String refreshToken(Response response) {
        return response.getCookie(Constants.COOKIE_REFRESH_TOKEN);
    }

    protected static Response refresh(String refreshToken) {
        return Csrf.fetch().apply(given())
                .cookie(Constants.COOKIE_REFRESH_TOKEN, refreshToken)
                .when()
                .post(Constants.API_REFRESH);
    }

    protected static Response logout(String refreshToken) {
        return Csrf.fetch().apply(given())
                .cookie(Constants.COOKIE_REFRESH_TOKEN, refreshToken)
                .when()
                .post(Constants.API_LOGOUT);
    }

    protected static Response certificateLogin(String assertion) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("assertion", assertion))
                .when()
                .post(Constants.API_CERTIFICATE);
    }

    /** An email address no account has, unique per call (never collides with lockout counters). */
    protected static String unknownEmail() {
        return "nobody-" + UUID.randomUUID() + "@example.org";
    }
}
