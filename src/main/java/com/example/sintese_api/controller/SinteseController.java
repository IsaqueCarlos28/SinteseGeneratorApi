package com.example.sintese_api.controller;

import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import com.example.sintese_api.service.SinteseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sinteses")
@Tag(
        name = "Sínteses",
        description = "Operações relacionadas à geração de sínteses integradas."
)
public class SinteseController {

    private final SinteseService sinteseService;

    public SinteseController(SinteseService sinteseService) {
        this.sinteseService = sinteseService;
    }

    @Operation(
            summary = "Gera uma síntese integrada",
            description = """
                    Recebe um ou mais documentos e utiliza inteligência artificial
                    para gerar uma única síntese integrada em português do Brasil.

                    A quantidade total de palavras dos documentos não pode
                    ultrapassar o limite configurado pela aplicação.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Documentos que serão utilizados para gerar a síntese integrada.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = SinteseRequest.class
                    ),
                    examples = @ExampleObject(
                            name = "Exemplo de requisição",
                            value = """
                                    {
                                      "documentos": [
                                        {
                                          "conteudo": "A reciclagem transforma materiais descartados em novos produtos, reduzindo a necessidade de extrair matéria-prima virgem da natureza."
                                        },
                                        {
                                          "conteudo": "O consumo consciente envolve escolhas de compra que consideram os impactos ambientais e sociais dos produtos adquiridos."
                                        }
                                      ]
                                    }
                                    """
                    )
            )
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
                                              "sintese": "A reciclagem e o consumo consciente contribuem para a redução dos impactos ambientais, incentivando o reaproveitamento de materiais e escolhas de consumo mais sustentáveis."
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Dados da requisição inválidos ou quantidade de palavras acima do limite permitido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Documento vazio",
                                            value = """
                                                    {
                                                      "detail": "O conteúdo do documento não pode ser vazio",
                                                      "status": 400,
                                                      "title": "Requisição inválida"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Lista de documentos vazia",
                                            value = """
                                                    {
                                                      "detail": "A lista de documentos não pode ser vazia",
                                                      "status": 400,
                                                      "title": "Requisição inválida"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "JSON inválido",
                                            value = """
                                                    {
                                                      "detail": "O corpo da requisição não possui um formato JSON válido.",
                                                      "status": 400,
                                                      "title": "JSON inválido"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Entrada muito grande",
                                            value = """
                                                    {
                                                      "detail": "A quantidade total de palavras dos documentos excede o limite permitido. Quantidade recebida: 50001. Limite: 50000.",
                                                      "status": 400,
                                                      "title": "Entrada muito grande"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "Recurso solicitado não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "O recurso solicitado não foi encontrado.",
                                              "status": 404,
                                              "title": "Recurso não encontrado"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "405",
                    description = "Método HTTP não permitido para o recurso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "O método HTTP utilizado não é permitido para este recurso.",
                                              "status": 405,
                                              "title": "Método não permitido"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "415",
                    description = "Tipo de conteúdo enviado não é suportado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "O tipo de conteúdo enviado não é suportado pela API.",
                                              "status": 415,
                                              "title": "Tipo de conteúdo não suportado"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "429",
                    description = "Limite de requisições da Gemini atingido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "A quantidade de requisições à Gemini excedeu o limite permitido.",
                                              "instance": "/api/sinteses",
                                              "status": 429,
                                              "title": "Limite de requisições atingido"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "500",
                    description = "Erro inesperado durante o processamento da requisição",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "Ocorreu um erro inesperado ao processar a requisição.",
                                              "status": 500,
                                              "title": "Erro interno do servidor"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "502",
                    description = "Erro ao processar a requisição através da Gemini",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "Não foi possível processar a síntese através da Gemini.",
                                              "instance": "/api/sinteses",
                                              "status": 502,
                                              "title": "Erro na Gemini"
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "504",
                    description = "A Gemini demorou além do tempo limite configurado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "detail": "A Gemini demorou demais para responder.",
                                              "instance": "/api/sinteses",
                                              "status": 504,
                                              "title": "Timeout na Gemini"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SinteseResponse> criarSintese(
            @Valid @RequestBody SinteseRequest request
    ) {

        SinteseResponse response =
                sinteseService.gerarSintese(request);

        return ResponseEntity.ok(response);
    }
}