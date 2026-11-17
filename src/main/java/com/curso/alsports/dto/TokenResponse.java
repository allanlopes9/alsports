package com.curso.alsports.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access token JWT emitido pelo endpoint de login")
public record TokenResponse(

        @Schema(
            description = "JWT assinado com HS256. Use no cabeçalho Authorization: Bearer.",
            example = "eyJhbGciOiJIUzI1NiJ9.exemplo.assinatura"
        )
        String accessToken,

        @Schema(
            description = "Esquema de autenticação a ser usado nas próximas requisições",
            example = "Bearer"
        )
        String tokenType,

        @Schema(
            description = "Validade absoluta do token em segundos",
            example = "1800"
        )
        long expiresIn,

        @Schema(
            description = "Instante absoluto de expiração do token",
            example = "2026-11-17T21:30:00Z"
        )
        Instant expiresAt) {
}
