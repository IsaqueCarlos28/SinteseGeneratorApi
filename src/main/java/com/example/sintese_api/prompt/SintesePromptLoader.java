package com.example.sintese_api.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class SintesePromptLoader {

    private final ObjectMapper objectMapper;

    public SintesePromptLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String carregar() {

        try {
            ClassPathResource resource =
                    new ClassPathResource("prompt/sintese.json");

            JsonNode json =
                    objectMapper.readTree(resource.getInputStream());

            return json.get("system_instruction").asText();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível carregar o prompt de síntese.",
                    e
            );
        }
    }
}