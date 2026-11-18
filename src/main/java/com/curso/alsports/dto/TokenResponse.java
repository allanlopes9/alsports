package com.curso.alsports.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Par de tokens devolvido por login e refresh.
 *
 * <p>O refresh token é opaco e deve ser substituído pelo cliente a cada
 * renovação. Nenhum hash ou dado interno da sessão é exposto.</p>
 */
@Schema(description = "Par de tokens emitido por login ou renovação")
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
            description = "Validade absoluta do access token em segundos",
            example = "1800"
        )
        long expiresIn,

        @Schema(
            description = "Instante absoluto de expiração do access token",
            example = "2026-11-17T21:30:00Z"
        )
        Instant expiresAt,

        @Schema(
            description = "Refresh token opaco. Deve ser enviado no corpo de /refresh e /logout.",
            example = "valor-opaco",
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String refreshToken,

        @Schema(
            description = "Limite absoluto da família de refresh tokens",
            example = "2026-11-18T05:00:00Z"
        )
        Instant refreshTokenExpiresAt) {
}
