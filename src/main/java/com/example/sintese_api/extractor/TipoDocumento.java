package com.example.sintese_api.extractor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Formatos de documento aceitos pela API.
 * Para suportar um novo formato: adicione uma constante aqui e crie um
 * {@link DocumentoExtractor} anotado com @Component.
 */
public enum TipoDocumento {

    TXT("txt", "text/plain"),
    PDF("pdf", "application/pdf"),
    DOCX(
            "docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final String extensao;
    private final String contentType;

    TipoDocumento(String extensao, String contentType) {
        this.extensao = extensao;
        this.contentType = contentType;
    }

    public String extensao() {
        return extensao;
    }

    public String contentType() {
        return contentType;
    }

    public static Optional<TipoDocumento> porExtensao(String extensao) {

        if (extensao == null) {
            return Optional.empty();
        }

        String normalizada = extensao.toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(tipo -> tipo.extensao.equals(normalizada))
                .findFirst();
    }

    public static String extensoesAceitas() {
        return Arrays.stream(values())
                .map(tipo -> "." + tipo.extensao)
                .collect(Collectors.joining(", "));
    }
}
