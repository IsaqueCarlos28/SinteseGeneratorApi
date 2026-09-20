package com.example.sintese_api.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SinteseConfigCalculatorTest {

    private final SinteseConfigCalculator calculator =
            new SinteseConfigCalculator();

    @Test
    void deveCalcularLimitesParaAte60Palavras() {

        SinteseConfig config = calculator.calcular(50);

        assertEquals(15, config.minPalavras());
        assertEquals(40, config.maxPalavras());
        assertEquals(84, config.maxOutputTokens());
    }

    @Test
    void deveCalcularLimitesPara61A150Palavras() {

        SinteseConfig config = calculator.calcular(100);

        assertEquals(30, config.minPalavras());
        assertEquals(80, config.maxPalavras());
        assertEquals(148, config.maxOutputTokens());
    }

    @Test
    void deveCalcularLimitesPara151A400Palavras() {

        SinteseConfig config = calculator.calcular(200);

        assertEquals(60, config.minPalavras());
        assertEquals(160, config.maxPalavras());
        assertEquals(276, config.maxOutputTokens());
    }

    @Test
    void deveCalcularLimitesPara401A800Palavras() {

        SinteseConfig config = calculator.calcular(500);

        assertEquals(120, config.minPalavras());
        assertEquals(280, config.maxPalavras());
        assertEquals(468, config.maxOutputTokens());
    }

    @Test
    void deveCalcularLimitesPara801A1500Palavras() {

        SinteseConfig config = calculator.calcular(1000);

        assertEquals(180, config.minPalavras());
        assertEquals(420, config.maxPalavras());
        assertEquals(692, config.maxOutputTokens());
    }

    @Test
    void deveCalcularLimitesParaMaisDe1500Palavras() {

        SinteseConfig config = calculator.calcular(2000);

        assertEquals(250, config.minPalavras());
        assertEquals(500, config.maxPalavras());
        assertEquals(820, config.maxOutputTokens());
    }

    @ParameterizedTest
    @CsvSource({
            "60, 15, 45",
            "61, 30, 48",
            "150, 30, 85",
            "151, 60, 120",
            "400, 60, 180",
            "401, 120, 280",
            "800, 120, 280",
            "801, 180, 420",
            "1500, 180, 420",
            "1501, 250, 500"
    })
    void deveRespeitarAsFronteirasDasFaixas(
            int palavras,
            int minEsperado,
            int maxEsperado
    ) {

        SinteseConfig config = calculator.calcular(palavras);

        assertEquals(minEsperado, config.minPalavras());
        assertEquals(maxEsperado, config.maxPalavras());
    }
}