package org.example.test;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Password policy on {@code POST /api/auth/password} (NIST SP 800-63B style: length and a
 * deny-list instead of composition rules).
 *
 * <p>Only rejected changes are tested, so the test account's password never changes. If the
 * server wrongly accepts one of these passwords, the account is broken until reset; that is
 * itself a finding.
 */
@RunOnProfiles({"dev", "stage"})
public class PasswordPolicyTest extends SecurityTest {

    private static ValidatableResponse changePassword(String current, String next) {
        return given(user)
                .contentType(ContentType.JSON)
                .body(Map.of("currentPassword", current, "newPassword", next))
        .when()
                .post(Constants.API_PASSWORD)
        .then();
    }

    /** REQ-033 */
    @Test
    @Tag("REQ-033")
    void shouldRejectTooShortPassword() {
        changePassword(Constants.DEFAULT_PASSWORD, "Ab3$".repeat(Constants.PASSWORD_MIN_LENGTH).substring(0, Constants.PASSWORD_MIN_LENGTH - 1))
                .statusCode(400);
    }

    /** REQ-033: a maximum protects the password hashing from denial of service. */
    @Test
    @Tag("REQ-033")
    void shouldRejectTooLongPassword() {
        changePassword(Constants.DEFAULT_PASSWORD, "x".repeat(Constants.PASSWORD_MAX_LENGTH + 1))
                .statusCode(400);
    }

    /** REQ-033: long enough, but on every attacker's word list. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"password1234", "123456789012", "qwertyuiop12", "iloveyou1234"})
    @Tag("REQ-033")
    void shouldRejectCommonPassword(String common) {
        changePassword(Constants.DEFAULT_PASSWORD, common)
                .statusCode(400);
    }

    /** REQ-033 */
    @Test
    @Tag("REQ-033")
    void shouldRejectPasswordContainingEmail() {
        changePassword(Constants.DEFAULT_PASSWORD, Constants.USER_EMAIL + "-2024")
                .statusCode(400);
    }

    /** REQ-033 */
    @Test
    @Tag("REQ-033")
    void shouldRejectUnchangedPassword() {
        changePassword(Constants.DEFAULT_PASSWORD, Constants.DEFAULT_PASSWORD)
                .statusCode(400);
    }

    /** REQ-033: a stolen access token alone must not be enough to take over the account. */
    @Test
    @Tag("REQ-033")
    void shouldRequireCurrentPassword() {
        changePassword("Wrong-Current-Password-1", "A-Completely-New-Passphrase-77")
                .statusCode(403);
    }

    /** REQ-033 */
    @Test
    @Tag("REQ-033")
    void shouldRequireAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("currentPassword", Constants.DEFAULT_PASSWORD, "newPassword", "A-Completely-New-Passphrase-77"))
        .when()
                .post(Constants.API_PASSWORD)
        .then()
                .statusCode(401);
    }
}
