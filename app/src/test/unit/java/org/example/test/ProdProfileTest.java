package org.example.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Server started with profile {@code prod}.
 */
class ProdProfileTest extends ProfileContext {

    @BeforeAll
    static void startWithProdProfile() {
        start("prod");
    }

    @Test
    @Tag("REQ-009")
    void shouldBeActive() {
        assertEquals(Set.of("prod"), Set.of(activeProfiles()));
    }

    /** REQ-010 (and REQ-001): only health may be public in prod; info and all others are closed. */
    @Test
    @Tag("REQ-001")
    @Tag("REQ-010")
    void shouldExposeOnlyHealth() {
        assertEquals(Set.of("health"), exposedEndpoints());
    }
}
