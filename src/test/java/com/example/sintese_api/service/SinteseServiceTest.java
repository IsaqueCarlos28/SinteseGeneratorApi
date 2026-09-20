package com.example.sintese_api.service;

import com.example.sintese_api.client.GeminiClient;
import com.example.sintese_api.config.SinteseConfigCalculator;
import com.example.sintese_api.dto.DocumentoRequest;
import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import com.example.sintese_api.exception.SinteseEntradaMuitoGrandeException;
import com.example.sintese_api.prompt.SintesePromptLoader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SinteseServiceTest {

    private final SinteseConfigCalculator configCalculator =
            new SinteseConfigCalculator();

    private final SintesePromptLoader promptLoader =
            Mockito.mock(SintesePromptLoader.class);

    private final GeminiClient geminiClient =
            Mockito.mock(GeminiClient.class);

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void deveGerarSinteseComSucesso() {

        when(promptLoader.carregar())
                .thenReturn("""
                        Gere uma síntese entre {min} e {max} palavras.
                        """);

        when(geminiClient.gerarSintese(
                anyString(),
                anyString(),
                anyInt()
        )).thenReturn("""
                {
                    "sintese": "A reciclagem reduz a quantidade de resíduos."
                }
                """);

        SinteseService service = criarService(50000);

        SinteseRequest request = new SinteseRequest(
                List.of(
                        new DocumentoRequest(
                                "A reciclagem reduz a quantidade de resíduos."
                        )
                )
        );

        SinteseResponse response =
                service.gerarSintese(request);

        assertEquals(
                "A reciclagem reduz a quantidade de resíduos.",
                response.sintese()
        );

        verify(promptLoader).carregar();

        verify(geminiClient).gerarSintese(
                contains("1"),
                contains("<documento>"),
                eq(100)
        );
    }

    @Test
    void deveAceitarMaisDeUmDocumento() {

        when(promptLoader.carregar())
                .thenReturn("""
                        Gere uma síntese entre {min} e {max} palavras.
                        """);

        when(geminiClient.gerarSintese(
                anyString(),
                anyString(),
                anyInt()
        )).thenReturn("""
                {
                    "sintese": "Síntese dos documentos."
                }
                """);

        SinteseService service = criarService(50000);

        SinteseRequest request = new SinteseRequest(
                List.of(
                        new DocumentoRequest("Primeiro documento."),
                        new DocumentoRequest("Segundo documento.")
                )
        );

        SinteseResponse response =
                service.gerarSintese(request);

        assertEquals(
                "Síntese dos documentos.",
                response.sintese()
        );

        verify(geminiClient).gerarSintese(
                anyString(),
                contains("<documento>"),
                anyInt()
        );
    }

    @Test
    void deveLancarExcecaoQuandoEntradaExcederLimite() {

        SinteseService service = criarService(5);

        SinteseRequest request = new SinteseRequest(
                List.of(
                        new DocumentoRequest(
                                "uma duas tres quatro cinco seis"
                        )
                )
        );

        assertThrows(
                SinteseEntradaMuitoGrandeException.class,
                () -> service.gerarSintese(request)
        );

        verifyNoInteractions(geminiClient);
    }

    @Test
    void deveInterpretarRespostaJsonDaGemini() {

        when(promptLoader.carregar())
                .thenReturn("""
                        Gere uma síntese entre {min} e {max} palavras.
                        """);

        when(geminiClient.gerarSintese(
                anyString(),
                anyString(),
                anyInt()
        )).thenReturn("""
                {
                    "sintese": "Resultado processado corretamente."
                }
                """);

        SinteseService service = criarService(50000);

        SinteseRequest request = new SinteseRequest(
                List.of(
                        new DocumentoRequest("Documento de teste.")
                )
        );

        SinteseResponse response =
                service.gerarSintese(request);

        assertEquals(
                "Resultado processado corretamente.",
                response.sintese()
        );
    }

    private SinteseService criarService(int limite) {

        SinteseService service = new SinteseService(
                configCalculator,
                promptLoader,
                geminiClient,
                objectMapper
        );

        try {
            var campo =
                    SinteseService.class
                            .getDeclaredField("maxPalavrasEntrada");

            campo.setAccessible(true);
            campo.set(service, limite);

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return service;
    }
}