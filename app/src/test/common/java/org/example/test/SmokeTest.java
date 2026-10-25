package org.example.test;

import org.junit.jupiter.api.BeforeEach;

public abstract class SmokeTest {
    protected String baseUrl;

    @BeforeEach
    void setUpBaseUrl() {
        this.baseUrl = System.getProperty("smoke.baseUrl", "http://localhost:8080");
    }
}