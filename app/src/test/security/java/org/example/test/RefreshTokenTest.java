package org.example.test;

import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Refresh token cookie and {@code POST /api/auth/refresh}.
 *
 * <p>Every test starts its own login, so rotation and revocation never affect other tests.
 */
@RunOnProfiles({"dev", "stage"})
public class RefreshTokenTest extends SecurityTest {

    private static Response freshLogin() {
        return login(Role.USER).then().statusCode(200).extract().response();
    }

    private static SetCookie refreshCookie(Response response) {
        return SetCookie.of(response, Constants.COOKIE_REFRESH_TOKEN)
                .orElseThrow(() -> new AssertionError("no " + Constants.COOKIE_REFRESH_TOKEN + " cookie"));
    }

    /** REQ-026: JavaScript (and so XSS) cannot read it. */
    @Test
    @Tag("REQ-026")
    void shouldBeHttpOnly() {
        assertTrue(refreshCookie(freshLogin()).httpOnly(), "HttpOnly");
    }

    /** REQ-026: never sent on cross-site requests. */
    @Test
    @Tag("REQ-026")
    void shouldBeSameSiteStrict() {
        assertThat(refreshCookie(freshLogin()).sameSite(), equalToIgnoringCase("Strict"));
    }

    /** REQ-026: only sent to the auth endpoints, not with every API call. */
    @Test
    @Tag("REQ-026")
    void shouldBeLimitedToAuthPath() {
        assertEquals(Constants.COOKIE_REFRESH_PATH, refreshCookie(freshLogin()).path());
    }

    /** REQ-026: stage runs behind HTTPS, so the cookie must never travel over plain HTTP. */
    @Test
    @Tag("REQ-026")
    @RunOnProfiles("stage")
    void shouldBeSecureInStage() {
        assertTrue(refreshCookie(freshLogin()).secure(), "Secure");
    }

    /** REQ-026: only in the cookie, never readable from the response body. */
    @Test
    @Tag("REQ-026")
    void shouldNotBeInResponseBody() {
        Response login = freshLogin();

        assertThat(login.asString(), not(containsString(refreshToken(login))));
    }

    /** REQ-027 */
    @Test
    @Tag("REQ-027")
    void shouldIssueNewAccessToken() {
        Response login = freshLogin();

        Response refreshed = refresh(refreshToken(login));

        assertEquals(200, refreshed.statusCode());
        assertThat(accessToken(refreshed), notNullValue());
        assertEquals(Role.USER.email(), TestJwt.payload(accessToken(refreshed)).getString("sub"));
    }

    /** REQ-027: every refresh hands out a new refresh token (rotation). */
    @Test
    @Tag("REQ-027")
    void shouldRotateRefreshToken() {
        String first = refreshToken(freshLogin());

        String second = refreshToken(refresh(first));

        assertThat("rotated refresh token", second, notNullValue());
        assertNotEquals(first, second);
    }

    /** REQ-028: a refresh token can be used once. */
    @Test
    @Tag("REQ-028")
    void shouldRejectReusedRefreshToken() {
        String first = refreshToken(freshLogin());
        assertEquals(200, refresh(first).statusCode());

        assertEquals(401, refresh(first).statusCode(), "second use of the same refresh token");
    }

    /** REQ-028: reuse means the token was stolen, so the whole session (token family) is revoked. */
    @Test
    @Tag("REQ-028")
    void shouldRevokeSessionOnReuse() {
        String first = refreshToken(freshLogin());
        String second = refreshToken(refresh(first));

        refresh(first); // attacker replays the old token

        assertEquals(401, refresh(second).statusCode(), "legitimate token must be revoked after reuse");
    }

    /** REQ-029 */
    @Test
    @Tag("REQ-029")
    void shouldRejectMissingRefreshToken() {
        Response response = Csrf.fetch().apply(given())
                .post(Constants.API_REFRESH);

        assertEquals(401, response.statusCode());
    }

    /** REQ-029 */
    @Test
    @Tag("REQ-029")
    void shouldRejectUnknownRefreshToken() {
        assertEquals(401, refresh("not-a-refresh-token").statusCode());
    }

    /** REQ-029: an access token is not a refresh token. */
    @Test
    @Tag("REQ-029")
    void shouldRejectAccessTokenAsRefreshToken() {
        assertEquals(401, refresh(accessToken(freshLogin())).statusCode());
    }
}
