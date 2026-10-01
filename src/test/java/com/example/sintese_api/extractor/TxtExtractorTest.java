package com.example.sintese_api.extractor;

import com.example.sintese_api.exception.DocumentoNaoProcessavelException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.example.sintese_api.support.Arquivos.arquivo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TxtExtractorTest {

    private final TxtExtractor extractor = new TxtExtractor();

    @Test
    void deveSuportarSomenteTextPlain() {
        assertTrue(extractor.suporta("text/plain"));
        assertTrue(!extractor.suporta("application/pdf"));
    }

    @Test
    void deveLerUtf8() {
        var arquivo = arquivo("a.txt", "text/plain",
                "Ação e coração.".getBytes(StandardCharsets.UTF_8));

        assertEquals("Ação e coração.", extractor.extrair(arquivo));
    }

    @Test
    void deveLerWindows1252() {
        var arquivo = arquivo("a.txt", "text/plain",
                "Ação e coração.".getBytes(Charset.forName("windows-1252")));

        assertEquals("Ação e coração.", extractor.extrair(arquivo));
    }

    @Test
    void deveLerUtf16ComBom() {
        var arquivo = arquivo("a.txt", "text/plain",
                "Ação e coração.".getBytes(StandardCharsets.UTF_16));

        assertEquals("Ação e coração.", extractor.extrair(arquivo));
    }

    @Test
    void deveRejeitarArquivoBinarioComExtensaoTxt() {
        var arquivo = arquivo("a.txt", "text/plain",
                new byte[]{'M', 'Z', 0, 0, 1, 2, 3});

        assertThrows(DocumentoNaoProcessavelException.class,
                () -> extractor.extrair(arquivo));
    }
}
