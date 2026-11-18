package com.curso.alsports.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo dos endpoints que recebem o refresh token opaco.
 *
 * <p>Não há anotação Bean Validation: token vazio, nulo ou desconhecido deve
 * produzir a mesma resposta 401 do serviço, sem revelar o motivo.</p>
 */
@Schema(description = "Refresh token opaco recebido no corpo da requisição")
public record RefreshTokenRequest(

        @Schema(
            description = "Valor opaco devolvido por login ou refresh",
            example = "valor-opaco",
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String refreshToken) {
}
