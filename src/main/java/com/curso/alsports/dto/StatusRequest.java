package com.curso.alsports.dto;

import com.curso.alsports.model.Status;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para alterar a situação de um registro")
public record StatusRequest(
        @Schema(
            description = "Nova situação do registro: ATIVO ou INATIVO",
            example = "ATIVO"
        )
        @NotNull(message = "Status é obrigatório")
        Status status) {
}
