package com.curso.alsports.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.curso.alsports.validation.QuantidadeValidator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "item_venda",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_item_venda_produto",
                columnNames = {"venda_id", "produto_id"}))
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "venda_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_venda_venda"))
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "produto_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_venda_produto"))
    private Produto produto;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantidade;

    @Column(
            name = "valor_unitario",
            nullable = false,
            precision = 18,
            scale = 2)
    private BigDecimal valorUnitario;

    protected ItemVenda() {
    }

    ItemVenda(
            Venda venda,
            Produto produto,
            BigDecimal quantidade,
            BigDecimal valorUnitario) {

        this.venda = Objects.requireNonNull(venda, "Venda é obrigatória");
        this.produto = Objects.requireNonNull(produto, "Produto é obrigatório");
        this.quantidade = validarPositivo(quantidade);
        this.valorUnitario = validarNaoNegativo(valorUnitario);
    }

    public BigDecimal calcularSubtotal() {
        return quantidade
                .multiply(valorUnitario)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal validarPositivo(BigDecimal quantidade) {
        return QuantidadeValidator.exigirPositiva(
                quantidade, "Quantidade do item deve ser positiva");
    }

    private static BigDecimal validarNaoNegativo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor unitário não pode ser negativo");
        }

        return valor;
    }

    public Long getId() {
        return id;
    }

    public Venda getVenda() {
        return venda;
    }

    public Produto getProduto() {
        return produto;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }
}
