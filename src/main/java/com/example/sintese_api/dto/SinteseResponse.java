package com.example.sintese_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SinteseResponse(

        @Schema(
                description = "Síntese integrada gerada a partir dos documentos fornecidos.",
                example = "A reciclagem e o consumo consciente contribuem para a redução dos impactos ambientais, incentivando o reaproveitamento de materiais e escolhas de consumo mais sustentáveis."
        )
        String sintese
) {
}