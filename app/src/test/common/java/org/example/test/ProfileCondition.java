package org.example.test;

import org.example.test.RunOnProfiles;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Arrays;

import static io.restassured.RestAssured.given;

/**
 * A JUnit 5 execution condition that dynamically enables or skips tests based on the active
 * environment profile of the running application.
 *
 * <p><b>Purpose:</b><br>
 * Modern applications often behave differently depending on their active profile (such as
 * {@code "dev"}, {@code "test"}, or {@code "prod"}). Certain tests are only meaningful in
 * specific environments. This condition checks the running application's active profile and
 * automatically prevents tests from running in unsupported profiles, saving execution time
 * and preventing false test failures.
 *
 * <p><b>How it works:</b><br>
 * When a test class or test method is annotated with {@link RunOnProfiles}, this class is invoked
 * by the JUnit 5 test framework before executing the test. It performs the following steps:
 * <ol>
 *   <li>Inspects the test element (class or method) for the {@link RunOnProfiles} annotation.
 *       If absent, the test is allowed to run unconditionally.</li>
 *   <li>Queries the application's {@code /actuator/info} endpoint to fetch the currently active profile
 *       (e.g., {@code app.profiles}), caching the result across tests for efficiency.</li>
 *   <li>Compares the active profile with the list of allowed profiles declared in {@link RunOnProfiles}.
 *       If matched, the test is enabled; otherwise, it is skipped with an explanatory message.</li>
 * </ol>
 *
 * <p><b>How to use it effectively:</b><br>
 * Rather than referencing this class directly, simply annotate your test class or method with
 * {@link RunOnProfiles}:
 * <pre>{@code
 * @RunOnProfiles("dev")
 * public class DevOnlyFeatureTest {
 *     @Test
 *     void testFeature() { ... }
 * }
 * }</pre>
 *
 * @see RunOnProfiles
 */
public class ProfileCondition implements ExecutionCondition {

    private static final ExtensionContext.Namespace NS =
            ExtensionContext.Namespace.create(ProfileCondition.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx) {
        var annotation = AnnotationSupport.findAnnotation(ctx.getElement(), RunOnProfiles.class);
        if (annotation.isEmpty()) {
            return ConditionEvaluationResult.enabled("No profile restriction");
        }

        String active = ctx.getRoot().getStore(NS).getOrComputeIfAbsent(
                "profile",
                k -> given().get("/actuator/info").jsonPath().getString("app.profiles"),
                String.class);

        return Arrays.asList(annotation.get().value()).contains(active)
                ? ConditionEvaluationResult.enabled("Profile " + active)
                : ConditionEvaluationResult.disabled("Skipped on profile " + active);
    }
}