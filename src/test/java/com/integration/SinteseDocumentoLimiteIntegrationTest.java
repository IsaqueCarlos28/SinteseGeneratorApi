package com.example.sintese_api.integration;

import com.example.sintese_api.client.GeminiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static com.example.sintese_api.support.Arquivos.arquivo;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Limite físico de arquivo reduzido para 1KB, configurado externamente. */
@SpringBootTest(properties = {
        "gemini.api-key=chave-de-teste",
        "app.upload.max-file-size=1KB"
})
@AutoConfigureMockMvc
class SinteseDocumentoLimiteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiClient geminiClient;

    @Test
    void deveRetornar413QuandoArquivoExcederOLimiteConfigurado()
            throws Exception {

        byte[] doisKb = "x".repeat(2048).getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/sinteses/documentos")
                        .file(arquivo("grande.txt", "text/plain", doisKb)))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.title").value("Arquivo muito grande"));

        verifyNoInteractions(geminiClient);
    }
}
