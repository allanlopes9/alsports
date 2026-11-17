package com.curso.alsports.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para incluir um item em uma venda aberta")
public class ItemVendaRequest {

    @Schema(
        description = "Identificador de um produto existente e ativo",
        example = "1"
    )
    @NotNull
    @Positive
    private Long produtoId;

    @Schema(
        description = "Quantidade positiva do produto, com até 3 casas decimais",
        example = "1.500",
        minimum = "0.001",
        maximum = "999999999999999.999"
    )
    @NotNull
    @DecimalMin(value = "0.001")
    @Digits(integer = 15, fraction = 3)
    private BigDecimal quantidade;

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
