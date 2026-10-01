package com.example.sintese_api.extractor;

import com.example.sintese_api.exception.DocumentoNaoProcessavelException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.example.sintese_api.support.Arquivos.arquivo;
import static com.example.sintese_api.support.Arquivos.recurso;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExtractorTest {

    private final PdfExtractor extractor = new PdfExtractor();

    @Test
    void deveSuportarSomenteApplicationPdf() {
        assertTrue(extractor.suporta("application/pdf"));
        assertTrue(!extractor.suporta("text/plain"));
    }

    @Test
    void deveExtrairTextoDePdf() {
        var arquivo = arquivo("exemplo.pdf", "application/pdf",
                recurso("exemplo.pdf"));

        String texto = extractor.extrair(arquivo);

        assertTrue(texto.contains("A reciclagem transforma materiais"));
    }

    @Test
    void deveRejeitarPdfCorrompido() {
        var arquivo = arquivo("quebrado.pdf", "application/pdf",
                "isto não é um pdf".getBytes(StandardCharsets.UTF_8));

        assertThrows(DocumentoNaoProcessavelException.class,
                () -> extractor.extrair(arquivo));
    }

    @Test
    void devePermitirPdfSemTextoParaQueOServicoRejeite() throws IOException {
        // PDF válido, mas só com uma página em branco (como um scan sem OCR).
        // O extractor devolve texto vazio; quem rejeita é o DocumentoService.
        byte[] pdfEmBranco;

        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {

            documento.addPage(new PDPage());
            documento.save(saida);
            pdfEmBranco = saida.toByteArray();
        }

        var arquivo = arquivo("branco.pdf", "application/pdf", pdfEmBranco);

        assertTrue(extractor.extrair(arquivo).isBlank());
    }
}
