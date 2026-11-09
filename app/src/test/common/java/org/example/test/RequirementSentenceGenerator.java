package org.example.test;

import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns test names into spoken requirements, so that class + method read as one sentence:
 * <pre>
 * ActuatorExposureTest.shouldNotRevealUnusedPaths()  →  "Actuator exposure" &gt; "should not reveal unused paths"
 * HealthEndpointTest.shouldRespondWithJSON()         →  "Health endpoint"   &gt; "should respond with JSON"
 * </pre>
 * <ul>
 *   <li>Class: camel case split into words, {@code Test}/{@code Tests} suffix dropped, first letter upper case.</li>
 *   <li>Method: camel case split into words, all lower case.</li>
 *   <li>Acronyms written in capitals ({@code UP}, {@code JSON}) are kept as-is.</li>
 * </ul>
 * Applied to all HTTP tests via {@link BaseHttpTest}. Name tests "should ..." (TDD/BDD style) and
 * the harvested requirement is the test name itself.
 */
public class RequirementSentenceGenerator implements DisplayNameGenerator {

    private static final Pattern CAMEL_CASE =
            Pattern.compile("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

    @Override
    public String generateDisplayNameForClass(Class<?> testClass) {
        String sentence = sentence(testClass.getSimpleName().replaceFirst("Tests?$", ""));
        return sentence.isEmpty() ? sentence : Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
    }

    @Override
    public String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> nestedClass) {
        return sentence(nestedClass.getSimpleName());
    }

    @Override
    public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
        return sentence(testMethod.getName());
    }

    /** {@code shouldReportUP} → {@code should report UP}. */
    static String sentence(String camelCase) {
        StringBuilder out = new StringBuilder();
        for (String word : CAMEL_CASE.split(camelCase)) {
            if (word.isEmpty()) {
                continue;
            }
            boolean acronym = word.length() > 1 && word.equals(word.toUpperCase());
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(acronym ? word : word.toLowerCase());
        }
        return out.toString();
    }
}
