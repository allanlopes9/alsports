package com.curso.alsports.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_barras", nullable = false, length = 50, unique = true)
    private String codigoBarras;

    private String nome;
    private Integer quantidade;

    @Column(name = "estoque_minimo", nullable = false, precision = 18, scale = 3)
    private BigDecimal estoqueMinimo;

    private BigDecimal preco;
    private LocalDate dataCadastro;
    private Boolean ativo;
    private String unidadeMedida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "categoria_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_produto_categoria"))
    private CategoriaProduto categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "fornecedor_id",
        foreignKey = @ForeignKey(name = "fk_produto_fornecedor"))
    private Fornecedor fornecedor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public void receberEstoque(Integer quantidade) {
        validarQuantidadeMovimentacao(quantidade);

        if (this.quantidade == null
            || this.quantidade > Integer.MAX_VALUE - quantidade) {
            throw new IllegalArgumentException(
                "A quantidade excede o limite permitido para o estoque");
        }

        this.quantidade += quantidade;
    }

    public void retirarEstoque(Integer quantidade) {
        validarQuantidadeMovimentacao(quantidade);

        if (this.quantidade == null || quantidade > this.quantidade) {
            throw new IllegalArgumentException(
                "Quantidade para retirada maior que o estoque disponível");
        }

        this.quantidade -= quantidade;
    }

    private void validarQuantidadeMovimentacao(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException(
                "A quantidade da movimentação deve ser positiva");
        }
    }

    public void alterarNome(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException(
                "Nome do produto é obrigatório");
        }

        this.nome = novoNome.trim();
    }

    public void alterarPreco(BigDecimal novoPreco) {
        if (novoPreco == null
            || novoPreco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "Preço não pode ser negativo");
        }

        this.preco = novoPreco;
    }

    public void alterarEstoqueMinimo(BigDecimal novoEstoqueMinimo) {
        if (novoEstoqueMinimo == null
            || novoEstoqueMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "Estoque mínimo não pode ser negativo");
        }

        this.estoqueMinimo = novoEstoqueMinimo;
    }

    public void alterarCategoria(CategoriaProduto novaCategoria) {
        if (novaCategoria == null) {
            throw new IllegalArgumentException(
                "Categoria de produto é obrigatória");
        }

        this.categoria = novaCategoria;
    }

    public void alterarFornecedor(Fornecedor novoFornecedor) {
        this.fornecedor = novoFornecedor;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void inativar() {
        this.ativo = false;
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

    public CategoriaProduto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProduto categoria) {
        this.categoria = categoria;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }
}
