package org.example.test;

import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CSRF protection of cookie-authenticated requests (refresh and logout).
 *
 * <p>Double submit: the token is set as a cookie readable by the page's own JavaScript and must be
 * repeated in the {@code X-XSRF-TOKEN} header. A cross-site attacker can make the browser send the
 * cookies, but cannot read them, so cannot set the header.
 */
@RunOnProfiles({"dev", "stage"})
public class CsrfTest extends SecurityTest {

    private static String freshRefreshToken() {
        return refreshToken(login(Role.USER).then().statusCode(200).extract().response());
    }

    /** REQ-030 */
    @Test
    @Tag("REQ-030")
    void shouldProvideToken() {
        Response response = given().get(Constants.API_CSRF);

        assertEquals(200, response.statusCode());
        assertTrue(SetCookie.of(response, Constants.COOKIE_CSRF).isPresent(), Constants.COOKIE_CSRF + " cookie");
    }

    /** REQ-030: the page must read the cookie to copy it into the header. */
    @Test
    @Tag("REQ-030")
    void shouldBeReadableByScript() {
        SetCookie cookie = SetCookie.of(given().get(Constants.API_CSRF), Constants.COOKIE_CSRF)
                .orElseThrow(() -> new AssertionError("no " + Constants.COOKIE_CSRF + " cookie"));

        assertFalse(cookie.httpOnly(), "CSRF cookie must not be HttpOnly");
    }

    /** REQ-030 */
    @Test
    @Tag("REQ-030")
    void shouldRejectRefreshWithoutToken() {
        given()
                .cookie(Constants.COOKIE_REFRESH_TOKEN, freshRefreshToken())
        .when()
                .post(Constants.API_REFRESH)
        .then()
                .statusCode(403);
    }

    /** REQ-030: header and cookie must match. */
    @Test
    @Tag("REQ-030")
    void shouldRejectRefreshWithWrongToken() {
        Csrf csrf = Csrf.fetch();

        given()
                .cookie(Constants.COOKIE_REFRESH_TOKEN, freshRefreshToken())
                .cookie(Constants.COOKIE_CSRF, csrf.token())
                .header(Constants.HEADER_CSRF, "forged-" + csrf.token())
        .when()
                .post(Constants.API_REFRESH)
        .then()
                .statusCode(403);
    }

    /** REQ-030: an attacker without the cookie cannot invent a token. */
    @Test
    @Tag("REQ-030")
    void shouldRejectRefreshWithHeaderOnly() {
        given()
                .cookie(Constants.COOKIE_REFRESH_TOKEN, freshRefreshToken())
                .header(Constants.HEADER_CSRF, "attacker-chosen")
        .when()
                .post(Constants.API_REFRESH)
        .then()
                .statusCode(403);
    }

    /** REQ-030: logging someone out is also a state change. */
    @Test
    @Tag("REQ-030")
    void shouldRejectLogoutWithoutToken() {
        given()
                .cookie(Constants.COOKIE_REFRESH_TOKEN, freshRefreshToken())
        .when()
                .post(Constants.API_LOGOUT)
        .then()
                .statusCode(403);
    }

    /** REQ-030: the happy path, so the 403s above are not just a broken endpoint. */
    @Test
    @Tag("REQ-030")
    void shouldAcceptRefreshWithToken() {
        assertEquals(200, refresh(freshRefreshToken()).statusCode());
    }
}
