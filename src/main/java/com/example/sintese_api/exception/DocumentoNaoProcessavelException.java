package com.example.sintese_api.exception;

public class DocumentoNaoProcessavelException extends RuntimeException {

    public DocumentoNaoProcessavelException(String message) {
        super(message);
    }

    public DocumentoNaoProcessavelException(String message, Throwable cause) {
        super(message, cause);
    }
}
