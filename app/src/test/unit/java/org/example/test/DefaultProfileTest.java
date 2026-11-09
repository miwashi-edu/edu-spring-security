package org.example.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Server started without any profile.
 */
class DefaultProfileTest extends ProfileContext {

    @BeforeAll
    static void startWithoutProfile() {
        start();
    }

    /** REQ-008: Forgetting to set a profile must give the most restrictive configuration. */
    @Test
    @Tag("REQ-008")
    void shouldBeProd() {
        assertArrayEquals(new String[0], activeProfiles(), "no profile should be active");
        assertArrayEquals(new String[]{"prod"}, defaultProfiles());
    }
}
