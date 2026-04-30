package com.gencatalog.client;

import com.gencatalog.exception.AiEngineException;
import com.gencatalog.model.EnrichedProduct;
import com.gencatalog.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client that calls the Python FastAPI AI engine's {@code POST /generate} endpoint.
 *
 * <p>The AI engine URL is read from the {@code ai.engine.url} property
 * (default: {@code http://localhost:8000}).
 *
 * <p>Throws {@link AiEngineException} on any HTTP error, timeout, or connection failure.
 * Never returns {@code null}.
 *
 * <p>Validates Requirements 3.4.
 */
@Component
public class AiEngineClient {

    private static final Logger log = LoggerFactory.getLogger(AiEngineClient.class);

    /** Timeout for each call to the AI engine. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    @org.springframework.beans.factory.annotation.Autowired
    public AiEngineClient(@Value("${ai.engine.url}") String aiEngineUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(aiEngineUrl)
                .build();
    }

    /**
     * Package-private constructor used in tests to inject a pre-built {@link WebClient}.
     */
    AiEngineClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Sends a generation request to the AI engine for the given product.
     *
     * @param product the product to enrich; must be fully populated
     * @return an {@link EnrichedProduct} with all four AI-generated fields; never {@code null}
     * @throws AiEngineException if the AI engine returns an HTTP error, is unreachable, or times out
     */
    public EnrichedProduct callAiEngine(Product product) {
        log.debug("Calling AI engine for product: {}", product.getProductName());

        Map<String, String> requestBody = Map.of(
                "product_name", product.getProductName(),
                "category", product.getCategory(),
                "price", product.getPrice()
        );

        try {
            AiEngineResponse response = webClient.post()
                    .uri("/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        int statusCode = clientResponse.statusCode().value();
                        return clientResponse.bodyToMono(String.class)
                                .map(body -> new AiEngineException(
                                        "AI engine returned HTTP " + statusCode + ": " + body))
                                .defaultIfEmpty(new AiEngineException(
                                        "AI engine returned HTTP " + statusCode));
                    })
                    .bodyToMono(AiEngineResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                throw new AiEngineException("AI engine returned an empty response");
            }

            EnrichedProduct enriched = new EnrichedProduct(
                    product.getProductName(),
                    product.getCategory(),
                    product.getPrice(),
                    response.description(),
                    response.tags(),
                    response.seoTitle(),
                    response.seoDescription()
            );

            log.debug("AI engine enriched product: {}", product.getProductName());
            return enriched;

        } catch (AiEngineException e) {
            throw e;
        } catch (WebClientRequestException e) {
            throw new AiEngineException("AI engine is unreachable: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AiEngineException("AI engine call failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal response record — maps snake_case JSON fields via @JsonProperty
    // -------------------------------------------------------------------------

    /**
     * Internal DTO for deserializing the AI engine's JSON response.
     * Uses {@link com.fasterxml.jackson.annotation.JsonProperty} to map snake_case fields.
     */
    private record AiEngineResponse(
            String description,
            String tags,
            @com.fasterxml.jackson.annotation.JsonProperty("seo_title") String seoTitle,
            @com.fasterxml.jackson.annotation.JsonProperty("seo_description") String seoDescription
    ) {}
}
