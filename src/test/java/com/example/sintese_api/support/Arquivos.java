package com.example.sintese_api.support;

import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** Utilitários de teste para montar arquivos enviados. */
public final class Arquivos {

    public static final String DOCX_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private Arquivos() {
    }

    public static MockMultipartFile arquivo(
            String nome,
            String contentType,
            byte[] conteudo
    ) {
        return new MockMultipartFile("documentos", nome, contentType, conteudo);
    }

    public static byte[] recurso(String nome) {

        try (InputStream entrada = Arquivos.class
                .getResourceAsStream("/documentos/" + nome)) {

            return entrada.readAllBytes();

        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
