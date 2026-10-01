package com.example.sintese_api.controller;

import com.example.sintese_api.dto.DocumentoRequest;
import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import com.example.sintese_api.service.DocumentoService;
import com.example.sintese_api.service.SinteseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
                    Recebe um ou mais arquivos, extrai o texto de cada um e utiliza
                    inteligência artificial para gerar uma única síntese integrada
                    em português do Brasil.

                    **Requisição**

                    Envie os arquivos utilizando `multipart/form-data`.

                    O campo `documentos` deve ser repetido para cada arquivo enviado.

                    **Formatos aceitos**

                    - `.txt`
                    - `.pdf`
                    - `.docx`

                    **Limites**

                    - Cada arquivo pode ter até **10 MB**.
                    - A requisição inteira pode ter até **30 MB**.
                    - A soma do texto extraído de todos os documentos não pode
                      ultrapassar **50.000 palavras**.

                    O limite de palavras é aplicado depois da extração do conteúdo.

                    **Processamento**

                    A extensão do arquivo determina o formato esperado.
                    Quando o `Content-Type` é informado, ele precisa ser compatível
                    com a extensão do arquivo.

                    Caso o `Content-Type` seja ausente ou
                    `application/octet-stream`, a aplicação permite o processamento
                    e utiliza o parser correspondente ao formato.

                    **Documentos não processáveis**

                    PDFs ou DOCX corrompidos ou protegidos por senha são rejeitados.

                    Documentos digitalizados que contenham somente imagens também
                    não são suportados, pois a aplicação atualmente não possui OCR.

                    **Exemplo com cURL**

                    ```
                    curl -X POST http://localhost:8080/api/sinteses/documentos \\
                      -F "documentos=@artigo.pdf" \\
                      -F "documentos=@notas.docx"
                    ```

                    Os documentos são combinados e utilizados para gerar uma única
                    síntese integrada.
                    """
    )
    @ApiResponses({

            @ApiResponse(
                    responseCode = "200",
                    description = "Síntese gerada com sucesso.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = SinteseResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Sucesso",
                                    value = """
                                            {
                                              "sintese": "A reciclagem e o consumo consciente contribuem para a redução dos impactos ambientais, incentivando o reaproveitamento de materiais e escolhas de consumo mais sustentáveis."
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Arquivo ou requisição inválida. Pode ocorrer quando:
                            - nenhum arquivo é enviado;
                            - um arquivo está vazio;
                            - a quantidade total de palavras ultrapassa 50.000.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Nenhum arquivo enviado",
                                            value = """
                                                    {
                                                      "detail": "Envie ao menos um arquivo no campo 'documentos'.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 400,
                                                      "title": "Arquivo inválido"
                                                    }
                                                    """
                                    ),
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
                    description = """
                            O tamanho do arquivo ou da requisição excede o limite permitido.

                            Limites atuais:
                            - 10 MB por arquivo;
                            - 30 MB por requisição.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Arquivo acima de 10 MB",
                                            value = """
                                                    {
                                                      "detail": "O arquivo 'artigo.pdf' excede o tamanho máximo de 10 MB.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 413,
                                                      "title": "Arquivo muito grande"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Requisição acima de 30 MB",
                                            value = """
                                                    {
                                                      "detail": "O arquivo ou a requisição excede o tamanho máximo permitido.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 413,
                                                      "title": "Arquivo muito grande"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "415",
                    description = """
                            Tipo de arquivo ou conteúdo não suportado.

                            Ocorre quando:
                            - a extensão não é `.txt`, `.pdf` ou `.docx`;
                            - o Content-Type declarado não corresponde à extensão;
                            - a requisição não utiliza um tipo de conteúdo suportado.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Extensão não suportada",
                                            value = """
                                                    {
                                                      "detail": "O arquivo 'planilha.xlsx' não é suportado. Formatos aceitos: .txt, .pdf, .docx.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 415,
                                                      "title": "Tipo de arquivo não suportado"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Content-Type incompatível",
                                            value = """
                                                    {
                                                      "detail": "O tipo de conteúdo 'image/png' do arquivo 'relatorio.pdf' não corresponde à extensão .pdf.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 415,
                                                      "title": "Tipo de arquivo não suportado"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Requisição que não é multipart",
                                            value = """
                                                    {
                                                      "detail": "O tipo de conteúdo enviado não é suportado pela API.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 415,
                                                      "title": "Tipo de conteúdo não suportado"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "422",
                    description = """
                            O arquivo possui um formato reconhecido, mas não pôde
                            ser processado.

                            Pode ocorrer quando:
                            - o arquivo está corrompido;
                            - o arquivo está protegido por senha;
                            - a extensão não corresponde ao conteúdo real;
                            - o documento não possui texto extraível;
                            - um TXT não possui conteúdo textual válido.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Documento digitalizado",
                                            value = """
                                                    {
                                                      "detail": "O arquivo 'scan.pdf' não contém texto extraível. Documentos digitalizados (imagens) não são suportados.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 422,
                                                      "title": "Documento não processável"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "PDF corrompido ou protegido",
                                            value = """
                                                    {
                                                      "detail": "Não foi possível ler o PDF 'contrato.pdf'. Verifique se ele não está corrompido ou protegido por senha.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 422,
                                                      "title": "Documento não processável"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "DOCX não processável",
                                            value = """
                                                    {
                                                      "detail": "Não foi possível ler o DOCX 'contrato.docx'. Verifique se ele não está corrompido, protegido por senha ou com a extensão trocada.",
                                                      "instance": "/api/sinteses/documentos",
                                                      "status": 422,
                                                      "title": "Documento não processável"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "429",
                    description = "A Gemini informou que o limite de requisições foi atingido.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "A quantidade de requisições à Gemini excedeu o limite permitido.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 429,
                                              "title": "Limite de requisições atingido"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Erro inesperado durante o processamento da requisição.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "Ocorreu um erro inesperado ao processar a requisição.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 500,
                                              "title": "Erro interno do servidor"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "502",
                    description = "Erro durante a comunicação ou processamento da resposta da Gemini.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "Não foi possível processar a síntese através da Gemini.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 502,
                                              "title": "Erro na Gemini"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "503",
                    description = """
                            A Gemini permaneceu indisponível após as tentativas
                            automáticas realizadas pela API.
                            """,
                    headers = @Header(
                            name = "Retry-After",
                            description = "Tempo sugerido de espera, em segundos, antes de uma nova tentativa.",
                            schema = @Schema(
                                    type = "integer",
                                    example = "30"
                            )
                    ),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "A Gemini está com alta demanda no momento. Tente novamente em instantes.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 503,
                                              "title": "Gemini indisponível"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "504",
                    description = "A Gemini não respondeu dentro do tempo limite configurado.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "A Gemini demorou demais para responder.",
                                              "instance": "/api/sinteses/documentos",
                                              "status": 504,
                                              "title": "Timeout na Gemini"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SinteseResponse> criarSinteseAPartirDeDocumentos(
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