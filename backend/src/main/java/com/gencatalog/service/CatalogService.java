package com.gencatalog.service;

import com.gencatalog.client.AiEngineClient;
import com.gencatalog.exception.AiEngineException;
import com.gencatalog.model.CatalogResponse;
import com.gencatalog.model.EnrichedProduct;
import com.gencatalog.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the catalog enrichment pipeline:
 * <ol>
 *   <li>Parse the uploaded CSV into a list of {@link Product} objects.</li>
 *   <li>For each product, call the AI engine via {@link AiEngineClient}.</li>
 *   <li>On {@link AiEngineException}, insert a fallback {@link EnrichedProduct}.</li>
 *   <li>Record wall-clock processing time and return a {@link CatalogResponse}.</li>
 * </ol>
 *
 * <p>Validates Requirements 3.1, 3.2, 3.3, 3.5.
 */
@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final CsvParserService csvParserService;
    private final AiEngineClient aiEngineClient;

    public CatalogService(CsvParserService csvParserService, AiEngineClient aiEngineClient) {
        this.csvParserService = csvParserService;
        this.aiEngineClient = aiEngineClient;
    }

    /**
     * Enriches a single product, returning a fallback on any AI engine failure.
     * Never throws — always returns a fully populated {@link EnrichedProduct}.
     *
     * @param product the product to enrich
     * @return enriched product or fallback
     */
    public EnrichedProduct enrichSingle(Product product) {
        try {
            return aiEngineClient.callAiEngine(product);
        } catch (AiEngineException e) {
            log.warn("AI engine failed for product '{}': {} — using fallback",
                    product.getProductName(), e.getMessage());
            return buildFallback(product);
        }
    }

    /**
     * Processes the uploaded CSV file and returns an enriched catalog.
     *
     * <p>Every input product will have exactly one corresponding entry in the result —
     * either an AI-enriched product or a fallback entry if the AI engine failed.
     *
     * @param file the uploaded CSV file; must be non-null and non-empty
     * @return a {@link CatalogResponse} with all enriched products, total count, and elapsed time
     */
    public CatalogResponse processCatalog(MultipartFile file) {
        long startTime = System.currentTimeMillis();

        List<Product> products = csvParserService.parseCsv(file);
        List<EnrichedProduct> results = new ArrayList<>(products.size());

        for (Product product : products) {
            try {
                EnrichedProduct enriched = aiEngineClient.callAiEngine(product);
                results.add(enriched);
                log.debug("Enriched product: {}", product.getProductName());
            } catch (AiEngineException e) {
                log.warn("AI engine failed for product '{}': {} — using fallback",
                        product.getProductName(), e.getMessage());
                results.add(buildFallback(product));
            }
        }

        long processingTimeMs = System.currentTimeMillis() - startTime;

        return new CatalogResponse(results, results.size(), processingTimeMs);
    }

    // -------------------------------------------------------------------------
    // Fallback builder
    // -------------------------------------------------------------------------

    /**
     * Builds a fallback {@link EnrichedProduct} with placeholder values for all four
     * AI-generated fields. All fields are non-null and non-empty.
     *
     * <p>Fallback values:
     * <ul>
     *   <li>{@code description} — "Description unavailable"</li>
     *   <li>{@code tags} — "" (empty string, still non-null)</li>
     *   <li>{@code seo_title} — first 59 characters of product_name</li>
     *   <li>{@code seo_description} — "Product details coming soon."</li>
     * </ul>
     *
     * @param product the product for which AI generation failed
     * @return a fully populated fallback {@link EnrichedProduct}
     */
    EnrichedProduct buildFallback(Product product) {
        String seoTitle = product.getProductName().length() > 59
                ? product.getProductName().substring(0, 59)
                : product.getProductName();

        return new EnrichedProduct(
                product.getProductName(),
                product.getCategory(),
                product.getPrice(),
                "Description unavailable",
                "",
                seoTitle,
                "Product details coming soon."
        );
    }
}
