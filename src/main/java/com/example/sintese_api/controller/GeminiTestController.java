package com.example.sintese_api.controller;

import com.example.sintese_api.client.GeminiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeminiTestController {

    private final GeminiClient geminiClient;

    public GeminiTestController(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    @GetMapping("/teste-gemini")
    public String testarGemini() {

        return geminiClient.gerarSintese(
                "Você é um assistente que responde de forma curta.",
                "Explique em uma frase o que é uma API REST.",
                100
        );
    }
}
