package org.example.test;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/**
 * A test account that keeps a JWT access token and a refresh-token cookie alive for the
 * whole JVM run.
 *
 * <p>{@link #spec()} returns a <i>static</i> request specification, usable as
 * {@code given(user).when()...then()...}. It carries no token itself; instead an attached
 * filter injects the <i>current</i> token on every request. That way the spec stays valid
 * even after the token has been refreshed or rotated.
 *
 * <p>Token lifecycle, handled by the filter:
 * <ol>
 *   <li>No token yet → log in ({@code POST} {@link Constants#ENDPOINT_AUTH}).</li>
 *   <li>Token (JWT {@code exp} claim) expires within {@link Constants#REFRESH_MARGIN_SECONDS} → refresh
 *       proactively before sending, so non-idempotent requests rarely need a retry.</li>
 *   <li>Server still answers 401 → refresh ({@code POST} {@link Constants#API_REFRESH} with the
 *       refresh cookie and a CSRF token); if that fails, log in again. Then retry the request once.</li>
 * </ol>
 * Login/refresh calls use plain {@code given()} (without the filter), so they never recurse.
 */
public class AuthenticatedUser {

    private static final Pattern EXP_CLAIM = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final String email;
    private final String password;
    private final RequestSpecification spec;

    private volatile String jwtToken;
    private volatile String refreshCookie;

    public AuthenticatedUser(String email, String password) {
        this.email = email;
        this.password = password;
        this.spec = new RequestSpecBuilder()
                .addFilter(new AuthRefreshFilter())
                .setContentType(ContentType.JSON)
                .build();
    }

    /** Request specification for {@code given(spec)}; always sends this user's current token. */
    public RequestSpecification spec() {
        return spec;
    }

    public String token() {
        return jwtToken;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Logs in and stores the access token and refresh cookie.
     *
     * @throws IllegalStateException if login is rejected or the response lacks a token/cookie
     */
    public synchronized void login() {
        Response r = given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post(Constants.ENDPOINT_AUTH);

        String token = r.getStatusCode() == 200 ? r.path(Constants.JSON_KEY_OBJECT) : null;
        String cookie = r.getCookie(Constants.COOKIE_REFRESH_TOKEN);
        if (token == null || cookie == null) {
            throw new IllegalStateException("Login failed for " + email + " (HTTP " + r.getStatusCode()
                    + (token == null ? ", no token" : "") + (cookie == null ? ", no refresh cookie" : "") + ")");
        }
        this.jwtToken = token;
        this.refreshCookie = cookie;
    }

    /**
     * Obtains a new access token via the refresh endpoint, falling back to a full re-login.
     *
     * @param rejectedToken the token the server just answered 401 to; if another thread has
     *                      already replaced it, nothing is done
     * @return true if a usable token is available
     */
    synchronized boolean refresh(String rejectedToken) {
        if (jwtToken != null && !jwtToken.equals(rejectedToken)) {
            return true; // already refreshed by a concurrent request
        }

        try {
            // Refresh is cookie-authenticated, so it needs a CSRF token (double submit)
            RequestSpecification req = Csrf.fetch().apply(given());
            if (refreshCookie != null) {
                req.cookie(Constants.COOKIE_REFRESH_TOKEN, refreshCookie);
            }
            Response r = req.when().post(Constants.API_REFRESH);
            String token = r.getStatusCode() == 200 ? r.path(Constants.JSON_KEY_OBJECT) : null;
            if (token != null) {
                this.jwtToken = token;
                String newCookie = r.getCookie(Constants.COOKIE_REFRESH_TOKEN);
                if (newCookie != null) {
                    this.refreshCookie = newCookie; // server rotated the refresh token
                }
                return true;
            }
        } catch (Exception ignored) {
            // fall through to re-login
        }

        // Refresh token is dead — full re-login.
        try {
            login();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Logs in if needed and refreshes proactively if the token is (nearly) expired. */
    private synchronized void ensureValidToken() {
        if (jwtToken == null) {
            login();
        } else if (expiresSoon(jwtToken)) {
            refresh(jwtToken);
        }
    }

    /** @return true if the JWT's {@code exp} claim is within the margin; false if unknown/not a JWT. */
    private static boolean expiresSoon(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher m = EXP_CLAIM.matcher(payload);
            return m.find() && Long.parseLong(m.group(1)) - Instant.now().getEpochSecond() < Constants.REFRESH_MARGIN_SECONDS;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return email;
    }

    /** Injects the current bearer token; on 401 refreshes and retries once. */
    private final class AuthRefreshFilter implements Filter {

        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                               FilterableResponseSpecification responseSpec,
                               FilterContext ctx) {
            ensureValidToken();
            String sentToken = jwtToken;
            requestSpec.replaceHeader(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + sentToken);

            Response response = ctx.next(requestSpec, responseSpec);

            if (response.getStatusCode() == 401 && refresh(sentToken)) {
                response = retry(requestSpec); // single retry
            }
            return response;
        }

        /**
         * Re-sends the already resolved request with the new token. {@code ctx.next()} may only be
         * called once per request and {@code ctx.send()} breaks on path parameters, so the retry is
         * a plain (filter-less) request built from the resolved method, URI, headers, cookies and body.
         * Multipart requests are not replayed.
         */
        private Response retry(FilterableRequestSpecification original) {
            original.replaceHeader(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + jwtToken);
            RequestSpecification req = given()
                    .urlEncodingEnabled(false) // URI is already encoded
                    .headers(original.getHeaders())
                    .cookies(original.getCookies());
            if (original.getContentType() != null) {
                req.contentType(original.getContentType());
            }
            Object body = original.getBody();
            if (body != null) {
                req.body(body);
            }
            if (!original.getFormParams().isEmpty()) {
                req.formParams(original.getFormParams());
            }
            return req.when().request(original.getMethod(), original.getURI());
        }
    }
}
