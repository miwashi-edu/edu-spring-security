package org.example.test;

import org.junit.jupiter.api.BeforeEach;



/** Base class for integration tests (src/test/integration). */
public abstract class IntegrationTest {
    protected String baseUrl;

    @BeforeEach
    void setUpBaseUrl() {
        this.baseUrl = System.getProperty("integration.baseUrl", "http://localhost:8080");
    }
}

