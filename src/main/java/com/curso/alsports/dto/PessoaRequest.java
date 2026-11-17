package com.curso.alsports.dto;

import com.curso.alsports.validation.CpfValido;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar ou alterar uma pessoa")
public class PessoaRequest {

    @Schema(
        description = "Nome completo da pessoa, com no máximo 150 caracteres",
        example = "Ana Souza"
    )
    @NotBlank
    @Size(max = 150)
    private String nome;

    @Schema(
        description = "E-mail da pessoa, normalizado para minúsculas, com no máximo 180 caracteres",
        example = "ana@example.com"
    )
    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @Schema(
        description = "CPF com exatamente 11 dígitos e dígitos verificadores válidos",
        example = "52998224725"
    )
    @NotBlank
    @CpfValido
    private String cpf;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
}
