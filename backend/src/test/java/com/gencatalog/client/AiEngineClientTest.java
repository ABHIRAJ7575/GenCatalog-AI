package com.gencatalog.client;

import com.gencatalog.exception.AiEngineException;
import com.gencatalog.model.EnrichedProduct;
import com.gencatalog.model.Product;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JUnit 5 tests for {@link AiEngineClient} using OkHttp {@link MockWebServer}.
 *
 * <p>Validates Requirement 3.4.
 */
class AiEngineClientTest {

    private MockWebServer mockWebServer;
    private AiEngineClient aiEngineClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Build a WebClient pointing at the mock server, then inject via package-private ctor
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        aiEngineClient = new AiEngineClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Product sampleProduct() {
        return new Product("iPhone 15 Pro", "Smartphones", "999.99");
    }

    private static final String VALID_AI_RESPONSE = """
            {
              "description": "A fantastic smartphone with cutting-edge features.",
              "tags": "iPhone, Apple, Smartphone",
              "seo_title": "iPhone 15 Pro – Best Smartphone",
              "seo_description": "Buy the iPhone 15 Pro with A17 Pro chip."
            }
            """;

    // -------------------------------------------------------------------------
    // Requirement 3.4 — HTTP error triggers AiEngineException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("HTTP 503 response causes AiEngineException to be thrown")
    void http503Response_throwsAiEngineException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setBody("Service Unavailable"));

        assertThatThrownBy(() -> aiEngineClient.callAiEngine(sampleProduct()))
                .isInstanceOf(AiEngineException.class)
                .hasMessageContaining("503");
    }

    @Test
    @DisplayName("HTTP 500 response causes AiEngineException to be thrown")
    void http500Response_throwsAiEngineException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        assertThatThrownBy(() -> aiEngineClient.callAiEngine(sampleProduct()))
                .isInstanceOf(AiEngineException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("HTTP 404 response causes AiEngineException to be thrown")
    void http404Response_throwsAiEngineException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Not Found"));

        assertThatThrownBy(() -> aiEngineClient.callAiEngine(sampleProduct()))
                .isInstanceOf(AiEngineException.class)
                .hasMessageContaining("404");
    }

    // -------------------------------------------------------------------------
    // Requirement 3.4 — valid JSON response returns populated EnrichedProduct
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Valid JSON response returns EnrichedProduct with all fields populated")
    void validJsonResponse_returnsEnrichedProductWithAllFields() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(VALID_AI_RESPONSE));

        EnrichedProduct result = aiEngineClient.callAiEngine(sampleProduct());

        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(result.getCategory()).isEqualTo("Smartphones");
        assertThat(result.getPrice()).isEqualTo("999.99");
        assertThat(result.getDescription())
                .isEqualTo("A fantastic smartphone with cutting-edge features.");
        assertThat(result.getTags()).isEqualTo("iPhone, Apple, Smartphone");
        assertThat(result.getSeoTitle()).isEqualTo("iPhone 15 Pro – Best Smartphone");
        assertThat(result.getSeoDescription())
                .isEqualTo("Buy the iPhone 15 Pro with A17 Pro chip.");
    }

    @Test
    @DisplayName("Valid response preserves original product fields in EnrichedProduct")
    void validResponse_preservesOriginalProductFields() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(VALID_AI_RESPONSE));

        Product product = new Product("Samsung Galaxy S24", "Electronics", "849.99");
        EnrichedProduct result = aiEngineClient.callAiEngine(product);

        assertThat(result.getProductName()).isEqualTo("Samsung Galaxy S24");
        assertThat(result.getCategory()).isEqualTo("Electronics");
        assertThat(result.getPrice()).isEqualTo("849.99");
    }

    @Test
    @DisplayName("callAiEngine never returns null on success")
    void callAiEngine_neverReturnsNull_onSuccess() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(VALID_AI_RESPONSE));

        EnrichedProduct result = aiEngineClient.callAiEngine(sampleProduct());

        assertThat(result).isNotNull();
    }
}
