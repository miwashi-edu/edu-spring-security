package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * Bearer token validation on the protected API ({@code GET /api/me}).
 */
@RunOnProfiles({"dev", "stage"})
public class BearerTokenTest extends SecurityTest {

    private static String validToken() {
        return accessToken(login(Role.USER).then().statusCode(200).extract().response());
    }

    /** REQ-024: 401 with {@code WWW-Authenticate: Bearer}, as RFC 6750 requires. */
    @Test
    @Tag("REQ-013")
    @Tag("REQ-024")
    void shouldRejectMissingToken() {
        given()
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401)
                .header("WWW-Authenticate", containsString("Bearer"));
    }

    /** REQ-024 */
    @Test
    @Tag("REQ-024")
    void shouldRejectMalformedToken() {
        given()
                .header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + "not.a.jwt")
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401);
    }

    /** REQ-024: changing a claim (here: promoting yourself to SUPERADMIN) breaks the signature. */
    @Test
    @Tag("REQ-024")
    void shouldRejectTamperedToken() {
        String forged = TestJwt.tampered(validToken(), "roles", List.of("SUPERADMIN"));

        given()
                .header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + forged)
        .when()
                .get(Constants.API_SUPERADMIN_AREA)
        .then()
                .statusCode(401);
    }

    /** REQ-024: classic JWT attack, an unsigned token with {@code alg: none}. */
    @Test
    @Tag("REQ-024")
    void shouldRejectUnsignedToken() {
        given()
                .header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + TestJwt.withAlgNone(validToken()))
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401);
    }

    /** REQ-024: a token signed by someone else's key. */
    @Test
    @Tag("REQ-024")
    void shouldRejectTokenSignedByUnknownKey() {
        String foreign = TestJwt.rs256(TestJwt.payload(validToken()).getMap("$"), TestJwt.unknownKey());

        given()
                .header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + foreign)
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401);
    }

    /** REQ-024: only the {@code Bearer} scheme is accepted. */
    @Test
    @Tag("REQ-024")
    void shouldRejectTokenWithoutBearerScheme() {
        given()
                .header(Constants.HEADER_AUTHORIZATION, validToken())
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401);
    }

    /** REQ-024: tokens in URLs leak into logs, browser history and referrer headers. */
    @Test
    @Tag("REQ-024")
    void shouldRejectTokenInQueryParameter() {
        given()
                .queryParam("access_token", validToken())
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401);
    }

    /** REQ-024: the refresh token is not an access token. */
    @Test
    @Tag("REQ-024")
    void shouldRejectRefreshTokenAsBearer() {
        String refresh = refreshToken(login(Role.USER).then().statusCode(200).extract().response());

        given()
                .header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + refresh)
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(401);
    }

    /** REQ-025 */
    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @Tag("REQ-025")
    void shouldReturnCurrentUser(Role role) {
        given(role.spec())
        .when()
                .get(Constants.API_ME)
        .then()
                .statusCode(200)
                .body("email", equalTo(role.email()))
                .body("roles", hasItem(role.name()));
    }
}
