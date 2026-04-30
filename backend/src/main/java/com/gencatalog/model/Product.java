package com.gencatalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single product row parsed from the uploaded CSV file.
 * Required fields: product_name, category, price.
 */
public class Product {

    @JsonProperty("product_name")
    private String productName;
    private String category;
    private String price;

    public Product() {
    }

    public Product(String productName, String category, String price) {
        this.productName = productName;
        this.category = category;
        this.price = price;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Product{productName='" + productName + "', category='" + category + "', price='" + price + "'}";
    }
}
