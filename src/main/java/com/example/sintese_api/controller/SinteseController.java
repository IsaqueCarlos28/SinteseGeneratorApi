package com.example.sintese_api.controller;

import com.example.sintese_api.DTOs.SinteseRequestDTO;
import com.example.sintese_api.DTOs.SinteseResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sinteses")
public class SinteseController {
    @PostMapping
    public ResponseEntity<SinteseResponseDTO> criarSintese(
            @Valid @RequestBody SinteseRequestDTO request
    ) {

        return ResponseEntity.ok(
                new SinteseResponseDTO("API funcionando!")
        );
    }
}
