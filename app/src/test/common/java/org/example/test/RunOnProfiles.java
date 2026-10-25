package org.example.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to restrict test execution to one or more specific application profiles.
 *
 * <p><b>Purpose:</b><br>
 * In modern applications, certain behaviors or endpoints are only active in specific
 * environments (e.g., development, testing, staging, or production). This annotation allows
 * developers to state clearly which environment profiles a test supports, ensuring tests only
 * run when the application is configured appropriately and are gracefully skipped otherwise.
 *
 * <p><b>How it works:</b><br>
 * This annotation is linked to {@link ProfileCondition} via {@link ExtendWith @ExtendWith}.
 * Before running any annotated test class or test method, the framework checks the running
 * application's active profile (retrieved via {@code /actuator/info}). If the active profile
 * matches any of the values passed to this annotation, the test executes normally. Otherwise,
 * the test is automatically skipped without failing the build.
 *
 * <p><b>How to use it effectively:</b>
 * <ul>
 *   <li><b>Annotating an entire test class:</b> Restricts all tests inside the class to run only
 *       when the application is running with one of the specified profiles:
 *       <pre>{@code
 *       @RunOnProfiles({"dev", "local"})
 *       public class DevelopmentToolsTest extends SmokeTest {
 *           @Test
 *           void testDevConsole() { ... }
 *       }
 *       }</pre>
 *   </li>
 *   <li><b>Annotating an individual test method:</b> Restricts only that specific test method:
 *       <pre>{@code
 *       public class FeatureTest extends SmokeTest {
 *           @Test
 *           @RunOnProfiles("prod")
 *           void testProductionMetrics() { ... }
 *       }
 *       }</pre>
 *   </li>
 * </ul>
 *
 * @see ProfileCondition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(ProfileCondition.class)
public @interface RunOnProfiles {
    /**
     * The list of acceptable profile names under which the test is permitted to run
     * (e.g., {@code "dev"}, {@code "staging"}, {@code "prod"}).
     *
     * @return an array of profile names
     */
    String[] value();
}
