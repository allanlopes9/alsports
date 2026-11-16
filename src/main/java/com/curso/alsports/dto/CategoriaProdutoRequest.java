package com.curso.alsports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar uma categoria de produtos")
public class CategoriaProdutoRequest {

    @Schema(
        description = "Nome da categoria, com no máximo 100 caracteres",
        example = "Futebol"
    )
    @NotBlank
    @Size(max = 100)
    private String nome;

    @Schema(
        description = "Indica se a categoria está ativa",
        example = "true"
    )
    @NotNull
    private Boolean ativo;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
