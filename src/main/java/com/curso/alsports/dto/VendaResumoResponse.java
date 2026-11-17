package com.curso.alsports.dto;

import java.time.LocalDateTime;

import com.curso.alsports.model.StatusVenda;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de uma venda, sem itens e sem total calculado")
public class VendaResumoResponse {

    @Schema(description = "Identificador único da venda", example = "1")
    private Long id;

    @Schema(description = "Identificador do cliente", example = "1")
    private Long clienteId;

    @Schema(description = "Nome do cliente", example = "Ana Souza")
    private String clienteNome;

    @Schema(description = "Identificador do colaborador vendedor", example = "1")
    private Long vendedorId;

    @Schema(description = "Nome do colaborador vendedor", example = "Bruno Lima")
    private String vendedorNome;

    @Schema(description = "Data e hora da abertura da venda", example = "2026-11-16T10:30:00")
    private LocalDateTime dataVenda;

    @Schema(description = "Situação atual da venda", example = "ABERTA")
    private StatusVenda status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public Long getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(Long vendedorId) {
        this.vendedorId = vendedorId;
    }

    public String getVendedorNome() {
        return vendedorNome;
    }

    public void setVendedorNome(String vendedorNome) {
        this.vendedorNome = vendedorNome;
    }

    public LocalDateTime getDataVenda() {
        return dataVenda;
    }

    public void setDataVenda(LocalDateTime dataVenda) {
        this.dataVenda = dataVenda;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public void setStatus(StatusVenda status) {
        this.status = status;
    }
}
