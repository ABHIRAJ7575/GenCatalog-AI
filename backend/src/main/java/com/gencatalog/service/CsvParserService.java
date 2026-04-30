package com.gencatalog.service;

import com.gencatalog.exception.CsvParseException;
import com.gencatalog.model.Product;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses an uploaded CSV file into a list of {@link Product} objects.
 *
 * <p>Required CSV headers (case-sensitive): {@code product_name}, {@code category}, {@code price}.
 * Rows with missing field values are skipped with a warning log.
 * Throws {@link CsvParseException} when headers are missing or the file is empty.
 */
@Service
public class CsvParserService {

    private static final Logger log = LoggerFactory.getLogger(CsvParserService.class);

    static final String HEADER_PRODUCT_NAME = "product_name";
    static final String HEADER_CATEGORY = "category";
    static final String HEADER_PRICE = "price";

    private static final List<String> REQUIRED_HEADERS =
            List.of(HEADER_PRODUCT_NAME, HEADER_CATEGORY, HEADER_PRICE);

    /**
     * Parses the given CSV {@link MultipartFile} into a list of {@link Product} objects.
     *
     * @param file the uploaded CSV file; must be non-null and non-empty
     * @return list of products with all three fields populated
     * @throws CsvParseException if the file is empty, headers are missing, or no data rows exist
     */
    public List<Product> parseCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CsvParseException("CSV file is empty or missing");
        }

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Read and validate header row
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new CsvParseException("CSV file is empty or missing");
            }

            // Trim headers to handle any surrounding whitespace
            List<String> headerList = Arrays.stream(headers)
                    .map(String::trim)
                    .toList();

            List<String> missingHeaders = REQUIRED_HEADERS.stream()
                    .filter(h -> !headerList.contains(h))
                    .toList();

            if (!missingHeaders.isEmpty()) {
                throw new CsvParseException(
                        "Invalid CSV: missing required columns: " + String.join(", ", missingHeaders));
            }

            int nameIdx = headerList.indexOf(HEADER_PRODUCT_NAME);
            int categoryIdx = headerList.indexOf(HEADER_CATEGORY);
            int priceIdx = headerList.indexOf(HEADER_PRICE);

            List<Product> products = new ArrayList<>();
            String[] row;
            int rowNumber = 1; // 1-based, header is row 0

            while ((row = reader.readNext()) != null) {
                rowNumber++;

                // Skip rows that don't have enough columns
                if (row.length <= Math.max(nameIdx, Math.max(categoryIdx, priceIdx))) {
                    log.warn("Row {} has insufficient columns — skipping", rowNumber);
                    continue;
                }

                String productName = row[nameIdx].trim();
                String category = row[categoryIdx].trim();
                String price = row[priceIdx].trim();

                if (productName.isEmpty() || category.isEmpty() || price.isEmpty()) {
                    log.warn("Row {} is missing one or more required field values — skipping", rowNumber);
                    continue;
                }

                products.add(new Product(productName, category, price));
            }

            if (products.isEmpty()) {
                throw new CsvParseException("CSV file contains no product rows");
            }

            return products;

        } catch (IOException e) {
            throw new CsvParseException("Failed to read CSV file: " + e.getMessage(), e);
        } catch (CsvValidationException e) {
            throw new CsvParseException("CSV validation error: " + e.getMessage(), e);
        }
    }
}
