package com.curso.alsports.dto;

import com.curso.alsports.validation.CnpjValido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar um fornecedor")
public class FornecedorRequest {

    @Schema(
        description = "Razão social do fornecedor, com no máximo 150 caracteres",
        example = "Fornecedor Exemplo Ltda"
    )
    @NotBlank
    @Size(max = 150)
    private String razaoSocial;

    @Schema(
        description = "CNPJ do fornecedor, contendo exatamente 14 dígitos numéricos e dígitos verificadores válidos",
        example = "11222333000181"
    )
    @NotBlank
    @CnpjValido
    private String cnpj;

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
}
