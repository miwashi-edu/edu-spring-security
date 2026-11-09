package org.example.test;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Arrays;

/**
 * JUnit 5 execution condition behind {@link RunOnProfiles}: enables or skips tests depending on
 * which environment the test run targets.
 *
 * <p><b>How it works:</b><br>
 * The target environment is declared by the test run through the environment variable
 * {@code TEST_PROFILE} ({@link Constants#PROFILE}, default {@code dev}). It is deliberately
 * <i>not</i> asked from the server: production may not expose {@code /actuator/info}, and a test
 * of the info endpoint (REQ-004) must not be gated by its own result.
 *
 * <p>Tests without {@link RunOnProfiles} always run. Annotated tests run only if
 * {@code TEST_PROFILE} is one of the listed profiles; otherwise they are skipped with a reason.
 *
 * <pre>{@code
 * @RunOnProfiles({"dev", "stage"})   // never runs with TEST_PROFILE=prod
 * public class InfoEndpointTest extends SmokeTest { ... }
 * }</pre>
 *
 * @see RunOnProfiles
 */
public class ProfileCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx) {
        var annotation = AnnotationSupport.findAnnotation(ctx.getElement(), RunOnProfiles.class);
        if (annotation.isEmpty()) {
            return ConditionEvaluationResult.enabled("No profile restriction");
        }

        String active = Constants.PROFILE;
        String[] allowed = annotation.get().value();

        return Arrays.asList(allowed).contains(active)
                ? ConditionEvaluationResult.enabled("Profile " + active)
                : ConditionEvaluationResult.disabled("Skipped on profile " + active
                        + " (runs only on " + String.join(", ", allowed) + "; set TEST_PROFILE to change)");
    }
}