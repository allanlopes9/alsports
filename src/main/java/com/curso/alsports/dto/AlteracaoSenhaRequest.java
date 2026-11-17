package com.curso.alsports.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para alterar a senha da própria conta")
public record AlteracaoSenhaRequest(

        @Schema(
            description = "Senha atual da conta autenticada",
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Senha atual é obrigatória")
        String senhaAtual,

        @Schema(
            description = "Nova senha em texto aberto. O servidor persiste somente o hash BCrypt.",
            example = "outra-frase-senha-2026",
            format = "password",
            minLength = 12,
            maxLength = 72,
            accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 12, max = 72, message = "Nova senha deve possuir entre 12 e 72 caracteres")
        String novaSenha) {
}
