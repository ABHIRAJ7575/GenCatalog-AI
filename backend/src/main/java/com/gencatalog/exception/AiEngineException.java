package com.gencatalog.exception;

/**
 * Thrown when the AI engine (Python FastAPI service) returns an error,
 * is unreachable, or times out during a generation request.
 */
public class AiEngineException extends RuntimeException {

    public AiEngineException(String message) {
        super(message);
    }

    public AiEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
