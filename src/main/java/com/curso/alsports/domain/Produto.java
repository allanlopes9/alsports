package com.curso.alsports.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Produto {

    private final String nome;
    private Integer quantidade;
    private BigDecimal preco;
    private final LocalDate dataCadastro;
    private Status status;
    private CategoriaProduto categoria;

    public Produto(
            String nome,
            Integer quantidade,
            BigDecimal preco,
            LocalDate dataCadastro) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório");
        }

        if (quantidade == null || quantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }

        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo");
        }

        if (dataCadastro == null) {
            throw new IllegalArgumentException("Data de cadastro é obrigatória");
        }

        this.nome = nome.trim();
        this.quantidade = quantidade;
        this.preco = preco;
        this.dataCadastro = dataCadastro;
        this.status = Status.ATIVO;
    }

    public String getNome() {
        return nome;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public Status getStatus() {
        return status;
    }

    public CategoriaProduto getCategoria() {
        return categoria;
    }

    public void receberEstoque(Integer quantidade) {
        validarQuantidadeMovimentacao(quantidade);
        this.quantidade += quantidade;
    }

    public void retirarEstoque(Integer quantidade) {
        validarQuantidadeMovimentacao(quantidade);

        if (quantidade > this.quantidade) {
            throw new IllegalArgumentException("Quantidade para retirada maior que o estoque disponível");
        }

        this.quantidade -= quantidade;
    }

    public void alterarPreco(BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo");
        }

        this.preco = preco;
    }

    public void ativar() {
        this.status = Status.ATIVO;
    }

    public void inativar() {
        this.status = Status.INATIVO;
    }

    public BigDecimal calcularValorEstoque() {
        return preco
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }

    void associarA(CategoriaProduto categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("Categoria não pode ser nula");
        }

        if (this.categoria != null && this.categoria != categoria) {
            throw new IllegalArgumentException("Produto já pertence a outra categoria");
        }

        this.categoria = categoria;
    }

    private void validarQuantidadeMovimentacao(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade da movimentação deve ser positiva");
        }
    }
}
