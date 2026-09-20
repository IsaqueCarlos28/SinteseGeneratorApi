package com.example.sintese_api.client;

import com.example.sintese_api.exception.GeminiException;
import com.example.sintese_api.exception.GeminiRateLimitException;
import com.example.sintese_api.exception.GeminiTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            @Value("${gemini.url}") String url,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .build();

        this.apiKey = apiKey;
        this.model = model;
    }

    public String gerarSintese(
            String systemInstruction,
            String input,
            int maxOutputTokens
    ) {

        GeminiRequest request = new GeminiRequest(
                model,
                systemInstruction,
                input,
                new GenerationConfig(
                        "low",
                        0.1,
                        maxOutputTokens
                ),
                criarResponseFormat()
        );

        try {

            GeminiResponse response = restClient.post()
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Api-Revision", "2026-05-20")
                    .body(request)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 429,
                            (request1, response1) -> {
                                throw new GeminiRateLimitException();
                            }
                    )
                    .onStatus(
                            HttpStatusCode::isError,
                            (request1, response1) -> {
                                throw new GeminiException(
                                        "A Gemini retornou um erro HTTP: "
                                                + response1.getStatusCode()
                                );
                            }
                    )
                    .body(GeminiResponse.class);

            return extrairTexto(response);

        } catch (ResourceAccessException exception) {

            throw new GeminiTimeoutException(
                    "Não foi possível obter resposta da Gemini.",
                    exception
            );

        } catch (GeminiRateLimitException | GeminiException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new GeminiException(
                    "Erro ao processar a resposta da Gemini.",
                    exception
            );
        }
    }

    private ResponseFormat criarResponseFormat() {

        return new ResponseFormat(
                "text",
                "application/json",
                new JsonSchema(
                        "object",
                        new Properties(
                                new SchemaProperty("string")
                        ),
                        new String[]{"sintese"}
                )
        );
    }

    private String extrairTexto(GeminiResponse response) {

        if (response.steps() == null) {
            throw new GeminiException(
                    "A Gemini retornou uma resposta sem steps."
            );
        }

        for (Step step : response.steps()) {

            if (!"model_output".equals(step.type())) {
                continue;
            }

            if (step.content() == null) {
                continue;
            }

            for (Content content : step.content()) {

                if ("text".equals(content.type())
                        && content.text() != null) {

                    return content.text();
                }
            }
        }

        throw new GeminiException(
                "Não foi possível encontrar o texto na resposta da Gemini."
        );
    }

    private record GeminiRequest(
            String model,
            String system_instruction,
            String input,
            GenerationConfig generation_config,
            ResponseFormat response_format
    ) {
    }

    private record GenerationConfig(
            String thinking_level,
            double temperature,
            int max_output_tokens
    ) {
    }

    private record ResponseFormat(
            String type,
            String mime_type,
            JsonSchema schema
    ) {
    }

    private record JsonSchema(
            String type,
            Properties properties,
            String[] required
    ) {
    }

    private record Properties(
            SchemaProperty sintese
    ) {
    }

    private record SchemaProperty(
            String type
    ) {
    }

    private record GeminiResponse(
            String id,
            String status,
            Step[] steps
    ) {
    }

    private record Step(
            String type,
            Content[] content
    ) {
    }

    private record Content(
            String type,
            String text
    ) {
    }
}