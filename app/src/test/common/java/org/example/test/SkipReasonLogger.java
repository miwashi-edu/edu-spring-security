package org.example.test;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * A JUnit 5 test extension that logs skipped and aborted tests to the console output.
 *
 * <p><b>Purpose:</b><br>
 * When running large test suites or automated CI pipelines, tests are often skipped or aborted
 * (for example, due to an unsatisfied assumption, missing prerequisite, or unsupported profile).
 * Without clear logging, it can be difficult to know why a test did not execute. This extension
 * prints clear and informative messages to standard output explaining why a test was bypassed.
 *
 * <p><b>How it works:</b><br>
 * By implementing JUnit 5's {@link TestWatcher} interface, this class receives callback events
 * when test lifecycle milestones occur:
 * <ul>
 *   <li>{@link #testAborted(ExtensionContext, Throwable)}: Triggered when a test aborts early,
 *       such as when an {@code org.junit.jupiter.api.Assumptions.assumeTrue} check fails. It logs
 *       the test name and the assumption failure message.</li>
 *   <li>{@link #testDisabled(ExtensionContext, Optional)}: Triggered when a test is disabled or
 *       skipped by a condition (such as {@link RunOnProfiles} or {@code @Disabled}). It logs the
 *       test name and the provided reason.</li>
 * </ul>
 *
 * <p><b>How to use it effectively:</b><br>
 * Attach this extension to any test class using JUnit 5's {@code @ExtendWith} annotation:
 * <pre>{@code
 * @ExtendWith(SkipReasonLogger.class)
 * public class SmokeTestSuite extends SmokeTest {
 *     @Test
 *     void testServiceHealth() { ... }
 * }
 * }</pre>
 * Whenever a test in the class is skipped or aborted, the reason will be printed directly in
 * the test execution logs for quick diagnosis.
 */
public class SkipReasonLogger implements TestWatcher {

    /**
     * Invoked when a test is aborted (for example, when an assumption fails).
     *
     * @param ctx the current extension context of the test
     * @param cause the underlying reason or exception that caused the test to abort
     */
    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        System.out.println("SKIPPED " + ctx.getDisplayName() + ": " + cause.getMessage());
    }

    /**
     * Invoked when a test is disabled or skipped (for example, via {@code @Disabled} or a custom condition).
     *
     * @param ctx the current extension context of the test
     * @param reason an optional description explaining why the test was disabled
     */
    @Override
    public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
        System.out.println("DISABLED " + ctx.getDisplayName() + ": " + reason.orElse(""));
    }
}