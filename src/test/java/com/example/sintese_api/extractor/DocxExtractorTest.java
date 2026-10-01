package com.example.sintese_api.extractor;

import com.example.sintese_api.exception.DocumentoNaoProcessavelException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.example.sintese_api.support.Arquivos.DOCX_TYPE;
import static com.example.sintese_api.support.Arquivos.arquivo;
import static com.example.sintese_api.support.Arquivos.recurso;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxExtractorTest {

    private final DocxExtractor extractor = new DocxExtractor();

    @Test
    void deveSuportarSomenteDocx() {
        assertTrue(extractor.suporta(DOCX_TYPE));
        assertTrue(!extractor.suporta("application/pdf"));
    }

    @Test
    void deveExtrairTextoDeDocx() {
        var arquivo = arquivo("exemplo.docx", DOCX_TYPE,
                recurso("exemplo.docx"));

        String texto = extractor.extrair(arquivo);

        assertTrue(texto.contains("A reciclagem transforma materiais"));
        assertTrue(texto.contains("ação, coração, órgão"));
    }

    @Test
    void deveRejeitarDocxCorrompido() {
        var arquivo = arquivo("quebrado.docx", DOCX_TYPE,
                "isto não é um docx".getBytes(StandardCharsets.UTF_8));

        assertThrows(DocumentoNaoProcessavelException.class,
                () -> extractor.extrair(arquivo));
    }
}
