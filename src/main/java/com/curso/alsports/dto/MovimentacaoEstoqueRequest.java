package com.curso.alsports.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovimentacaoEstoqueRequest(
        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        Integer quantidade) {
}
