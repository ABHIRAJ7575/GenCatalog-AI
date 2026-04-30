package com.gencatalog.service;

import com.gencatalog.exception.CsvParseException;
import com.gencatalog.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JUnit 5 tests for {@link CsvParserService}.
 *
 * <p>Validates Requirements 2.1, 2.2, 2.3, 2.4.
 */
class CsvParserServiceTest {

    private CsvParserService csvParserService;

    @BeforeEach
    void setUp() {
        csvParserService = new CsvParserService();
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

    // -------------------------------------------------------------------------
    // Requirement 2.1 — valid CSV parsed into Product list
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Valid CSV with 3 data rows returns a list of 3 Products")
    void validCsvWithThreeRows_returnsThreeProducts() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n"
                + "Samsung Galaxy S24,Smartphones,849.99\n"
                + "Google Pixel 8,Smartphones,699.99\n";

        List<Product> products = csvParserService.parseCsv(csvFile(csv));

        assertThat(products).hasSize(3);

        assertThat(products.get(0).getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(products.get(0).getCategory()).isEqualTo("Smartphones");
        assertThat(products.get(0).getPrice()).isEqualTo("999.99");

        assertThat(products.get(1).getProductName()).isEqualTo("Samsung Galaxy S24");
        assertThat(products.get(2).getProductName()).isEqualTo("Google Pixel 8");
    }

    // -------------------------------------------------------------------------
    // Requirement 2.2 — missing required column throws CsvParseException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSV missing 'price' column throws CsvParseException")
    void csvMissingPriceColumn_throwsCsvParseException() {
        String csv = "product_name,category\n"
                + "iPhone 15 Pro,Smartphones\n";

        assertThatThrownBy(() -> csvParserService.parseCsv(csvFile(csv)))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("price");
    }

    @Test
    @DisplayName("CSV missing 'product_name' column throws CsvParseException")
    void csvMissingProductNameColumn_throwsCsvParseException() {
        String csv = "category,price\n"
                + "Smartphones,999.99\n";

        assertThatThrownBy(() -> csvParserService.parseCsv(csvFile(csv)))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("product_name");
    }

    @Test
    @DisplayName("CSV missing all required columns throws CsvParseException listing all missing")
    void csvMissingAllRequiredColumns_throwsCsvParseExceptionWithAllMissing() {
        String csv = "name,cat,cost\n"
                + "Widget,Tools,5.00\n";

        assertThatThrownBy(() -> csvParserService.parseCsv(csvFile(csv)))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("product_name")
                .hasMessageContaining("category")
                .hasMessageContaining("price");
    }

    // -------------------------------------------------------------------------
    // Requirement 2.3 — headers present but no data rows throws CsvParseException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSV with headers but no data rows throws CsvParseException")
    void csvWithHeadersButNoDataRows_throwsCsvParseException() {
        String csv = "product_name,category,price\n";

        assertThatThrownBy(() -> csvParserService.parseCsv(csvFile(csv)))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("no product rows");
    }

    @Test
    @DisplayName("Completely empty file throws CsvParseException")
    void emptyFile_throwsCsvParseException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> csvParserService.parseCsv(emptyFile))
                .isInstanceOf(CsvParseException.class);
    }

    // -------------------------------------------------------------------------
    // Requirement 2.4 — rows with missing field values are skipped
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSV row with empty product_name is skipped; valid rows are returned")
    void csvRowMissingProductName_rowSkippedOthersReturned() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n"
                + ",Smartphones,849.99\n"          // missing product_name
                + "Google Pixel 8,Smartphones,699.99\n";

        List<Product> products = csvParserService.parseCsv(csvFile(csv));

        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getProductName)
                .containsExactly("iPhone 15 Pro", "Google Pixel 8");
    }

    @Test
    @DisplayName("CSV row with empty price is skipped; valid rows are returned")
    void csvRowMissingPrice_rowSkippedOthersReturned() {
        String csv = "product_name,category,price\n"
                + "iPhone 15 Pro,Smartphones,999.99\n"
                + "Samsung Galaxy S24,Smartphones,\n"  // missing price
                + "Google Pixel 8,Smartphones,699.99\n";

        List<Product> products = csvParserService.parseCsv(csvFile(csv));

        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getProductName)
                .containsExactly("iPhone 15 Pro", "Google Pixel 8");
    }

    @Test
    @DisplayName("CSV where all rows have missing fields throws CsvParseException (no valid rows)")
    void csvAllRowsMissingFields_throwsCsvParseException() {
        String csv = "product_name,category,price\n"
                + ",Smartphones,999.99\n"
                + "Samsung,,849.99\n"
                + "Google Pixel 8,,\n";

        assertThatThrownBy(() -> csvParserService.parseCsv(csvFile(csv)))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("no product rows");
    }

    // -------------------------------------------------------------------------
    // Additional edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSV headers with extra whitespace are still recognised")
    void csvHeadersWithWhitespace_parsedCorrectly() {
        String csv = " product_name , category , price \n"
                + "Widget,Tools,5.00\n";

        List<Product> products = csvParserService.parseCsv(csvFile(csv));

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getProductName()).isEqualTo("Widget");
    }

    @Test
    @DisplayName("CSV columns in non-standard order are parsed correctly")
    void csvColumnsInDifferentOrder_parsedCorrectly() {
        String csv = "price,product_name,category\n"
                + "999.99,iPhone 15 Pro,Smartphones\n";

        List<Product> products = csvParserService.parseCsv(csvFile(csv));

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getProductName()).isEqualTo("iPhone 15 Pro");
        assertThat(products.get(0).getCategory()).isEqualTo("Smartphones");
        assertThat(products.get(0).getPrice()).isEqualTo("999.99");
    }
}
