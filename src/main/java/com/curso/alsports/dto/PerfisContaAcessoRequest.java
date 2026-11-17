package com.curso.alsports.dto;

import java.util.Set;

import com.curso.alsports.model.PerfilAcesso;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Perfis que substituirão integralmente os perfis da conta")
public record PerfisContaAcessoRequest(

        @Schema(
            description = "Ao menos um perfil. A conta nunca fica sem perfil.",
            example = "[\"OPERADOR\", \"ADMINISTRADOR\"]"
        )
        @NotEmpty(message = "Informe ao menos um perfil")
        Set<PerfilAcesso> perfis) {
}
