package com.example.sintese_api.controller;

import com.example.sintese_api.dto.SinteseRequest;
import com.example.sintese_api.dto.SinteseResponse;
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
    public ResponseEntity<SinteseResponse> criarSintese(
            @Valid @RequestBody SinteseRequest request
    ) {

        return ResponseEntity.ok(
                new SinteseResponse("API funcionando!")
        );
    }
}
