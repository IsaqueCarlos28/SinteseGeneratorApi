package com.example.sintese_api.exception;

public class ArquivoMuitoGrandeException extends RuntimeException {

    public ArquivoMuitoGrandeException(String message) {
        super(message);
    }

    public ArquivoMuitoGrandeException(String message, Throwable cause) {
        super(message, cause);
    }
}
