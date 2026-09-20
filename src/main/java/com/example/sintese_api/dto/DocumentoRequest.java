package com.example.sintese_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DocumentoRequest(

        @Schema(
                description = "Conteúdo textual do documento que será utilizado na síntese.",
                example = "A reciclagem transforma materiais descartados em novos produtos, reduzindo a necessidade de extrair matéria-prima virgem da natureza."
        )
        @NotBlank(message = "O conteúdo do documento não pode ser vazio")
        String conteudo
) {
}