package com.example.sintese_api.controller;

import com.example.sintese_api.dto.DocumentoRequest;
import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import com.example.sintese_api.extractor.DocxExtractor;
import com.example.sintese_api.extractor.PdfExtractor;
import com.example.sintese_api.extractor.TxtExtractor;
import com.example.sintese_api.service.DocumentoService;
import com.example.sintese_api.service.SinteseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.example.sintese_api.support.Arquivos.arquivo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SinteseDocumentoController.class)
@Import({
        DocumentoService.class,
        TxtExtractor.class,
        PdfExtractor.class,
        DocxExtractor.class
})
class SinteseDocumentoControllerTest {

    private static final String URL = "/api/sinteses/documentos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SinteseService sinteseService;

    private byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void deveReutilizarOSinteseServiceComOsTextosExtraidos() throws Exception {

        SinteseRequest esperado = new SinteseRequest(List.of(
                new DocumentoRequest("Primeiro."),
                new DocumentoRequest("Segundo.")
        ));

        when(sinteseService.gerarSintese(esperado))
                .thenReturn(new SinteseResponse("Síntese gerada."));

        mockMvc.perform(multipart(URL)
                        .file(arquivo("a.txt", "text/plain", bytes("Primeiro.")))
                        .file(arquivo("b.txt", "text/plain", bytes("Segundo."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sintese").value("Síntese gerada."));

        verify(sinteseService).gerarSintese(esperado);
    }

    @Test
    void deveRetornar400QuandoNenhumArquivoForEnviado() throws Exception {

        mockMvc.perform(multipart(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Arquivo inválido"))
                .andExpect(jsonPath("$.instance").value(URL));

        verifyNoInteractions(sinteseService);
    }

    @Test
    void deveRetornar400ParaArquivoVazio() throws Exception {

        mockMvc.perform(multipart(URL)
                        .file(arquivo("vazio.txt", "text/plain", new byte[0])))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sinteseService);
    }

    @Test
    void deveRetornar415ParaFormatoNaoSuportado() throws Exception {

        mockMvc.perform(multipart(URL)
                        .file(arquivo("planilha.xlsx",
                                "application/octet-stream", bytes("x"))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title")
                        .value("Tipo de arquivo não suportado"));

        verifyNoInteractions(sinteseService);
    }

    @Test
    void deveRetornar422ParaArquivoCorrompido() throws Exception {

        mockMvc.perform(multipart(URL)
                        .file(arquivo("quebrado.pdf",
                                "application/pdf", bytes("não é pdf"))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.title")
                        .value("Documento não processável"));

        verifyNoInteractions(sinteseService);
    }

    @Test
    void deveRetornar415QuandoEnviarJsonNoEndpointDeDocumentos()
            throws Exception {

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
