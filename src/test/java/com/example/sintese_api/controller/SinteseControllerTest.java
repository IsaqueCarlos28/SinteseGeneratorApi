package com.example.sintese_api.controller;

import com.example.sintese_api.service.SinteseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SinteseController.class)
class SinteseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SinteseService sinteseService;

    @Test
    void deveRetornar400QuandoListaDeDocumentosEstiverVazia()
            throws Exception {

        mockMvc.perform(
                post("/api/sinteses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "documentos": []
                                }
                                """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoDocumentoEstiverVazio()
            throws Exception {

        mockMvc.perform(
                post("/api/sinteses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "documentos": [
                                        {
                                            "conteudo": ""
                                        }
                                    ]
                                }
                                """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoDocumentoEstiverEmBranco()
            throws Exception {

        mockMvc.perform(
                post("/api/sinteses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "documentos": [
                                        {
                                            "conteudo": "   "
                                        }
                                    ]
                                }
                                """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoJsonForInvalido()
            throws Exception {

        mockMvc.perform(
                post("/api/sinteses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "documentos": [
                                        {
                                            "conteudo": "texto"
                                    }
                                }
                                """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404QuandoRecursoNaoExiste() throws Exception {

        mockMvc.perform(
                        get("/api/recurso-que-nao-existe")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail")
                        .value("O recurso solicitado não foi encontrado."))
                .andExpect(jsonPath("$.instance")
                        .value("/api/recurso-que-nao-existe"));
    }

    @Test
    void deveRetornar405QuandoMetodoNaoForPermitido() throws Exception {

        mockMvc.perform(
                        get("/api/sinteses")
                )
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.title").value("Método não permitido"))
                .andExpect(jsonPath("$.detail")
                        .value("O método HTTP utilizado não é permitido para este recurso."))
                .andExpect(jsonPath("$.instance")
                        .value("/api/sinteses"));
    }

    @Test
    void deveRetornar415QuandoTipoDeConteudoNaoForSuportado() throws Exception {

        mockMvc.perform(
                        post("/api/sinteses")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("texto simples")
                )
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.title")
                        .value("Tipo de conteúdo não suportado"))
                .andExpect(jsonPath("$.detail")
                        .value("O tipo de conteúdo enviado não é suportado pela API."))
                .andExpect(jsonPath("$.instance")
                        .value("/api/sinteses"));
    }
}