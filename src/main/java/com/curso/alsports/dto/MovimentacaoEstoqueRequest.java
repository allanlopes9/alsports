package com.curso.alsports.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para movimentar o estoque de um produto")
public record MovimentacaoEstoqueRequest(
        @Schema(
            description = "Quantidade positiva, com até 3 casas decimais, a ser somada ou subtraída do saldo",
            example = "0.500",
            minimum = "0.001",
            maximum = "999999999999999.999"
        )
        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser positiva")
        @Digits(
            integer = 15,
            fraction = 3,
            message = "Quantidade deve ter no máximo 3 casas decimais")
        BigDecimal quantidade) {
}
