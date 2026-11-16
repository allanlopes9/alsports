package com.curso.alsports.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para movimentar o estoque de um produto")
public record MovimentacaoEstoqueRequest(
        @Schema(
            description = "Quantidade inteira positiva a ser somada ou subtraída do saldo",
            example = "5"
        )
        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        Integer quantidade) {
}
