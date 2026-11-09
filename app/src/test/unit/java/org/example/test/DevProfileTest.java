package org.example.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Server started with profile {@code dev}.
 */
class DevProfileTest extends ProfileContext {

    @BeforeAll
    static void startWithDevProfile() {
        start("dev");
    }

    @Test
    @Tag("REQ-009")
    void shouldBeActive() {
        assertEquals(Set.of("dev"), Set.of(activeProfiles()));
    }

    /** REQ-001, REQ-004, REQ-005: health and info are open, nothing else. */
    @Test
    @Tag("REQ-001")
    @Tag("REQ-004")
    @Tag("REQ-005")
    void shouldExposeOnlyHealthAndInfo() {
        assertEquals(Set.of("health", "info"), exposedEndpoints());
    }

    /** REQ-004: {@code /actuator/info} contains {@code "profile": "dev"}. */
    @Test
    @Tag("REQ-004")
    void shouldReportProfileInInfo() {
        assertEquals("dev", info().get("profile"));
    }
}
