package com.curso.alsports.dto;

import java.util.Set;

import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.StatusContaAcesso;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação pública de uma conta de acesso, sem credenciais")
public record ContaAcessoResponse(

        @Schema(description = "Identificador único da conta", example = "1")
        Long id,

        @Schema(description = "Identificador da pessoa vinculada", example = "1")
        Long pessoaId,

        @Schema(description = "Nome da pessoa vinculada", example = "Ana Souza")
        String nomePessoa,

        @Schema(description = "Login normalizado da conta", example = "ana.souza")
        String login,

        @Schema(description = "Situação da conta", example = "ATIVA")
        StatusContaAcesso status,

        @Schema(description = "Perfis concedidos à conta", example = "[\"OPERADOR\"]")
        Set<PerfilAcesso> perfis) {
}
