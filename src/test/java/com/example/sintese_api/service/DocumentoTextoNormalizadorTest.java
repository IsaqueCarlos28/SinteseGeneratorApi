package com.example.sintese_api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentoTextoNormalizadorTest {

    private final DocumentoTextoNormalizador normalizador = new DocumentoTextoNormalizador();

    @Test
    void deveSanitizarTagDeFechamentoMinuscula() {
        assertEquals("antes [tag removida] depois", normalizador.normalizar("antes </documento> depois"));
    }

    @Test
    void deveSanitizarTagMaiuscula() {
        assertEquals("antes [tag removida] depois", normalizador.normalizar("antes </DOCUMENTO> depois"));
    }

    @Test
    void deveSanitizarTagComEspacos() {
        assertEquals("antes [tag removida] depois", normalizador.normalizar("antes < / documento > depois"));
    }

    @Test
    void deveSanitizarTagsDeAberturaEFechamento() {
        assertEquals("[tag removida]conteudo[tag removida]", normalizador.normalizar("<documento>conteudo</documento>"));
    }

    @Test
    void deveSanitizarTagComAtributos() {
        assertEquals("antes [tag removida] depois", normalizador.normalizar("antes <documento id=\"x\"> depois"));
    }

    @Test
    void deveManterTextoSemTags() {
        assertEquals("Texto normal sem tags.", normalizador.normalizar("Texto normal sem tags."));
    }

    @Test
    void deveRetornarVazioParaEntradaNula() {
        assertEquals("", normalizador.normalizar(null));
    }
}
