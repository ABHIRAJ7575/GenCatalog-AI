package com.gencatalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gencatalog.exception.AiEngineException;
import com.gencatalog.exception.CsvParseException;
import com.gencatalog.model.CatalogResponse;
import com.gencatalog.model.EnrichedProduct;
import com.gencatalog.service.CatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for {@link CatalogController}.
 *
 * <p>Validates Requirements 2.2, 2.3, 2.5, 12.1, 12.2.
 */
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Test: valid CSV multipart upload → 200 with CatalogResponse JSON
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Valid CSV upload returns 200 with CatalogResponse JSON")
    void validCsvUpload_returns200WithCatalogResponse() throws Exception {
        // Arrange: mock CatalogService to return a CatalogResponse with 1 product
        EnrichedProduct product = new EnrichedProduct(
                "iPhone 15 Pro", "Smartphones", "999.99",
                "A great smartphone with advanced features.",
                "iPhone, Apple, Smartphone",
                "iPhone 15 Pro – Titanium",
                "Buy iPhone 15 Pro with A17 Pro chip."
        );
        CatalogResponse mockResponse = new CatalogResponse(List.of(product), 1, 1234L);
        when(catalogService.processCatalog(any())).thenReturn(mockResponse);

        String csvContent = "product_name,category,price\niPhone 15 Pro,Smartphones,999.99\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csvContent.getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/process-catalog").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].product_name", is("iPhone 15 Pro")))
                .andExpect(jsonPath("$.processing_time_ms", is(1234)));
    }

    // -------------------------------------------------------------------------
    // Test: non-CSV content-type → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Non-CSV content-type returns 400")
    void nonCsvContentType_returns400() throws Exception {
        // Arrange: upload a file with text/plain content type and no .csv extension
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.txt", "text/plain",
                "some plain text content".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/process-catalog").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("JSON content-type returns 400")
    void jsonContentType_returns400() throws Exception {
        // Arrange: upload a file with application/json content type
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.json", "application/json",
                "{\"key\": \"value\"}".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/process-catalog").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    // -------------------------------------------------------------------------
    // Test: CSV missing columns → 400 with error field
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSV missing required columns returns 400 with error field")
    void csvMissingColumns_returns400WithErrorField() throws Exception {
        // Arrange: CatalogService throws CsvParseException for missing columns
        when(catalogService.processCatalog(any()))
                .thenThrow(new CsvParseException(
                        "Invalid CSV: missing required columns: category, price"));

        String csvContent = "product_name\niPhone 15 Pro\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csvContent.getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/process-catalog").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("missing required columns")));
    }

    // -------------------------------------------------------------------------
    // Test: empty CSV → 400 with "CSV file contains no product rows"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Empty CSV (headers only) returns 400 with 'CSV file contains no product rows'")
    void emptyCsv_returns400WithNoProductRowsMessage() throws Exception {
        // Arrange: CatalogService throws CsvParseException for empty CSV
        when(catalogService.processCatalog(any()))
                .thenThrow(new CsvParseException("CSV file contains no product rows"));

        String csvContent = "product_name,category,price\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", csvContent.getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/process-catalog").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("CSV file contains no product rows")));
    }

    // -------------------------------------------------------------------------
    // Test: AI engine unreachable → 503
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AI engine unreachable returns 503 with error message")
    void aiEngineUnreachable_returns503() throws Exception {
        // Arrange: CatalogService throws AiEngineException
        when(catalogService.processCatalog(any()))
                .thenThrow(new AiEngineException("Connection refused"));

        String csvContent = "product_name,category,price\niPhone 15 Pro,Smartphones,999.99\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csvContent.getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/process-catalog").file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", is("AI engine unavailable, please try again later")));
    }

    // -------------------------------------------------------------------------
    // Test: CSV file with .csv extension but octet-stream content-type → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSV file with octet-stream content-type but .csv extension is accepted")
    void csvFileWithOctetStreamContentType_isAccepted() throws Exception {
        // Arrange: some browsers send application/octet-stream for CSV files
        EnrichedProduct product = new EnrichedProduct(
                "Test Product", "Electronics", "49.99",
                "A test product description.",
                "test, product",
                "Test Product",
                "Buy Test Product online."
        );
        CatalogResponse mockResponse = new CatalogResponse(List.of(product), 1, 500L);
        when(catalogService.processCatalog(any())).thenReturn(mockResponse);

        String csvContent = "product_name,category,price\nTest Product,Electronics,49.99\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "application/octet-stream", csvContent.getBytes());

        // Act & Assert: should be accepted because filename ends with .csv
        mockMvc.perform(multipart("/process-catalog").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)));
    }
}
