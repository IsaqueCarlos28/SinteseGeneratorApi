package com.example.sintese_api.extractor;

import com.example.sintese_api.exception.DocumentoNaoProcessavelException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class DocxExtractor implements DocumentoExtractor {

    @Override
    public boolean suporta(String contentType) {
        return TipoDocumento.DOCX.contentType().equalsIgnoreCase(contentType);
    }

    @Override
    public String extrair(MultipartFile arquivo) {

        String nome = NomeArquivo.seguro(arquivo.getOriginalFilename());

        try (InputStream entrada = arquivo.getInputStream();
             XWPFDocument documento = new XWPFDocument(entrada);
             XWPFWordExtractor extrator = new XWPFWordExtractor(documento)) {

            return extrator.getText();

        } catch (IOException | RuntimeException exception) {
            // Inclui .doc antigo renomeado para .docx, arquivo corrompido,
            // protegido por senha ou bloqueado pelas proteções contra zip bomb.
            throw new DocumentoNaoProcessavelException(
                    "Não foi possível ler o DOCX '" + nome + "'. "
                            + "Verifique se ele não está corrompido, "
                            + "protegido por senha ou com a extensão trocada.",
                    exception
            );
        }
    }
}
