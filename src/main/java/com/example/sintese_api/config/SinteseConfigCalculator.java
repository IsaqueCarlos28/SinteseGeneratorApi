package com.example.sintese_api.config;

import org.springframework.stereotype.Component;

@Component
public class SinteseConfigCalculator {

    public SinteseConfig calcular(int quantidadePalavras) {

        int minBase;
        int maxBase;

        if (quantidadePalavras <= 60) {
            minBase = 15;
            maxBase = 45;

        } else if (quantidadePalavras <= 150) {
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