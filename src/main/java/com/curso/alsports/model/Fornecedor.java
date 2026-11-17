package com.curso.alsports.model;

import com.curso.alsports.domain.DocumentoFiscal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "fornecedor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fornecedor_cnpj",
                columnNames = "cnpj"))
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 150)
    private String razaoSocial;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    public Fornecedor() {
    }

    public Fornecedor(String razaoSocial, String cnpj) {
        this.razaoSocial = razaoSocial;
        this.cnpj = validarCnpj(cnpj);
        this.status = Status.ATIVO;
    }

    public void alterarDados(String razaoSocial, String cnpj) {
        String cnpjValidado = validarCnpj(cnpj);

        this.razaoSocial = razaoSocial;
        this.cnpj = cnpjValidado;
    }

    private static String validarCnpj(String cnpj) {
        if (cnpj == null || !DocumentoFiscal.cnpjValido(cnpj)) {
            throw new IllegalArgumentException("CNPJ inválido");
        }

        return cnpj;
    }

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
        this.cnpj = validarCnpj(cnpj);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}