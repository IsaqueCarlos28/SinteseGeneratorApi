package com.example.sintese_api.client;

import java.time.Duration;

import com.example.sintese_api.exception.GeminiException;
import com.example.sintese_api.exception.GeminiIndisponivelException;
import com.example.sintese_api.exception.GeminiRateLimitException;
import com.example.sintese_api.exception.GeminiTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String apiRevision;
    private final String thinkingLevel;
    private final double temperature;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GeminiClient.class);

    @Value("${gemini.max-retries:2}")
    private int maxRetries;

    @Value("${gemini.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    public GeminiClient(
            @Value("${gemini.url}") String url,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.api-revision}") String apiRevision,
            @Value("${gemini.timeout-seconds}") int timeoutSeconds,
            @Value("${gemini.thinking-level}") String thinkingLevel,
            @Value("${gemini.temperature}") double temperature
    ) {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory();

        requestFactory.setReadTimeout(
                Duration.ofSeconds(timeoutSeconds)
        );

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build();

        this.apiKey = apiKey;
        this.model = model;
        this.apiRevision = apiRevision;
        this.thinkingLevel = thinkingLevel;
        this.temperature = temperature;
    }

    public String gerarSintese(String systemInstruction, String input, int maxOutputTokens) {

        GeminiRequest request = new GeminiRequest(
                model,
                systemInstruction,
                input,
                new GenerationConfig(thinkingLevel, temperature, maxOutputTokens),
                criarResponseFormat()
        );

        int tentativa = 0;

        while (true) {
            try {
                return chamar(request);
            } catch (GeminiIndisponivelException exception) {

                if (tentativa >= maxRetries) {
                    throw exception;
                }

                long espera = retryBackoffMs * (1L << tentativa);
                tentativa++;

                log.warn("Gemini indisponível (503). Tentativa {}/{} falhou; nova tentativa em {} ms.",
                        tentativa, maxRetries + 1, espera);

                dormir(espera);
            }
        }
    }

    private String chamar(GeminiRequest request) {
        try {

            GeminiResponse response = restClient.post()
                    .header("x-goog-api-key", apiKey)
                    .header("Api-Revision", apiRevision)
                    .body(request)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 429,
                            (req, res) -> { throw new GeminiRateLimitException(); }
                    )
                    .onStatus(
                            status -> status.value() == 503,
                            (req, res) -> {
                                throw new GeminiIndisponivelException(
                                        "A Gemini está indisponível (503).",
                                        503,
                                        lerCorpo(res)
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> {
                                throw new GeminiException(
                                        "A Gemini retornou um erro HTTP: " + res.getStatusCode(),
                                        res.getStatusCode().value(),
                                        lerCorpo(res)
                                );
                            }
                    )
                    .body(GeminiResponse.class);

            return extrairTexto(response);

        } catch (ResourceAccessException exception) {
            throw new GeminiTimeoutException("Não foi possível obter resposta da Gemini.", exception);
        } catch (GeminiRateLimitException | GeminiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GeminiException("Erro ao processar a resposta da Gemini.", exception);
        }
    }

    private String lerCorpo(org.springframework.http.client.ClientHttpResponse res) throws java.io.IOException {
        return new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Interrompido durante nova tentativa.", e);
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