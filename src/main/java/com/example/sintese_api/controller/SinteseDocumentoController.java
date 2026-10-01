package com.example.sintese_api.controller;

import com.example.sintese_api.dto.DocumentoRequest;
import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import com.example.sintese_api.service.DocumentoService;
import com.example.sintese_api.service.SinteseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sinteses/documentos")
@Tag(
        name = "Sínteses",
        description = "Operações relacionadas à geração de sínteses integradas."
)
public class SinteseDocumentoController {

    private final DocumentoService documentoService;
    private final SinteseService sinteseService;

    public SinteseDocumentoController(
            DocumentoService documentoService,
            SinteseService sinteseService
    ) {
        this.documentoService = documentoService;
        this.sinteseService = sinteseService;
    }

    @Operation(
            summary = "Gera uma síntese integrada a partir de arquivos",
            description = """
                    Recebe um ou mais arquivos (TXT, PDF ou DOCX) no campo
                    `documentos`, extrai o texto de cada um e gera uma única
                    síntese integrada em português do Brasil.

                    O limite de palavras é aplicado ao texto extraído, e não
                    ao tamanho do arquivo. Documentos digitalizados (imagens)
                    não são suportados.
                    """
    )
    @ApiResponses({

            @ApiResponse(
                    responseCode = "200",
                    description = "Síntese gerada com sucesso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = SinteseResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Sucesso",
                                    value = """
                                            {
                                              "sintese": "A reciclagem e o consumo consciente contribuem para a redução dos impactos ambientais."
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Nenhum arquivo enviado, arquivo vazio ou quantidade de palavras acima do limite permitido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Arquivo vazio",
                                            value = """
                                                    {
                                                      "detail": "O arquivo 'notas.txt' está vazio.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 400,
                                                      "title": "Arquivo inválido"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Entrada muito grande",
                                            value = """
                                                    {
                                                      "detail": "A quantidade total de palavras dos documentos excede o limite permitido. Quantidade recebida: 72000. Limite: 50000.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 400,
                                                      "title": "Entrada muito grande"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "413",
                    description = "Arquivo ou requisição acima do tamanho máximo permitido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "O arquivo 'artigo.pdf' excede o tamanho máximo de 10 MB.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 413,
                                              "title": "Arquivo muito grande"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "415",
                    description = "Formato de arquivo não suportado (aceitos: .txt, .pdf, .docx)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "O arquivo 'planilha.xlsx' não é suportado. Formatos aceitos: .txt, .pdf, .docx.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 415,
                                              "title": "Tipo de arquivo não suportado"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "422",
                    description = "Arquivo corrompido, protegido por senha ou sem texto extraível",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "O arquivo 'scan.pdf' não contém texto extraível. Documentos digitalizados (imagens) não são suportados.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 422,
                                              "title": "Documento não processável"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "429",
                    description = "Limite de requisições da Gemini atingido"
            ),

            @ApiResponse(
                    responseCode = "502",
                    description = "Erro ao processar a requisição através da Gemini"
            ),

            @ApiResponse(
                    responseCode = "504",
                    description = "A Gemini demorou além do tempo limite configurado"
            )
    })
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SinteseResponse> criarSinteseAPartirDeDocumentos(
            @Parameter(description = "Arquivos TXT, PDF ou DOCX (um ou mais).")
            @RequestParam(value = "documentos", required = false)
            List<MultipartFile> documentos
    ) {

        List<DocumentoRequest> textos =
                documentoService.extrairDocumentos(documentos);

        SinteseResponse response = sinteseService.gerarSintese(
                new SinteseRequest(textos)
        );

        return ResponseEntity.ok(response);
    }
}
