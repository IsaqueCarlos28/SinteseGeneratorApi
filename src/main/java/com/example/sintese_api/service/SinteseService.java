package com.example.sintese_api.service;

import com.example.sintese_api.config.SinteseConfig;
import com.example.sintese_api.config.SinteseConfigCalculator;
import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import org.springframework.stereotype.Service;

@Service
public class SinteseService {

    private final SinteseConfigCalculator configCalculator;

    public SinteseService(
            SinteseConfigCalculator configCalculator
    ) {
        this.configCalculator = configCalculator;
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

        return new SinteseResponse(
                "Palavras: " + quantidadePalavras
                        + ", mínimo: " + config.minPalavras()
                        + ", máximo: " + config.maxPalavras()
                        + ", tokens: " + config.maxOutputTokens()
        );
    }

    private int contarPalavras(String texto) {

        if (texto == null || texto.isBlank()) {
            return 0;
        }

        return texto.trim().split("\\s+").length;
    }
}
