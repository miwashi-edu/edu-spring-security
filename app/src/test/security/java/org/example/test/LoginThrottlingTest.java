package org.example.test;

import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Brute force protection on login.
 *
 * <p>Uses a fresh unknown email per test, so the real test accounts are never locked. Unknown
 * accounts must be throttled exactly like existing ones, otherwise throttling reveals which
 * accounts exist (REQ-017).
 */
@RunOnProfiles({"dev", "stage"})
public class LoginThrottlingTest extends SecurityTest {

    private static Response failLoginUntilThrottled(String email) {
        for (int i = 1; i <= Constants.MAX_FAILED_LOGINS; i++) {
            assertEquals(401, login(email, "Wrong-Password-" + i).statusCode(), "attempt " + i);
        }
        return login(email, "Wrong-Password-again");
    }

    /** REQ-034 */
    @Test
    @Tag("REQ-034")
    void shouldThrottleRepeatedFailures() {
        assertEquals(429, failLoginUntilThrottled(unknownEmail()).statusCode());
    }

    /** REQ-034: tells well-behaved clients when to try again. */
    @Test
    @Tag("REQ-034")
    void shouldTellWhenToRetry() {
        assertThat(failLoginUntilThrottled(unknownEmail()).header("Retry-After"), notNullValue());
    }

    /** REQ-034: throttling one account must not lock out everybody else. */
    @Test
    @Tag("REQ-034")
    void shouldThrottlePerAccount() {
        failLoginUntilThrottled(unknownEmail());

        assertEquals(200, login(Role.ADMIN).statusCode());
    }
}
