package com.curso.alsports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar um produto")
public class ProdutoRequest {

    @Schema(
        description = "Código de barras único do produto, com no máximo 50 caracteres",
        example = "7891234567890"
    )
    @NotBlank
    @Size(max = 50)
    private String codigoBarras;

    @Schema(
        description = "Nome comercial do produto, com no máximo 150 caracteres",
        example = "Bola de futebol"
    )
    @NotBlank
    @Size(max = 150)
    private String nome;

    @Schema(
        description = "Saldo inicial em estoque, que não pode ser negativo",
        example = "10"
    )
    @NotNull
    @PositiveOrZero
    private Integer quantidade;

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
        description = "Data de cadastro do produto",
        example = "2026-11-04"
    )
    @NotNull
    private LocalDate dataCadastro;

    @Schema(
        description = "Indica se o produto está ativo",
        example = "true"
    )
    @NotNull
    private Boolean ativo;

    @Schema(
        description = "Unidade de medida do produto, com no máximo 30 caracteres",
        example = "UN"
    )
    @NotBlank
    @Size(max = 30)
    private String unidadeMedida;

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

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
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

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(String unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
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
