package com.gencatalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Extends Product with AI-generated catalog enrichment fields:
 * description, tags, seo_title, and seo_description.
 */
public class EnrichedProduct extends Product {

    private String description;
    private String tags;

    @JsonProperty("seo_title")
    private String seoTitle;

    @JsonProperty("seo_description")
    private String seoDescription;

    public EnrichedProduct() {
        super();
    }

    public EnrichedProduct(String productName, String category, String price,
                           String description, String tags,
                           String seoTitle, String seoDescription) {
        super(productName, category, price);
        this.description = description;
        this.tags = tags;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    @Override
    public String toString() {
        return "EnrichedProduct{productName='" + getProductName()
                + "', category='" + getCategory()
                + "', price='" + getPrice()
                + "', description='" + description
                + "', tags='" + tags
                + "', seoTitle='" + seoTitle
                + "', seoDescription='" + seoDescription + "'}";
    }
}
