
package com.curso.alsports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação completa de um produto")
public class ProdutoResponse {

    @Schema(description = "Identificador único do produto", example = "1")
    private Long id;

    @Schema(description = "Código de barras do produto", example = "7891234567890")
    private String codigoBarras;

    @Schema(description = "Nome comercial do produto", example = "Bola de futebol")
    private String nome;

    @Schema(description = "Quantidade atual disponível em estoque, com até 3 casas decimais", example = "25.000")
    private BigDecimal quantidade;

    @Schema(description = "Quantidade mínima desejada para reposição", example = "5.000")
    private BigDecimal estoqueMinimo;

    @Schema(description = "Preço unitário do produto", example = "89.90")
    private BigDecimal preco;

    @Schema(description = "Data de cadastro do produto", example = "2026-11-04")
    private LocalDate dataCadastro;

    @Schema(description = "Indica se o produto está ativo", example = "true")
    private Boolean ativo;

    @Schema(description = "Unidade de medida do produto", example = "UN")
    private String unidadeMedida;

    @Schema(description = "Categoria à qual o produto pertence")
    private CategoriaProdutoResponse categoria;

    @Schema(description = "Fornecedor do produto, quando informado", nullable = true)
    private FornecedorResponse fornecedor;

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

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

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
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

    public CategoriaProdutoResponse getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProdutoResponse categoria) {
        this.categoria = categoria;
    }

    public FornecedorResponse getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(FornecedorResponse fornecedor) {
        this.fornecedor = fornecedor;
    }
}
