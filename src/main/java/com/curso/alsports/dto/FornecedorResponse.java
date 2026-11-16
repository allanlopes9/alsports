
package com.curso.alsports.dto;

import com.curso.alsports.model.Status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de um fornecedor")
public class FornecedorResponse {

    @Schema(description = "Identificador único do fornecedor", example = "1")
    private Long id;

    @Schema(description = "Razão social do fornecedor", example = "Fornecedor Exemplo Ltda")
    private String razaoSocial;

    @Schema(description = "CNPJ do fornecedor", example = "00.000.000/0000-00")
    private String cnpj;

    @Schema(description = "Situação cadastral do fornecedor", example = "ATIVO")
    private Status status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
