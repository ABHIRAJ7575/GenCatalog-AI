package com.gencatalog.service;

import com.gencatalog.client.AiEngineClient;
import com.gencatalog.exception.AiEngineException;
import com.gencatalog.model.CatalogResponse;
import com.gencatalog.model.EnrichedProduct;
import com.gencatalog.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * JUnit 5 + Mockito tests for {@link CatalogService}.
 *
 * <p>Validates Requirements 3.1, 3.2, 3.3, 3.5.
 */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private AiEngineClient aiEngineClient;

    private CsvParserService csvParserService;
    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        csvParserService = new CsvParserService();
        catalogService = new CatalogService(csvParserService, aiEngineClient);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private EnrichedProduct enrichedProduct(Product product, String suffix) {
        return new EnrichedProduct(
                product.getProductName(),
                product.getCategory(),
                product.getPrice(),
                "Description for " + product.getProductName() + suffix,
                "tag1, tag2, tag3",
                product.getProductName() + " - SEO Title",
                "SEO description for " + product.getProductName()
        );
    }

    // -------------------------------------------------------------------------
    // Requirement 3.1, 3.2 — all products enriched successfully
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Mock AiEngineClient returns success for all products; result count equals input count")
    void allProductsEnrichedSuccessfully_resultCountEqualsInputCount() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n"
                + "Samsung Galaxy S24,Smartphones,849.99\n"
                + "Google Pixel 8,Smartphones,699.99\n";

        // Mock: return enriched product for any input
        when(aiEngineClient.callAiEngine(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    return enrichedProduct(p, " (AI-generated)");
                });

        CatalogResponse response = catalogService.processCatalog(csvFile(csv));

        assertThat(response).isNotNull();
        assertThat(response.getProducts()).hasSize(3);
        assertThat(response.getTotal()).isEqualTo(3);

        // Verify all products have enriched descriptions
        assertThat(response.getProducts())
                .allMatch(p -> p.getDescription().contains("AI-generated"));
    }

    // -------------------------------------------------------------------------
    // Requirement 3.3 — fallback entry inserted on AiEngineException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Mock AiEngineClient throws AiEngineException for one product; fallback entry present and other products enriched")
    void oneProductFails_fallbackEntryPresentAndOthersEnriched() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n"
                + "Samsung Galaxy S24,Smartphones,849.99\n"
                + "Google Pixel 8,Smartphones,699.99\n";

        // Mock: fail for the second product, succeed for others
        when(aiEngineClient.callAiEngine(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    if (p.getProductName().equals("Samsung Galaxy S24")) {
                        throw new AiEngineException("AI engine failed for this product");
                    }
                    return enrichedProduct(p, " (AI-generated)");
                });

        CatalogResponse response = catalogService.processCatalog(csvFile(csv));

        assertThat(response).isNotNull();
        assertThat(response.getProducts()).hasSize(3);
        assertThat(response.getTotal()).isEqualTo(3);

        // First product should be enriched
        EnrichedProduct first = response.getProducts().get(0);
        assertThat(first.getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(first.getDescription()).contains("AI-generated");

        // Second product should have fallback
        EnrichedProduct second = response.getProducts().get(1);
        assertThat(second.getProductName()).isEqualTo("Samsung Galaxy S24");
        assertThat(second.getDescription()).isEqualTo("Description unavailable");
        assertThat(second.getTags()).isEmpty();
        assertThat(second.getSeoTitle()).isEqualTo("Samsung Galaxy S24");
        assertThat(second.getSeoDescription()).isEqualTo("Product details coming soon.");

        // Third product should be enriched
        EnrichedProduct third = response.getProducts().get(2);
        assertThat(third.getProductName()).isEqualTo("Google Pixel 8");
        assertThat(third.getDescription()).contains("AI-generated");
    }

    @Test
    @DisplayName("All products fail; all fallback entries present")
    void allProductsFail_allFallbackEntriesPresent() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n"
                + "Samsung Galaxy S24,Smartphones,849.99\n";

        // Mock: always throw exception
        when(aiEngineClient.callAiEngine(any(Product.class)))
                .thenThrow(new AiEngineException("AI engine unavailable"));

        CatalogResponse response = catalogService.processCatalog(csvFile(csv));

        assertThat(response).isNotNull();
        assertThat(response.getProducts()).hasSize(2);
        assertThat(response.getTotal()).isEqualTo(2);

        // All products should have fallback
        assertThat(response.getProducts())
                .allMatch(p -> p.getDescription().equals("Description unavailable"));
    }

    // -------------------------------------------------------------------------
    // Requirement 3.5 — processing_time_ms is non-negative
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processing_time_ms is non-negative")
    void processingTimeMs_isNonNegative() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n";

        when(aiEngineClient.callAiEngine(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    return enrichedProduct(p, "");
                });

        CatalogResponse response = catalogService.processCatalog(csvFile(csv));

        assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("processing_time_ms increases with processing time")
    void processingTimeMs_increasesWithProcessingTime() throws InterruptedException {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n";

        // Mock: add a small delay to simulate processing
        when(aiEngineClient.callAiEngine(any(Product.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(10); // 10ms delay
                    Product p = invocation.getArgument(0);
                    return enrichedProduct(p, "");
                });

        CatalogResponse response = catalogService.processCatalog(csvFile(csv));

        // Should be at least 10ms (allowing for some variance)
        assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // Additional edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Single product CSV processed correctly")
    void singleProductCsv_processedCorrectly() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n";

        when(aiEngineClient.callAiEngine(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    return enrichedProduct(p, "");
                });

        CatalogResponse response = catalogService.processCatalog(csvFile(csv));

        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fallback for product with long name truncates seo_title to 59 chars")
    void fallbackForLongProductName_truncatesSeoTitle() {
        String longName = "A".repeat(100);
        String csv = "product_name,category,price\n"
                + longName + ",Smartphones,999.99\n";

        when(aiEngineClient.callAiEngine(any(Product.class)))
                .thenThrow(new AiEngineException("AI engine failed"));

        CatalogResponse response = catalogService.processCatalog(csvFile(csv));

        EnrichedProduct product = response.getProducts().get(0);
        assertThat(product.getSeoTitle()).hasSize(59);
        assertThat(product.getSeoTitle()).isEqualTo("A".repeat(59));
    }
}
