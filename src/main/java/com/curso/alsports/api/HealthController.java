package com.curso.alsports.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Saúde", description = "Verificação de disponibilidade da API")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(
        summary = "Verificar disponibilidade da API",
        description = "Retorna OK quando a API está disponível."
    )
    public String health() {
        return "OK";
    }
}
