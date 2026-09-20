package com.example.sintese_api.controller;

import com.example.sintese_api.client.GeminiClient;
import com.example.sintese_api.prompt.SintesePromptLoader;
import com.example.sintese_api.service.SinteseService;
import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sinteses")
public class SinteseController {

    private final SinteseService sinteseService;

    public SinteseController(SinteseService sinteseService) {
        this.sinteseService = sinteseService;
    }

    @PostMapping
    public ResponseEntity<SinteseResponse> criarSintese(
            @Valid @RequestBody SinteseRequest request
    ) {
        SinteseResponse response =
                sinteseService.gerarSintese(request);

        return ResponseEntity.ok(response);
    }


}
