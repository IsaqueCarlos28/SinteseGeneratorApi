package com.example.sintese_api.exception;

public class GeminiRateLimitException extends RuntimeException {
    public GeminiRateLimitException() {
        super("Limite de requisições da Gemini atingido.");
    }
}
