package com.curso.alsports.model;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "colaborador",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_colaborador_pessoa",
                    columnNames = "pessoa_id"),
            @UniqueConstraint(
                    name = "uk_colaborador_matricula",
                    columnNames = "matricula")
        })
public class Colaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pessoa_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_colaborador_pessoa"))
    private Pessoa pessoa;

    @Column(nullable = false, length = 30)
    private String matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FuncaoColaborador funcao;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    protected Colaborador() {
    }

    public Colaborador(
            Pessoa pessoa,
            String matricula,
            FuncaoColaborador funcao,
            LocalDate dataAdmissao) {

        this.pessoa = Objects.requireNonNull(pessoa, "Pessoa é obrigatória");
        this.matricula = validarMatricula(matricula);
        this.funcao = Objects.requireNonNull(funcao, "Função é obrigatória");
        this.dataAdmissao = Objects.requireNonNull(
                dataAdmissao, "Data de admissão é obrigatória");
    }

    public void alterarDados(String matricula, FuncaoColaborador funcao) {
        this.matricula = validarMatricula(matricula);
        this.funcao = Objects.requireNonNull(funcao, "Função é obrigatória");
    }

    public boolean podeRealizarVenda() {
        return funcao != null && funcao.podeRealizarVenda();
    }

    private static String validarMatricula(String matricula) {
        if (matricula == null || matricula.isBlank()) {
            throw new IllegalArgumentException("Matrícula é obrigatória");
        }

        return matricula.trim();
    }

    public Long getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public String getMatricula() {
        return matricula;
    }

    public FuncaoColaborador getFuncao() {
        return funcao;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }
}
