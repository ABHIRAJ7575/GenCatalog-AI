package com.gencatalog.controller;

import com.gencatalog.model.CatalogResponse;
import com.gencatalog.model.EnrichedProduct;
import com.gencatalog.model.Product;
import com.gencatalog.service.CatalogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller exposing the catalog enrichment endpoints.
 *
 * <p>{@code POST /process-catalog} — accepts a CSV file, enriches all products, returns CatalogResponse.
 * <p>{@code POST /enrich} — accepts a single Product JSON body, returns an EnrichedProduct.
 *    Used by the frontend for incremental per-product processing.
 *
 * <p>Validates Requirements 2.2, 2.3, 2.5, 3.4, 12.1, 12.2, 12.3.
 */
@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Processes an uploaded CSV file and returns an enriched catalog.
     *
     * <p>Validates that the uploaded file is a CSV (by content-type or filename extension).
     * Returns HTTP 400 if the file is not a CSV, if required columns are missing, or if
     * the CSV contains no data rows. Returns HTTP 503 if the AI engine is unreachable.
     *
     * @param file the uploaded CSV file
     * @return {@link CatalogResponse} with enriched products, total count, and processing time
     */
    @PostMapping(value = "/process-catalog", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CatalogResponse> processCatalog(
            @RequestParam("file") MultipartFile file) {

        if (!isCsvFile(file)) {
            throw new InvalidFileTypeException("Only CSV files are accepted. " +
                    "Please upload a file with a .csv extension or text/csv content type.");
        }

        CatalogResponse response = catalogService.processCatalog(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Enriches a single product and returns the result immediately.
     *
     * <p>Used by the frontend for incremental per-product processing so the UI can
     * render each result as soon as it arrives rather than waiting for the full batch.
     *
     * @param product the product to enrich
     * @return {@link EnrichedProduct} with AI-generated fields (or fallback values on failure)
     */
    @PostMapping(value = "/enrich", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnrichedProduct> enrich(@RequestBody Product product) {
        EnrichedProduct enriched = catalogService.enrichSingle(product);
        return ResponseEntity.ok(enriched);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the file appears to be a CSV based on its content-type
     * or original filename extension.
     */
    private boolean isCsvFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            // Accept text/csv and application/vnd.ms-excel (common CSV MIME types)
            if (contentType.equalsIgnoreCase("text/csv")
                    || contentType.equalsIgnoreCase("application/csv")
                    || contentType.equalsIgnoreCase("application/vnd.ms-excel")) {
                return true;
            }
        }

        // Fall back to filename extension check
        String originalFilename = file.getOriginalFilename();
        return originalFilename != null
                && originalFilename.toLowerCase().endsWith(".csv");
    }

    // -------------------------------------------------------------------------
    // Local exception for invalid file type (mapped to 400 by GlobalExceptionHandler)
    // -------------------------------------------------------------------------

    /**
     * Thrown when the uploaded file is not a CSV.
     */
    static class InvalidFileTypeException extends RuntimeException {
        InvalidFileTypeException(String message) {
            super(message);
        }
    }
}
