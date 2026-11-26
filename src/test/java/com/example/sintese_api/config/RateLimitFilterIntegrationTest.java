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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "gemini.api-key=chave-de-teste",
        "app.rate-limit.enabled=true",
        "app.rate-limit.max-requests=2",
        "app.rate-limit.window-seconds=60",
        "app.rate-limit.trusted-proxies="
})
@AutoConfigureMockMvc
class RateLimitFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiClient geminiClient;

    @Test
    void deveAplicarRateLimitNaCadeiaHttpReal() throws Exception {
        when(geminiClient.gerarSintese(anyString(), anyString(), anyInt()))
                .thenReturn("""
                        {"sintese":"Resumo de teste."}
                        """);

        String corpo = """
                {"documentos":[{"conteudo":"Texto de teste para rate limit."}]}
                """;

        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.10.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.10.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.10.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesRegex("[1-9]\\d*")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Limite de requisições atingido"));
    }

    @Test
    void clientesDiferentesDevemTerContadoresIndependentesNaCadeiaHttp() throws Exception {
        when(geminiClient.gerarSintese(anyString(), anyString(), anyInt()))
                .thenReturn("""
                        {"sintese":"Resumo de teste."}
                        """);

        String corpo = """
                {"documentos":[{"conteudo":"Texto de teste para rate limit."}]}
                """;

        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.20.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.20.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.20.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.20.0.2"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
    }

    @Test
    void xForwardedForNaoDevePermitirSpoofQuandoNaoHaProxyConfiavel() throws Exception {
        when(geminiClient.gerarSintese(anyString(), anyString(), anyInt()))
                .thenReturn("""
                        {"sintese":"Resumo de teste."}
                        """);

        String corpo = """
                {"documentos":[{"conteudo":"Texto de teste para rate limit."}]}
                """;

        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.30.0.1"); request.addHeader("X-Forwarded-For", "203.0.113.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.30.0.1"); request.addHeader("X-Forwarded-For", "203.0.113.2"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/sinteses")
                        .with(request -> { request.setRemoteAddr("10.30.0.1"); request.addHeader("X-Forwarded-For", "203.0.113.3"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isTooManyRequests());
    }
}
