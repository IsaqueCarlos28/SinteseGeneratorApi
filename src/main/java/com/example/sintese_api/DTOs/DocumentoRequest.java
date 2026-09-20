package com.example.sintese_api.DTOs;

import jakarta.validation.constraints.NotBlank;

public record DocumentoRequest(
        @NotBlank(message = "O conteúdo do documento não pode ser vazio")
        String conteudo
){}
