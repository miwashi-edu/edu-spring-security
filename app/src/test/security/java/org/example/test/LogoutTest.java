package org.example.test;

import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Logout ({@code POST /api/auth/logout}).
 */
@RunOnProfiles({"dev", "stage"})
public class LogoutTest extends SecurityTest {

    private static String freshRefreshToken() {
        return refreshToken(login(Role.USER).then().statusCode(200).extract().response());
    }

    /** REQ-031 */
    @Test
    @Tag("REQ-031")
    void shouldSucceed() {
        assertThat(logout(freshRefreshToken()).statusCode(), oneOf(200, 204));
    }

    /** REQ-031: the refresh token is revoked on the server, not just forgotten by the browser. */
    @Test
    @Tag("REQ-031")
    void shouldRevokeRefreshToken() {
        String refresh = freshRefreshToken();

        logout(refresh);

        assertEquals(401, refresh(refresh).statusCode());
    }

    /** REQ-031 */
    @Test
    @Tag("REQ-031")
    void shouldClearRefreshCookie() {
        Response response = logout(freshRefreshToken());

        SetCookie cookie = SetCookie.of(response, Constants.COOKIE_REFRESH_TOKEN)
                .orElseThrow(() -> new AssertionError("logout must overwrite the refresh cookie"));
        assertTrue(cookie.cleared(), "Max-Age=0 or empty value");
    }
}
