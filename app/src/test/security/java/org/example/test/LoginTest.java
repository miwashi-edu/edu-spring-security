package org.example.test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Login with email and password ({@code POST /api/auth}).
 */
@RunOnProfiles({"dev", "stage"})
public class LoginTest extends SecurityTest {

    /** REQ-016: user, admin and superadmin get an access token in the body and a refresh token cookie. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @Tag("REQ-016")
    void shouldLogIn(Role role) {
        login(role)
        .then()
                .statusCode(200)
                .body(Constants.JSON_KEY_OBJECT, notNullValue())
                .cookie(Constants.COOKIE_REFRESH_TOKEN, notNullValue());
    }

    /** REQ-017 */
    @Test
    @Tag("REQ-017")
    void shouldRejectWrongPassword() {
        Response response = login(Constants.USER_EMAIL, "Wrong-Password-123");

        assertEquals(401, response.statusCode());
        assertTrue(SetCookie.of(response, Constants.COOKIE_REFRESH_TOKEN).isEmpty(), "no refresh token on failed login");
    }

    /** REQ-017 */
    @Test
    @Tag("REQ-017")
    void shouldRejectUnknownUser() {
        login(unknownEmail(), Constants.DEFAULT_PASSWORD)
        .then()
                .statusCode(401);
    }

    /** REQ-017: an attacker must not learn which accounts exist (no user enumeration). */
    @Test
    @Tag("REQ-017")
    void shouldAnswerWrongPasswordAndUnknownUserAlike() {
        Response wrongPassword = login(Constants.USER_EMAIL, "Wrong-Password-123");
        Response unknownUser = login(unknownEmail(), "Wrong-Password-123");

        assertEquals(wrongPassword.statusCode(), unknownUser.statusCode(), "status");
        assertEquals(wrongPassword.jsonPath().getString("message"), unknownUser.jsonPath().getString("message"), "message");
    }

    /** REQ-018 */
    @Test
    @Tag("REQ-018")
    void shouldRejectMissingPassword() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + Constants.USER_EMAIL + "\"}")
        .when()
                .post(Constants.ENDPOINT_AUTH)
        .then()
                .statusCode(400);
    }

    /** REQ-018 */
    @Test
    @Tag("REQ-018")
    void shouldRejectMalformedJSON() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":")
        .when()
                .post(Constants.ENDPOINT_AUTH)
        .then()
                .statusCode(400)
                .body(not(containsString("Exception")));
    }

    /** REQ-018: credentials must never travel in a URL (logs, history, referrer). */
    @Test
    @Tag("REQ-018")
    void shouldNotAcceptCredentialsInURL() {
        given()
                .queryParam("email", Constants.USER_EMAIL)
                .queryParam("password", Constants.DEFAULT_PASSWORD)
        .when()
                .get(Constants.ENDPOINT_AUTH)
        .then()
                .statusCode(oneOf(401, 404, 405));
    }

    /** REQ-019 */
    @Test
    @Tag("REQ-019")
    void shouldNotBeCached() {
        login(Role.USER)
        .then()
                .statusCode(200)
                .header("Cache-Control", containsString("no-store"));
    }

    /** REQ-020: authentication is token based; no server-side HTTP session. */
    @Test
    @Tag("REQ-020")
    void shouldNotCreateSession() {
        Response response = login(Role.USER);

        assertEquals(200, response.statusCode());
        assertTrue(SetCookie.of(response, "JSESSIONID").isEmpty(), "login must not create a JSESSIONID session");
    }

    /** REQ-016: the access token is returned as JSON. */
    @Test
    @Tag("REQ-016")
    void shouldRespondWithJSON() {
        login(Role.USER)
        .then()
                .statusCode(200)
                .contentType(containsString("json"))
                .body(Constants.JSON_KEY_OBJECT, not(equalTo("")));
    }
}
