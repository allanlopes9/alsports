package com.curso.alsports.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais para emitir um access token JWT")
public record LoginRequest(

        @Schema(
            description = "Login da conta de acesso, sem diferenciar maiúsculas de minúsculas",
            example = "ana.souza",
            maxLength = 80
        )
        @NotBlank(message = "Login é obrigatório")
        @Size(max = 80, message = "Login deve possuir no máximo 80 caracteres")
        String login,

        @Schema(
            description = "Senha em texto aberto. A senha nunca é devolvida nem incluída no token.",
            example = "uma-frase-senha-2026",
            format = "password",
            maxLength = 72,
            accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Senha é obrigatória")
        @Size(max = 72, message = "Senha deve possuir no máximo 72 caracteres")
        String senha) {
}
