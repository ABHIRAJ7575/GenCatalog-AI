package com.gencatalog.controller;

import com.gencatalog.exception.AiEngineException;
import com.gencatalog.exception.CsvParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

/**
 * Global exception handler that maps application exceptions to structured JSON
 * error responses of the form {@code {"error": "..."}}.
 *
 * <p>Validates Requirements 12.1, 12.2, 12.3.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles CSV parsing errors (missing columns, empty file, no data rows).
     * Returns HTTP 400 with the exception message.
     */
    @ExceptionHandler(CsvParseException.class)
    public ResponseEntity<Map<String, String>> handleCsvParseException(CsvParseException ex) {
        log.warn("CSV parse error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles invalid file type uploads (non-CSV files).
     * Returns HTTP 400 with the exception message.
     */
    @ExceptionHandler(CatalogController.InvalidFileTypeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFileTypeException(
            CatalogController.InvalidFileTypeException ex) {
        log.warn("Invalid file type: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles multipart request errors (e.g. missing file parameter).
     * Returns HTTP 400.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipartException(MultipartException ex) {
        log.warn("Multipart request error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid multipart request: " + ex.getMessage()));
    }

    /**
     * Handles AI engine unreachable errors.
     * Returns HTTP 503 with a user-friendly message.
     */
    @ExceptionHandler(AiEngineException.class)
    public ResponseEntity<Map<String, String>> handleAiEngineException(AiEngineException ex) {
        log.error("AI engine error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "AI engine unavailable, please try again later"));
    }

    /**
     * Catch-all handler for any unhandled exceptions.
     * Returns HTTP 500 with a generic error message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
    }
}
