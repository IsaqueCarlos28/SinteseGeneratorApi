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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sinteses")
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
                    mediaType = "application/json",
                    schema = @Schema(implementation = SinteseRequest.class),
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
                            mediaType = "application/json",
                            schema = @Schema(implementation = SinteseResponse.class),
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
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Documento vazio",
                                            value = """
                                                    {
                                                      "detail": "O conteúdo do documento não pode ser vazio",
                                                      "status": 400,
                                                      "title": "Dados inválidos"
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
                    responseCode = "429",
                    description = "Limite de requisições da Gemini atingido",
                    content = @Content(
                            mediaType = "application/json",
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
                    responseCode = "502",
                    description = "Erro ao processar a requisição através da Gemini",
                    content = @Content(
                            mediaType = "application/json",
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
                            mediaType = "application/json",
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
    @PostMapping
    public ResponseEntity<SinteseResponse> criarSintese(
            @Valid @RequestBody SinteseRequest request
    ) {
        SinteseResponse response =
                sinteseService.gerarSintese(request);

        return ResponseEntity.ok(response);
    }
}
