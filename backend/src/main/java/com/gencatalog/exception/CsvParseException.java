package com.gencatalog.exception;

/**
 * Thrown when the uploaded CSV file cannot be parsed due to missing required
 * headers, an empty file, or other structural problems.
 */
public class CsvParseException extends RuntimeException {

    public CsvParseException(String message) {
        super(message);
    }

    public CsvParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
