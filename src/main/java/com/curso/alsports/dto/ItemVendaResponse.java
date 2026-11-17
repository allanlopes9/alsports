package com.curso.alsports.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de um item de venda")
public class ItemVendaResponse {

    @Schema(description = "Identificador único do item", example = "1")
    private Long id;

    @Schema(description = "Identificador do produto", example = "1")
    private Long produtoId;

    @Schema(description = "Descrição do produto no momento da consulta", example = "Bola de futebol")
    private String produtoDescricao;

    @Schema(description = "Quantidade vendida, com até 3 casas decimais", example = "1.500")
    private BigDecimal quantidade;

    @Schema(description = "Preço unitário capturado na inclusão do item", example = "89.90")
    private BigDecimal valorUnitario;

    @Schema(description = "Subtotal do item", example = "179.80")
    private BigDecimal subtotal;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoDescricao() {
        return produtoDescricao;
    }

    public void setProdutoDescricao(String produtoDescricao) {
        this.produtoDescricao = produtoDescricao;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
