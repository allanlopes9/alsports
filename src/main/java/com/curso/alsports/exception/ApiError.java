package com.curso.alsports.exception;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação padronizada de um erro da API")
public record ApiError(

    @Schema(example = "2026-09-29T18:30:00Z")
    Instant timestamp,

    @Schema(description = "Código HTTP numérico", example = "400")
    int status,

    @Schema(
        description = "Nome padronizado do status HTTP",
        example = "Bad Request"
    )
    String error,

    @Schema(description = "Mensagem segura para o consumidor")
    String message,

    @Schema(example = "/produtos")
    String path,

    @Schema(description = "Erros separados por campo")
    Map<String, String> fields

) {
}
