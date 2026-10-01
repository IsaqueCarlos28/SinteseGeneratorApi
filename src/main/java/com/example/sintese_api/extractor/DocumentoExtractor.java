package com.example.sintese_api.extractor;

import org.springframework.web.multipart.MultipartFile;

/**
 * Extrai texto puro de um formato de documento.
 * <p>
 * O {@code DocumentoService} descobre os extractors disponíveis pelo Spring
 * e escolhe o correto por {@link #suporta(String)}, sem conhecer nenhum
 * formato específico.
 */
public interface DocumentoExtractor {

    /**
     * @param contentType tipo já validado pelo serviço (ex.: "application/pdf")
     */
    boolean suporta(String contentType);

    /**
     * @throws com.example.sintese_api.exception.DocumentoNaoProcessavelException
     *         se o arquivo estiver corrompido, protegido ou não puder ser lido
     */
    String extrair(MultipartFile arquivo);
}
