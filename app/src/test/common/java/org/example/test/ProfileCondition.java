package org.example.test;

import org.example.test.RunOnProfiles;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Arrays;

import static io.restassured.RestAssured.given;

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