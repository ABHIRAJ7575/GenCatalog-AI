package com.gencatalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response payload returned by the /process-catalog endpoint.
 * Contains the list of enriched products, total count, and processing time.
 */
public class CatalogResponse {

    private List<EnrichedProduct> products;
    private int total;

    @JsonProperty("processing_time_ms")
    private long processingTimeMs;

    public CatalogResponse() {
    }

    public CatalogResponse(List<EnrichedProduct> products, int total, long processingTimeMs) {
        this.products = products;
        this.total = total;
        this.processingTimeMs = processingTimeMs;
    }

    public List<EnrichedProduct> getProducts() {
        return products;
    }

    public void setProducts(List<EnrichedProduct> products) {
        this.products = products;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    @Override
    public String toString() {
        return "CatalogResponse{products=" + products
                + ", total=" + total
                + ", processingTimeMs=" + processingTimeMs + "}";
    }
}
