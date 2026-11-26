package com.example.sintese_api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentoSanitizerTest {

    private final DocumentoSanitizer sanitizer = new DocumentoSanitizer();

    @Test
    void deveSanitizarTagDeFechamentoMinuscula() {
        assertEquals("antes [tag removida] depois", sanitizer.sanitizarTagsDocumento("antes </documento> depois"));
    }

    @Test
    void deveSanitizarTagMaiuscula() {
        assertEquals("antes [tag removida] depois", sanitizer.sanitizarTagsDocumento("antes </DOCUMENTO> depois"));
    }

    @Test
    void deveSanitizarTagComEspacos() {
        assertEquals("antes [tag removida] depois", sanitizer.sanitizarTagsDocumento("antes < / documento > depois"));
    }

    @Test
    void deveSanitizarTagsDeAberturaEFechamento() {
        assertEquals("[tag removida]conteudo[tag removida]", sanitizer.sanitizarTagsDocumento("<documento>conteudo</documento>"));
    }

    @Test
    void deveSanitizarTagComAtributos() {
        assertEquals("antes [tag removida] depois", sanitizer.sanitizarTagsDocumento("antes <documento id=\"x\"> depois"));
    }

    @Test
    void deveManterTextoSemTags() {
        assertEquals("Texto normal sem tags.", sanitizer.sanitizarTagsDocumento("Texto normal sem tags."));
    }

    @Test
    void deveRetornarNullParaEntradaNula() {
        assertNull(sanitizer.sanitizarTagsDocumento(null));
    }
}
