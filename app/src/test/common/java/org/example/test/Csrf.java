package org.example.test;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * CSRF token for cookie-authenticated requests (double submit: the {@code XSRF-TOKEN} cookie
 * value is repeated in the {@code X-XSRF-TOKEN} header).
 *
 * <pre>{@code
 * Csrf csrf = Csrf.fetch();
 * csrf.apply(given()).cookie(COOKIE_REFRESH_TOKEN, refresh).post(API_REFRESH);
 * }</pre>
 */
public record Csrf(String token) {

    /** Fetches a fresh token from {@link Constants#API_CSRF}. */
    public static Csrf fetch() {
        Response r = given().get(Constants.API_CSRF);
        String token = r.getCookie(Constants.COOKIE_CSRF);
        if (token == null) {
            throw new IllegalStateException("No " + Constants.COOKIE_CSRF + " cookie from " + Constants.API_CSRF
                    + " (HTTP " + r.getStatusCode() + ")");
        }
        return new Csrf(token);
    }

    /** Adds cookie and header to the request. */
    public RequestSpecification apply(RequestSpecification request) {
        return request.cookie(Constants.COOKIE_CSRF, token).header(Constants.HEADER_CSRF, token);
    }
}
