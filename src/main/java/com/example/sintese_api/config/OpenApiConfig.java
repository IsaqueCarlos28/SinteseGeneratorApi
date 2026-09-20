package com.example.sintese_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sinteseOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Síntese API")
                                .description("""
                                        API REST para geração de sínteses integradas
                                        utilizando inteligência artificial generativa.

                                        A API recebe um ou mais documentos e utiliza
                                        a Gemini para produzir uma única síntese
                                        integrada em português do Brasil.
                                        """)
                                .version("1.0.0")
                );
    }
}