package com.curso.alsports.dto;

import java.time.LocalDate;

import com.curso.alsports.model.FuncaoColaborador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para criar o vínculo profissional de um colaborador")
public class ColaboradorRequest {

    @Schema(
        description = "Identificador de uma pessoa existente e ativa",
        example = "2"
    )
    @NotNull
    @Positive
    private Long pessoaId;

    @Schema(
        description = "Matrícula única do colaborador, com no máximo 30 caracteres",
        example = "VEN-001"
    )
    @NotBlank
    @Size(max = 30)
    private String matricula;

    @Schema(
        description = "Função profissional do colaborador",
        example = "VENDEDOR"
    )
    @NotNull
    private FuncaoColaborador funcao;

    @Schema(
        description = "Data de admissão do colaborador",
        example = "2026-09-29"
    )
    @NotNull
    private LocalDate dataAdmissao;

    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public FuncaoColaborador getFuncao() {
        return funcao;
    }

    public void setFuncao(FuncaoColaborador funcao) {
        this.funcao = funcao;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(LocalDate dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }
}
