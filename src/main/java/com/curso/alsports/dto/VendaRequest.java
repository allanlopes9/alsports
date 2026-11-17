package com.curso.alsports.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para abrir uma venda")
public class VendaRequest {

    @Schema(
        description = "Identificador de um cliente existente",
        example = "1"
    )
    @NotNull
    @Positive
    private Long clienteId;

    @Schema(
        description = "Identificador de um colaborador VENDEDOR ou GERENTE",
        example = "1"
    )
    @NotNull
    @Positive
    private Long vendedorId;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(Long vendedorId) {
        this.vendedorId = vendedorId;
    }
}
