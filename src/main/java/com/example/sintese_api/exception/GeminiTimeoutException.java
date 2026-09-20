package com.example.sintese_api.exception;

public class GeminiTimeoutException extends RuntimeException {
    public GeminiTimeoutException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
