package org.example.test;

import groovy.json.JsonOutput;
import io.restassured.path.json.JsonPath;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal JWT toolkit for tests (JDK only, no JWT library): decode tokens issued by the server,
 * and build signed, unsigned, forged and tampered tokens to attack it.
 */
public final class TestJwt {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private TestJwt() {
    }

    // ------------------------------------------------------------------ decode

    public static JsonPath header(String jwt) {
        return JsonPath.from(new String(B64D.decode(part(jwt, 0)), StandardCharsets.UTF_8));
    }

    public static JsonPath payload(String jwt) {
        return JsonPath.from(new String(B64D.decode(part(jwt, 1)), StandardCharsets.UTF_8));
    }

    public static String rawPayload(String jwt) {
        return new String(B64D.decode(part(jwt, 1)), StandardCharsets.UTF_8);
    }

    private static String part(String jwt, int index) {
        String[] parts = jwt.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Not a JWT (expected 3 parts, got " + parts.length + ")");
        }
        return parts[index];
    }

    // ------------------------------------------------------------------ build

    /** RS256-signed JWT. */
    public static String rs256(Map<String, Object> claims, PrivateKey key) {
        String signingInput = encode(Map.of("alg", "RS256", "typ", "JWT"), claims);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + B64.encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** HS256-signed JWT (used for the algorithm confusion attack: public key as HMAC secret). */
    public static String hs256(Map<String, Object> claims, byte[] secret) {
        String signingInput = encode(Map.of("alg", "HS256", "typ", "JWT"), claims);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return signingInput + "." + B64.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Unsigned JWT ({@code alg: none}) with the claims of {@code jwt}. */
    public static String withAlgNone(String jwt) {
        return B64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + part(jwt, 1) + ".";
    }

    /** {@code jwt} with one claim changed and the original signature kept. */
    public static String tampered(String jwt, String claim, Object value) {
        Map<String, Object> claims = new LinkedHashMap<>(payload(jwt).getMap("$"));
        claims.put(claim, value);
        return part(jwt, 0) + "." + B64.encodeToString(JsonOutput.toJson(claims).getBytes(StandardCharsets.UTF_8))
                + "." + part(jwt, 2);
    }

    private static String encode(Map<String, Object> header, Map<String, Object> claims) {
        return B64.encodeToString(JsonOutput.toJson(header).getBytes(StandardCharsets.UTF_8))
                + "." + B64.encodeToString(JsonOutput.toJson(claims).getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ certificate login

    /** Valid claims for a certificate login assertion for {@code email}, living {@code seconds}. */
    public static Map<String, Object> assertionClaims(String email, long seconds) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", email);
        claims.put("sub", email);
        claims.put("aud", Constants.CERT_AUDIENCE);
        claims.put("iat", now);
        claims.put("exp", now + seconds);
        claims.put("jti", UUID.randomUUID().toString());
        return claims;
    }

    /** Private key of the user's certificate ({@link Constants#CERT_KEY_FILE}). */
    public static PrivateKey certificateKey() {
        String pem = readPem(Constants.CERT_KEY_FILE);
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Not a PKCS#8 RSA key: " + Constants.CERT_KEY_FILE, e);
        }
    }

    /** Public key matching {@link #certificateKey()}. */
    public static PublicKey certificatePublicKey() {
        RSAPrivateCrtKey key = (RSAPrivateCrtKey) certificateKey();
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent()));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A fresh RSA key no server knows. */
    public static PrivateKey unknownKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair().getPrivate();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readPem(String location) {
        try {
            if (location.startsWith("classpath:")) {
                String resource = location.substring("classpath:".length());
                try (InputStream in = TestJwt.class.getClassLoader().getResourceAsStream(resource)) {
                    if (in == null) {
                        throw new IllegalStateException("Classpath resource not found: " + resource);
                    }
                    return new String(in.readAllBytes(), StandardCharsets.US_ASCII);
                }
            }
            return Files.readString(Path.of(location));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + location, e);
        }
    }
}
