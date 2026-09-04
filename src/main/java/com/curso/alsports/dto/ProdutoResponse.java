package com.curso.alsports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProdutoResponse {

    private Long id;
    private String nome;
    private Integer quantidade;
    private BigDecimal estoqueMinimo;
    private BigDecimal preco;
    private LocalDate dataCadastro;
    private Boolean ativo;
    private String unidadeMedida;
    private CategoriaProdutoResponse categoria;
    private FornecedorResponse fornecedor;

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