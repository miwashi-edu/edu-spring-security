package org.example.test;

import org.example.App;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Whitebox base for tests that verify how the server is configured per profile.
 *
 * <p>Starts the real application ({@link App}) with a given profile on a random port, once per
 * test class, and exposes what Spring actually resolved: active profiles, exposed actuator
 * endpoints and the content of the info endpoint. Asking the running context (instead of reading
 * YAML files) means both {@code management.endpoints.web.exposure} and the
 * {@code management.endpoint(s).access} settings are taken into account.
 */
abstract class ProfileContext extends UnitTest {

    private static ConfigurableApplicationContext context;

    /**
     * Starts the application with the given profile (or none, to test the default).
     * Call from a {@code @BeforeAll} in the subclass.
     */
    static void start(String... profiles) {
        context = new SpringApplicationBuilder(App.class)
                .profiles(profiles)
                .logStartupInfo(false)
                // Command-line args override application.yaml (builder.properties() would not):
                // random free port, never clashes with a running server
                .run("--server.port=0");
    }

    @AfterAll
    static void stop() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    static String[] activeProfiles() {
        return context.getEnvironment().getActiveProfiles();
    }

    static String[] defaultProfiles() {
        return context.getEnvironment().getDefaultProfiles();
    }

    /** Ids of the actuator endpoints reachable over HTTP, e.g. {@code [health, info]}. */
    static Set<String> exposedEndpoints() {
        Set<String> ids = new TreeSet<>();
        for (ExposableWebEndpoint endpoint : context.getBean(WebEndpointsSupplier.class).getEndpoints()) {
            ids.add(endpoint.getEndpointId().toString());
        }
        return ids;
    }

    /** Content of {@code /actuator/info}, or an empty map if the info endpoint does not exist. */
    static Map<String, Object> info() {
        InfoEndpoint endpoint = context.getBeanProvider(InfoEndpoint.class).getIfAvailable();
        return endpoint == null ? Map.of() : endpoint.info();
    }
}
