package com.curso.alsports.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para alterar um produto")
public class ProdutoUpdateRequest {

    @Schema(
        description = "Novo nome comercial do produto, com no máximo 150 caracteres",
        example = "Bola de futebol oficial"
    )
    @NotBlank
    @Size(max = 150)
    private String nome;

    @Schema(
        description = "Quantidade mínima desejada para reposição, que não pode ser negativa",
        example = "5.000"
    )
    @NotNull
    @DecimalMin(value = "0.000")
    private BigDecimal estoqueMinimo;

    @Schema(
        description = "Preço unitário do produto, que não pode ser negativo",
        example = "89.90"
    )
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal preco;

    @Schema(
        description = "Identificador de uma categoria existente",
        example = "1"
    )
    @NotNull
    @Positive
    private Long categoriaId;

    @Schema(
        description = "Identificador opcional de um fornecedor existente",
        example = "1",
        nullable = true
    )
    @Positive
    private Long fornecedorId;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(BigDecimal estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(Long fornecedorId) {
        this.fornecedorId = fornecedorId;
    }
}