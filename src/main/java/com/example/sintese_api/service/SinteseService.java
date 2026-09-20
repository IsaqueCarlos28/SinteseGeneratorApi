package com.example.sintese_api.service;

import com.example.sintese_api.config.SinteseConfig;
import com.example.sintese_api.config.SinteseConfigCalculator;
import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import com.example.sintese_api.prompt.SintesePromptLoader;
import org.springframework.stereotype.Service;

@Service
public class SinteseService {

    private final SinteseConfigCalculator configCalculator;
    private final SintesePromptLoader promptLoader;

    public SinteseService(
            SinteseConfigCalculator configCalculator,
            SintesePromptLoader promptLoader
    ) {
        this.configCalculator = configCalculator;
        this.promptLoader = promptLoader;
    }

    public SinteseResponse gerarSintese(SinteseRequest request) {

        int quantidadePalavras = request.documentos()
                .stream()
                .mapToInt(documento ->
                        contarPalavras(documento.conteudo())
                )
                .sum();

        SinteseConfig config =
                configCalculator.calcular(quantidadePalavras);

        String prompt = promptLoader.carregar();

        prompt = prompt
                .replace("{min}", String.valueOf(config.minPalavras()))
                .replace("{max}", String.valueOf(config.maxPalavras()));

        String documentos = request.documentos()
                .stream()
                .map(documento ->
                        "<documento>\n"
                                + documento.conteudo()
                                + "\n</documento>"
                )
                .reduce((documento1, documento2) ->
                        documento1 + "\n\n" + documento2
                )
                .orElse("");

        String input = documentos;

        return new SinteseResponse(
                "Prompt:\n\n"
                        + prompt
                        + "\n\nInput:\n\n"
                        + input
        );
    }

    private int contarPalavras(String texto) {

        if (texto == null || texto.isBlank()) {
            return 0;
        }

        return texto.trim().split("\\s+").length;
    }
}