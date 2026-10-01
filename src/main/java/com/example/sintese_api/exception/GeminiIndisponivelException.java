package com.example.sintese_api.exception;

public class GeminiIndisponivelException extends GeminiException {
    public GeminiIndisponivelException(String message, Integer upstreamStatus, String upstreamBody) {
        super(message, upstreamStatus, upstreamBody);
    }
}