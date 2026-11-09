package org.example.test;

import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Certificate login ({@code POST /api/auth/certificate}).
 *
 * <p>The user has received a certificate by mail. To log in, the client signs a short-lived JWT
 * (the <i>assertion</i>) with the certificate's private key; the server verifies it with the
 * registered certificate and answers like a password login (access token + refresh cookie).
 *
 * <pre>
 * header:  {"alg":"RS256","typ":"JWT"}
 * payload: {"iss":email, "sub":email, "aud":"edu-spring-security", "iat":now, "exp":now+≤300s, "jti":uuid}
 * </pre>
 *
 * Key: {@link Constants#CERT_KEY_FILE} (default: dev-only fixture in {@code security/resources/certificates}).
 */
@RunOnProfiles({"dev", "stage"})
public class CertificateLoginTest extends SecurityTest {

    private static final String EMAIL = Constants.CERT_EMAIL;

    private static Map<String, Object> validClaims() {
        return TestJwt.assertionClaims(EMAIL, 60);
    }

    private static String signed(Map<String, Object> claims) {
        return TestJwt.rs256(claims, TestJwt.certificateKey());
    }

    // ------------------------------------------------------------------ REQ-035

    /** REQ-035 */
    @Test
    @Tag("REQ-035")
    void shouldLogInWithSignedAssertion() {
        certificateLogin(signed(validClaims()))
        .then()
                .statusCode(200)
                .body(Constants.JSON_KEY_OBJECT, notNullValue())
                .cookie(Constants.COOKIE_REFRESH_TOKEN, notNullValue());
    }

    /** REQ-035: the access token identifies the certificate's owner. */
    @Test
    @Tag("REQ-035")
    void shouldIssueTokenForCertificateOwner() {
        Response response = certificateLogin(signed(validClaims()));

        assertEquals(200, response.statusCode());
        assertEquals(EMAIL, TestJwt.payload(accessToken(response)).getString("sub"));
    }

    // ------------------------------------------------------------------ REQ-036

    /** REQ-036: signed by a key that is not registered. */
    @Test
    @Tag("REQ-036")
    void shouldRejectUnknownKey() {
        certificateLogin(TestJwt.rs256(validClaims(), TestJwt.unknownKey()))
                .then().statusCode(401);
    }

    /** REQ-036: a valid signature for one user cannot log in as another. */
    @Test
    @Tag("REQ-036")
    void shouldRejectImpersonation() {
        Map<String, Object> claims = validClaims();
        claims.put("iss", Constants.ADMIN_EMAIL);
        claims.put("sub", Constants.ADMIN_EMAIL);

        certificateLogin(signed(claims))
                .then().statusCode(401);
    }

    /** REQ-036 */
    @Test
    @Tag("REQ-036")
    void shouldRejectUnsignedAssertion() {
        certificateLogin(TestJwt.withAlgNone(signed(validClaims())))
                .then().statusCode(401);
    }

    /** REQ-036: algorithm confusion, HS256 "signed" with the (public!) certificate key as secret. */
    @Test
    @Tag("REQ-036")
    void shouldRejectAlgorithmConfusion() {
        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(TestJwt.certificatePublicKey().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        certificateLogin(TestJwt.hs256(validClaims(), publicPem.getBytes(StandardCharsets.US_ASCII)))
                .then().statusCode(401);
    }

    /** REQ-036 */
    @Test
    @Tag("REQ-036")
    void shouldRejectTamperedAssertion() {
        certificateLogin(TestJwt.tampered(signed(validClaims()), "sub", Constants.SUPERADMIN_EMAIL))
                .then().statusCode(401);
    }

    /** REQ-036 */
    @Test
    @Tag("REQ-036")
    void shouldRejectExpiredAssertion() {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = validClaims();
        claims.put("iat", now - 600);
        claims.put("exp", now - 300);

        certificateLogin(signed(claims))
                .then().statusCode(401);
    }

    /** REQ-036: assertions must be short-lived, a stolen one is useless soon. */
    @Test
    @Tag("REQ-036")
    void shouldRejectLongLivedAssertion() {
        certificateLogin(signed(TestJwt.assertionClaims(EMAIL, Constants.CERT_MAX_ASSERTION_SECONDS + 3600)))
                .then().statusCode(401);
    }

    /** REQ-036: an assertion made for another service must not work here. */
    @Test
    @Tag("REQ-036")
    void shouldRejectWrongAudience() {
        Map<String, Object> claims = validClaims();
        claims.put("aud", "some-other-service");

        certificateLogin(signed(claims))
                .then().statusCode(401);
    }

    /** REQ-036: without {@code jti} replay cannot be detected. */
    @Test
    @Tag("REQ-036")
    void shouldRejectAssertionWithoutId() {
        Map<String, Object> claims = validClaims();
        claims.remove("jti");

        certificateLogin(signed(claims))
                .then().statusCode(401);
    }

    /** REQ-036: each assertion ({@code jti}) can be used once. */
    @Test
    @Tag("REQ-036")
    void shouldRejectReplayedAssertion() {
        String assertion = signed(validClaims());
        assertEquals(200, certificateLogin(assertion).statusCode(), "first use");

        assertEquals(401, certificateLogin(assertion).statusCode(), "replay");
    }
}
