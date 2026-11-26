package com.example.sintese_api.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Remove tags {@code <documento>} e {@code </documento>} do conteúdo antes
 * de ele ser enviado ao modelo. Isso impede que o conteúdo fornecido pelo
 * usuário feche o delimitador usado no prompt e seja interpretado como
 * instrução.
 */
@Component
public class DocumentoSanitizer {

    private static final Pattern TAG_DOCUMENTO =
            Pattern.compile("(?i)<\\s*/?\\s*documento\\b[^>]*>");

    private static final String SUBSTITUICAO = "[tag removida]";

    public String sanitizarTagsDocumento(String texto) {
        if (texto == null) {
            return null;
        }
        return TAG_DOCUMENTO.matcher(texto).replaceAll(SUBSTITUICAO);
    }
}
