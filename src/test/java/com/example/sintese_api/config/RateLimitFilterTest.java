package com.example.sintese_api.config;

import com.example.sintese_api.client.GeminiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "gemini.api-key=chave-de-teste",
        "app.rate-limit.enabled=true",
        "app.rate-limit.max-requests=2",
        "app.rate-limit.window-seconds=60"
})
@AutoConfigureMockMvc
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiClient geminiClient;

    @Test
    void deveRetornar429QuandoLimitePorIpForExcedido() throws Exception {

        when(geminiClient.gerarSintese(anyString(), anyString(), anyInt()))
                .thenReturn("""
                        {"sintese":"Resumo de teste."}
                        """);

        String corpo = """
                {"documentos":[{"conteudo":"Texto de teste para rate limit."}]}
                """;

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/sinteses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/sinteses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", containsString("")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Limite de requisições atingido"))
                .andExpect(jsonPath("$.detail").exists());
    }
}
