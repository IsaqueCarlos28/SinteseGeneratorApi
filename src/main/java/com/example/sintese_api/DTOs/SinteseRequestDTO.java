package com.example.sintese_api.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SinteseRequestDTO(
        @NotEmpty(message = "A lista de documentos não pode ser vazia")
        @Valid
        List<DocumentoRequest> documentos
) {
}
