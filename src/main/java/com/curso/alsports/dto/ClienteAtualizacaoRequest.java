package com.curso.alsports.dto;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para alterar um cliente")
public class ClienteAtualizacaoRequest {

    @Schema(
        description = "Novo telefone do cliente, com no máximo 30 caracteres",
        example = "(11) 98888-1111",
        nullable = true
    )
    @Size(max = 30)
    private String telefone;

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
