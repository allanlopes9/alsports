package com.curso.alsports.dto;

import java.time.Instant;

import com.curso.alsports.model.TipoEventoSeguranca;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de leitura da trilha de segurança. Não expõe o hash de correlação.
 */
@Schema(name = "EventoSegurancaResponse", description = "Evento estruturado de segurança")
public record EventoSegurancaResponse(
        @Schema(description = "Identificador do evento", example = "42")
        Long id,

        @Schema(description = "Tipo do evento", example = "LOGIN_FALHA")
        TipoEventoSeguranca tipo,

        @Schema(description = "Instante em que o fato ocorreu")
        Instant ocorridoEm,

        @Schema(description = "Conta autora, quando conhecida")
        Long atorContaId,

        @Schema(description = "Conta afetada, quando conhecida")
        Long alvoContaId,

        @Schema(description = "Endereço de origem observado", example = "127.0.0.1")
        String enderecoOrigem) {
}
