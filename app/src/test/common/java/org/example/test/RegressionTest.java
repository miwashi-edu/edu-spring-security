package org.example.test;

import org.junit.jupiter.api.Tag;

/**
 * Marker interface for regression test suites.
 *
 * <p><b>Purpose:</b><br>
 * A regression test ensures that previously developed features and bug fixes continue to work
 * as intended when new changes are made to the codebase. This interface allows developers to
 * categorize test classes as regression tests easily without manually repeating tag annotations.
 *
 * <p><b>How it works:</b><br>
 * This interface is annotated with JUnit 5's {@link Tag @Tag("regression")}. In JUnit 5, tags
 * declared on an interface are automatically inherited by any test class that implements it.
 * Build tools (such as Gradle or Maven) and continuous integration (CI) pipelines can filter tests
 * by this tag to run full regression test suites.
 *
 * <p><b>How to use it effectively:</b>
 * <ul>
 *   <li>Add {@code implements RegressionTest} to any test class that validates regression scenarios:
 *       <pre>{@code
 *       public class OrderProcessingTest implements RegressionTest {
 *           @Test
 *           void testOrderTotalCalculation() { ... }
 *       }
 *       }</pre>
 *   </li>
 *   <li>Execute all regression tests in your build tool by specifying the tag filter, for example
 *       in Gradle:
 *       <pre>{@code
 *       ./gradlew test -PincludeTags=regression
 *       }</pre>
 *   </li>
 * </ul>
 */
@Tag("regression")
public interface RegressionTest {
}