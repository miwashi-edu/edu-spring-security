package org.example.test;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The access token (JWT) issued at login.
 */
@RunOnProfiles({"dev", "stage"})
public class AccessTokenTest extends SecurityTest {

    private static String token(Role role) {
        return accessToken(login(role).then().statusCode(200).extract().response());
    }

    /** REQ-021: three parts, signed with an asymmetric or HMAC algorithm, never {@code none}. */
    @Test
    @Tag("REQ-021")
    void shouldBeSignedJWT() {
        String jwt = token(Role.USER);

        assertEquals(3, jwt.split("\\.").length, "header.payload.signature");
        assertThat(TestJwt.header(jwt).getString("alg"), oneOf("RS256", "ES256", "EdDSA", "HS256"));
        assertThat(TestJwt.header(jwt).getString("alg"), not(equalToIgnoringCase("none")));
    }

    /** REQ-021: subject is the user's email. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @Tag("REQ-021")
    void shouldIdentifyUser(Role role) {
        assertEquals(role.email(), TestJwt.payload(token(role)).getString("sub"));
    }

    /** REQ-021: {@code roles} claim contains the account's role. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @Tag("REQ-021")
    void shouldContainRole(Role role) {
        assertThat(TestJwt.payload(token(role)).getList("roles", String.class), hasItem(role.name()));
    }

    /** REQ-022 */
    @Test
    @Tag("REQ-022")
    void shouldLiveAtMostFifteenMinutes() {
        JsonPath claims = TestJwt.payload(token(Role.USER));

        assertThat("iat", claims.get("iat"), notNullValue());
        assertThat("exp", claims.get("exp"), notNullValue());
        assertThat(claims.getLong("exp") - claims.getLong("iat"), lessThanOrEqualTo(Constants.MAX_ACCESS_TOKEN_SECONDS));
    }

    /** REQ-023: the payload is only base64 encoded, anyone can read it. */
    @Test
    @Tag("REQ-023")
    void shouldNotContainPassword() {
        String payload = TestJwt.rawPayload(token(Role.USER));

        assertThat(payload, not(containsString(Constants.DEFAULT_PASSWORD)));
        assertThat(payload.toLowerCase(), not(containsString("password")));
    }

    /** REQ-023 */
    @Test
    @Tag("REQ-023")
    void shouldNotContainRefreshToken() {
        var login = login(Role.USER).then().statusCode(200).extract().response();

        assertThat(TestJwt.rawPayload(accessToken(login)), not(containsString(refreshToken(login))));
    }
}
