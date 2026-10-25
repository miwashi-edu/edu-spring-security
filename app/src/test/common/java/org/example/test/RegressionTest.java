package org.example.test;

import org.junit.jupiter.api.Tag;

/**
 * Tagging interface for regression test suites.
 * Implementing this interface automatically tags the test class with @Tag("regression").
 */
@Tag("regression")
public interface RegressionTest {
}