package org.example;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public abstract class IntegrationTest {

    protected static String baseUrl;

    @BeforeAll
    static void setUpRestAssured() {
        baseUrl = System.getProperty("integration.baseUrl", System.getenv().getOrDefault("INTEGRATION_BASE_URL", "http://localhost:8080"));
        RestAssured.baseURI = baseUrl;
        RestAssured.port = RestAssured.UNDEFINED_PORT;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}