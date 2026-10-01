package com.example.sintese_api.extractor;

import com.example.sintese_api.exception.DocumentoNaoProcessavelException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class PdfExtractor implements DocumentoExtractor {

    @Override
    public boolean suporta(String contentType) {
        return TipoDocumento.PDF.contentType().equalsIgnoreCase(contentType);
    }

    @Override
    public String extrair(MultipartFile arquivo) {

        String nome = NomeArquivo.seguro(arquivo.getOriginalFilename());

        try (PDDocument documento = Loader.loadPDF(arquivo.getBytes())) {

            return new PDFTextStripper().getText(documento);

        } catch (IOException | RuntimeException exception) {
            // PDF corrompido, protegido por senha, sem permissão de extração...
            throw new DocumentoNaoProcessavelException(
                    "Não foi possível ler o PDF '" + nome + "'. "
                            + "Verifique se ele não está corrompido "
                            + "ou protegido por senha.",
                    exception
            );
        }
    }
}
