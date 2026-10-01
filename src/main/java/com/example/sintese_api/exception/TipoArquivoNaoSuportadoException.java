package com.example.sintese_api.exception;

public class TipoArquivoNaoSuportadoException extends RuntimeException {

    public TipoArquivoNaoSuportadoException(String message) {
        super(message);
    }

    public TipoArquivoNaoSuportadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
