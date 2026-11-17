package com.curso.alsports.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para associar o papel de cliente a uma pessoa")
public class ClienteRequest {

    @Schema(
        description = "Identificador de uma pessoa existente e ativa",
        example = "1"
    )
    @NotNull
    @Positive
    private Long pessoaId;

    @Schema(
        description = "Telefone opcional do cliente, com no máximo 30 caracteres",
        example = "(11) 99999-0000",
        nullable = true
    )
    @Size(max = 30)
    private String telefone;

    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
