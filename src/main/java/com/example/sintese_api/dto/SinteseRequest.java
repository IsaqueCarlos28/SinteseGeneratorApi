package com.example.sintese_api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SinteseRequest(

        @ArraySchema(
                arraySchema = @Schema(
                        description = "Lista de documentos que serão utilizados para gerar uma única síntese integrada."
                ),
                minItems = 1,
                schema = @Schema(
                        implementation = DocumentoRequest.class
                )
        )
        @NotEmpty(message = "A lista de documentos não pode ser vazia")
        @Valid
        List<DocumentoRequest> documentos
) {
}