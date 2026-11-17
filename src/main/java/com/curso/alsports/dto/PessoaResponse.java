package com.curso.alsports.dto;

import java.time.LocalDate;

import com.curso.alsports.model.Status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de uma pessoa")
public class PessoaResponse {

    @Schema(description = "Identificador único da pessoa", example = "1")
    private Long id;

    @Schema(description = "Nome completo da pessoa", example = "Ana Souza")
    private String nome;

    @Schema(description = "E-mail normalizado da pessoa", example = "ana@example.com")
    private String email;

    @Schema(description = "CPF com 11 dígitos", example = "52998224725")
    private String cpf;

    @Schema(description = "Data de cadastro controlada pelo servidor", example = "2026-11-16")
    private LocalDate dataCadastro;

    @Schema(description = "Situação cadastral da pessoa", example = "ATIVO")
    private Status status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
