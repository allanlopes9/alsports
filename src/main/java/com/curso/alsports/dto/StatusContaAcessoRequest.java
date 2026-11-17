package com.curso.alsports.dto;

import com.curso.alsports.model.StatusContaAcesso;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo status de uma conta de acesso")
public record StatusContaAcessoRequest(

        @Schema(
            description = "ATIVA libera a autenticação; BLOQUEADA impede novos logins",
            example = "BLOQUEADA"
        )
        @NotNull(message = "Status é obrigatório")
        StatusContaAcesso status) {
}
