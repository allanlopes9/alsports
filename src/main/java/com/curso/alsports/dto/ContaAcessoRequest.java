package com.curso.alsports.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastrar uma conta de acesso")
public record ContaAcessoRequest(

        @Schema(
            description = "Identificador da pessoa que receberá a credencial",
            example = "1"
        )
        @NotNull(message = "Pessoa é obrigatória")
        Long pessoaId,

        @Schema(
            description = "Login único, sem diferenciar maiúsculas de minúsculas. "
                + "Aceita letras, números, ponto, sublinhado e hífen.",
            example = "ana.souza",
            minLength = 3,
            maxLength = 80
        )
        @NotBlank(message = "Login é obrigatório")
        @Size(min = 3, max = 80, message = "Login deve possuir entre 3 e 80 caracteres")
        @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "Login deve conter apenas letras, números, ponto, sublinhado ou hífen"
        )
        String login,

        @Schema(
            description = "Senha em texto aberto. O servidor persiste somente o hash BCrypt.",
            example = "uma-frase-senha-2026",
            format = "password",
            minLength = 12,
            maxLength = 72,
            accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 12, max = 72, message = "Senha deve possuir entre 12 e 72 caracteres")
        String senha) {
}
