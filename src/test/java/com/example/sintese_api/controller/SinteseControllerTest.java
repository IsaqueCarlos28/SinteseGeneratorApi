package com.example.sintese_api.controller;

import com.example.sintese_api.service.SinteseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
}