
package com.curso.alsports.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de uma categoria de produtos")
public class CategoriaProdutoResponse {

    @Schema(description = "Identificador único da categoria", example = "1")
    private Long id;

    @Schema(description = "Nome da categoria", example = "Futebol")
    private String nome;

    @Schema(description = "Indica se a categoria está ativa", example = "true")
    private Boolean ativo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
