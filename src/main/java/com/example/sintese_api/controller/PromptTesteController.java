package com.example.sintese_api.controller;

import com.example.sintese_api.prompt.SintesePromptLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromptTesteController {

    private final SintesePromptLoader promptLoader;

    public PromptTesteController(SintesePromptLoader promptLoader) {
        this.promptLoader = promptLoader;
    }

    @GetMapping("/teste-prompt")
    public String testarPrompt() {
        return promptLoader.carregar();
    }
}
