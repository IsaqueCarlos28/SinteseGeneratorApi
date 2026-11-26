package com.example.sintese_api.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Normaliza texto de documentos antes de serem usados no prompt.
 * <p>
 * A normalização remove caracteres problemáticos e neutraliza tags
 * {@code <documento>} / {@code </documento>} para impedir que o conteúdo
 * enviado pelo usuário feche o delimitador do prompt e seja interpretado
 * como instrução.
 */
@Component
public class DocumentoTextoNormalizador {

    private static final Pattern TAG_DOCUMENTO =
            Pattern.compile("(?i)<\\s*/?\\s*documento\\b[^>]*>");

    private static final Pattern CARACTERES_DE_CONTROLE =
            Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");

    private static final Pattern LINHAS_EM_BRANCO_EXCESSIVAS =
            Pattern.compile("\\n{3,}");

    public String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String resultado = texto
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        resultado = CARACTERES_DE_CONTROLE
                .matcher(resultado).replaceAll("");

        resultado = TAG_DOCUMENTO
                .matcher(resultado).replaceAll("[tag removida]");

        resultado = LINHAS_EM_BRANCO_EXCESSIVAS
                .matcher(resultado).replaceAll("\n\n");

        return resultado.trim();
    }
}
