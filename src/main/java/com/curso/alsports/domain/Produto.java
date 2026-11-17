package com.curso.alsports.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.curso.alsports.validation.QuantidadeValidator;

public class Produto {

    private final String nome;
    private BigDecimal quantidade;
    private BigDecimal preco;
    private final LocalDate dataCadastro;
    private Status status;
    private CategoriaProduto categoria;

    public Produto(
            String nome,
            BigDecimal quantidade,
            BigDecimal preco,
            LocalDate dataCadastro) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório");
        }

        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo");
        }

        if (dataCadastro == null) {
            throw new IllegalArgumentException("Data de cadastro é obrigatória");
        }

        this.nome = nome.trim();
        this.quantidade = QuantidadeValidator.exigirNaoNegativa(
                quantidade, "Quantidade não pode ser negativa");
        this.preco = preco;
        this.dataCadastro = dataCadastro;
        this.status = Status.ATIVO;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getQuantidade() {
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

    public void receberEstoque(BigDecimal quantidade) {
        BigDecimal valor = QuantidadeValidator.exigirPositiva(
                quantidade, "Quantidade da movimentação deve ser positiva");
        this.quantidade = this.quantidade.add(valor).setScale(
                QuantidadeValidator.ESCALA, RoundingMode.UNNECESSARY);
    }

    public void retirarEstoque(BigDecimal quantidade) {
        BigDecimal valor = QuantidadeValidator.exigirPositiva(
                quantidade, "Quantidade da movimentação deve ser positiva");

        if (valor.compareTo(this.quantidade) > 0) {
            throw new IllegalArgumentException(
                    "Quantidade para retirada maior que o estoque disponível");
        }

        this.quantidade = this.quantidade.subtract(valor).setScale(
                QuantidadeValidator.ESCALA, RoundingMode.UNNECESSARY);
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
                .multiply(quantidade)
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
}
