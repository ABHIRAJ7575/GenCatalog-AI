package com.gencatalog.service;

import com.gencatalog.exception.AiEngineException;
import com.gencatalog.model.CatalogResponse;
import com.gencatalog.model.EnrichedProduct;
import com.gencatalog.model.Product;
import net.jqwik.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * jqwik property-based tests for {@link CatalogService}.
 *
 * <p><b>Property 1: Complete output coverage</b>
 * <p>For any list of valid products parsed from a CSV, the CatalogResponse products array
 * SHALL contain exactly one entry (enriched or fallback) for each input product.
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3</b>
 */
class CatalogServicePropertyTest {

    // -------------------------------------------------------------------------
    // Property 1: Complete output coverage
    // Validates: Requirements 3.1, 3.2, 3.3
    // -------------------------------------------------------------------------

    /**
     * Property 1: Complete output coverage
     *
     * <p>For any list of 1–5 valid products, the CatalogResponse SHALL contain exactly
     * one entry per input product, regardless of whether the AI engine succeeds or fails.
     * The alternating mock simulates a realistic mix of successes and failures.
     *
     * <p><b>Validates: Requirements 3.1, 3.2, 3.3</b>
     */
    @Property
    void property1_completeOutputCoverage(
            @ForAll("validProductLists") List<Product> products) {

        // Build a CSV string from the generated products
        MockMultipartFile csvFile = buildCsvFile(products);

        // Alternating stub: even indices succeed, odd indices throw AiEngineException
        AtomicInteger callCount = new AtomicInteger(0);
        AlternatingAiEngineClient alternatingClient = new AlternatingAiEngineClient(callCount);

        CsvParserService csvParserService = new CsvParserService();
        CatalogService catalogService = new CatalogService(csvParserService, alternatingClient);

        CatalogResponse response = catalogService.processCatalog(csvFile);

        // Core property: output size must equal input size
        assertThat(response.getProducts())
                .as("CatalogResponse.products.size() must equal input product count")
                .hasSize(products.size());

        assertThat(response.getTotal())
                .as("CatalogResponse.total must equal input product count")
                .isEqualTo(products.size());

        // Every output entry must have a non-null, non-empty description
        for (int i = 0; i < products.size(); i++) {
            EnrichedProduct enriched = response.getProducts().get(i);
            assertThat(enriched.getProductName())
                    .as("Product at index %d must preserve product_name", i)
                    .isEqualTo(products.get(i).getProductName());
            assertThat(enriched.getDescription())
                    .as("Product at index %d must have a non-null description", i)
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Arbitraries (generators)
    // -------------------------------------------------------------------------

    /**
     * Generates lists of 1–5 valid {@link Product} objects.
     * Product fields use safe characters (no commas, newlines, or quotes)
     * so the CSV round-trip is unambiguous.
     */
    @Provide
    Arbitrary<List<Product>> validProductLists() {
        Arbitrary<Product> productArbitrary = Combinators.combine(
                safeString(1, 30),   // product_name
                safeString(1, 20),   // category
                validPrice()         // price
        ).as(Product::new);

        return productArbitrary.list().ofMinSize(1).ofMaxSize(5);
    }

    /**
     * Generates non-empty strings of printable ASCII characters that are safe
     * for CSV embedding (no commas, double-quotes, newlines, or carriage returns).
     */
    private Arbitrary<String> safeString(int minLength, int maxLength) {
        // Use alphanumeric characters only — safe for CSV without quoting,
        // and no leading/trailing spaces that would be trimmed by CsvParserService
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(minLength)
                .ofMaxLength(maxLength);
    }

    /**
     * Generates valid price strings like "1.99" to "9999.99".
     */
    private Arbitrary<String> validPrice() {
        return Arbitraries.integers()
                .between(1, 9999)
                .map(i -> i + ".99");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link MockMultipartFile} from a list of products by serializing
     * them to a CSV string with the required headers.
     */
    private MockMultipartFile buildCsvFile(List<Product> products) {
        StringBuilder csv = new StringBuilder("product_name,category,price\n");
        for (Product p : products) {
            csv.append(p.getProductName())
               .append(",")
               .append(p.getCategory())
               .append(",")
               .append(p.getPrice())
               .append("\n");
        }
        return new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Stub AiEngineClient — alternates between success and failure
    // -------------------------------------------------------------------------

    /**
     * A simple stub that extends {@link com.gencatalog.client.AiEngineClient} using the
     * public URL-based constructor (which only builds a WebClient and never connects).
     * Overrides {@code callAiEngine} to alternate between returning a valid
     * {@link EnrichedProduct} (even call indices) and throwing {@link AiEngineException}
     * (odd call indices), simulating a realistic mix of successes and failures.
     */
    private static class AlternatingAiEngineClient extends com.gencatalog.client.AiEngineClient {

        private final AtomicInteger callCount;

        AlternatingAiEngineClient(AtomicInteger callCount) {
            // Use a dummy URL — the WebClient is never actually invoked because
            // callAiEngine is fully overridden below.
            super("http://localhost:8000");
            this.callCount = callCount;
        }

        @Override
        public EnrichedProduct callAiEngine(Product product) {
            int index = callCount.getAndIncrement();
            if (index % 2 == 0) {
                // Success path: return a valid EnrichedProduct
                String seoTitle = product.getProductName().length() > 59
                        ? product.getProductName().substring(0, 59)
                        : product.getProductName();
                return new EnrichedProduct(
                        product.getProductName(),
                        product.getCategory(),
                        product.getPrice(),
                        "AI-generated description for " + product.getProductName(),
                        "tag1, tag2, tag3",
                        seoTitle,
                        "SEO description for " + product.getProductName()
                );
            } else {
                // Failure path: throw AiEngineException to trigger fallback
                throw new AiEngineException("Simulated AI engine failure for index " + index);
            }
        }
    }
}
