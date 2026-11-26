package com.example.sintese_api.service;

import com.example.sintese_api.dto.DocumentoRequest;
import com.example.sintese_api.exception.ArquivoInvalidoException;
import com.example.sintese_api.exception.ArquivoMuitoGrandeException;
import com.example.sintese_api.exception.DocumentoNaoProcessavelException;
import com.example.sintese_api.exception.TipoArquivoNaoSuportadoException;
import com.example.sintese_api.extractor.DocumentoExtractor;
import com.example.sintese_api.extractor.NomeArquivo;
import com.example.sintese_api.extractor.TipoDocumento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

/**
 * Transforma arquivos enviados em {@link DocumentoRequest}, o mesmo modelo
 * usado pelo fluxo textual (JSON). Não conhece nenhum formato específico:
 * a leitura ? delegada aos {@link DocumentoExtractor}.
 * <p>
 * Etapas (cada uma isolada em um m?todo, o que facilita mover o processamento
 * para uma fila no futuro): validar arquivo ? escolher extractor ? extrair ?
 * validar e normalizar conteúdo.
 * <p>
 * O limite de palavras N?O ? aplicado aqui: ele continua no
 * {@code SinteseService}, sobre o texto j? extra?do.
 */
@Service
public class DocumentoService {

    private static final String OCTET_STREAM = "application/octet-stream";

    private final List<DocumentoExtractor> extractors;
    private final DataSize tamanhoMaximo;
    private final DocumentoTextoNormalizador normalizador;

    public DocumentoService(
            List<DocumentoExtractor> extractors,
            @Value("${app.upload.max-file-size}") DataSize tamanhoMaximo,
            DocumentoTextoNormalizador normalizador
    ) {
        this.extractors = extractors;
        this.tamanhoMaximo = tamanhoMaximo;
        this.normalizador = normalizador;
    }

    public List<DocumentoRequest> extrairDocumentos(
            List<MultipartFile> arquivos
    ) {

        if (arquivos == null || arquivos.isEmpty()) {
            throw new ArquivoInvalidoException(
                    "Envie ao menos um arquivo no campo 'documentos'."
            );
        }

        return arquivos.stream()
                .map(this::extrairDocumento)
                .toList();
    }

    DocumentoRequest extrairDocumento(MultipartFile arquivo) {

        String nome = NomeArquivo.seguro(arquivo.getOriginalFilename());

        validarArquivo(arquivo, nome);

        TipoDocumento tipo = identificarTipo(arquivo, nome);

        DocumentoExtractor extractor = extractors.stream()
                .filter(candidato -> candidato.suporta(tipo.contentType()))
                .findFirst()
                .orElseThrow(() -> new TipoArquivoNaoSuportadoException(
                        "O arquivo '" + nome + "' não é suportado. "
                                + "Formatos aceitos: "
                                + TipoDocumento.extensoesAceitas() + "."
                ));

        String texto = extrair(extractor, arquivo, nome);

        return new DocumentoRequest(validarENormalizar(texto, nome));
    }

    private void validarArquivo(MultipartFile arquivo, String nome) {

        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException(
                    "O arquivo '" + nome + "' est? vazio."
            );
        }

        if (arquivo.getSize() > tamanhoMaximo.toBytes()) {
            throw new ArquivoMuitoGrandeException(
                    "O arquivo '" + nome + "' excede o tamanho m?ximo de "
                            + tamanhoMaximo.toMegabytes() + " MB."
            );
        }
    }

    /**
     * A extensão escolhe o formato, e o Content-Type declarado precisa ser
     * compat?vel com ele. Se o tipo declarado for gen?rico
     * (application/octet-stream) ou ausente, ? aceito, porque muitos
     * clientes HTTP não o preenchem. A confirmação final de que o conteúdo
     * ? mesmo daquele formato vem do parser (que falha em arquivo trocado).
     */
    private TipoDocumento identificarTipo(MultipartFile arquivo, String nome) {

        TipoDocumento tipo = TipoDocumento
                .porExtensao(NomeArquivo.extensao(nome))
                .orElseThrow(() -> tipoNaoSuportado(nome));

        String declarado = normalizarContentType(arquivo.getContentType());

        boolean compativel = declarado.isEmpty()
                || declarado.equals(OCTET_STREAM)
                || declarado.equals(tipo.contentType());

        if (!compativel) {
            throw new TipoArquivoNaoSuportadoException(
                    "O tipo de conteúdo '" + declarado + "' do arquivo '"
                            + nome + "' não corresponde à extensão ."
                            + tipo.extensao() + "."
            );
        }

        return tipo;
    }

    private TipoArquivoNaoSuportadoException tipoNaoSuportado(String nome) {
        return new TipoArquivoNaoSuportadoException(
                "O arquivo '" + nome + "' não é suportado. "
                        + "Formatos aceitos: "
                        + TipoDocumento.extensoesAceitas() + "."
        );
    }

    private String extrair(
            DocumentoExtractor extractor,
            MultipartFile arquivo,
            String nome
    ) {
        try {
            return extractor.extrair(arquivo);
        } catch (DocumentoNaoProcessavelException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DocumentoNaoProcessavelException(
                    "Não foi possível processar o arquivo '" + nome + "'.",
                    exception
            );
        }
    }

    private String validarENormalizar(String texto, String nome) {

        String normalizado = normalizador.normalizar(texto);

        if (normalizado.isBlank()) {
            throw new DocumentoNaoProcessavelException(
                    "O arquivo '" + nome + "' não contém texto extraível. "
                            + "Documentos digitalizados (imagens) não são "
                            + "suportados."
            );
        }

        return normalizado;
    }

    private String normalizarContentType(String contentType) {

        if (contentType == null) {
            return "";
        }

        int parametros = contentType.indexOf(';');
        String base = parametros >= 0
                ? contentType.substring(0, parametros)
                : contentType;

        return base.trim().toLowerCase(Locale.ROOT);
    }
}
