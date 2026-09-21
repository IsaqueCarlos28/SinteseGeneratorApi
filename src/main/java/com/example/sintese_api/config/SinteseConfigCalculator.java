package com.example.sintese_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SinteseConfigCalculator {

    @Value("${sintese.min-output-tokens-limit}")
    private int minOutputTokensLimit;

    public SinteseConfig calcular(int quantidadePalavras) {

        /*
         * Textos muito pequenos:
         * não faz sentido obrigar a IA a reduzir o conteúdo.
         */

        if (quantidadePalavras <= 60) {

            int maxOutputTokens = Math.max(
                    minOutputTokensLimit,
                    (int) Math.ceil(quantidadePalavras * 1.6) + 20
            );

            return new SinteseConfig(
                    1,
                    quantidadePalavras,
                    maxOutputTokens
            );
        }

        int minBase;
        int maxBase;

        if (quantidadePalavras <= 150) {

            minBase = 30;
            maxBase = 85;

        } else if (quantidadePalavras <= 400) {

            minBase = 60;
            maxBase = 180;

        } else if (quantidadePalavras <= 800) {

            minBase = 120;
            maxBase = 280;

        } else if (quantidadePalavras <= 1500) {

            minBase = 180;
            maxBase = 420;

        } else {

            minBase = 250;
            maxBase = 500;
        }

        int maxFinal = Math.min(
                maxBase,
                (int) Math.floor(quantidadePalavras * 0.8)
        );

        int minFinal = Math.min(
                minBase,
                maxFinal
        );

        int maxOutputTokens = (int) Math.ceil(
                maxFinal * 1.6
        ) + 20;

        return new SinteseConfig(
                minFinal,
                maxFinal,
                maxOutputTokens
        );
    }
}