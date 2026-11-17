package com.curso.alsports.dto;

import com.curso.alsports.model.FuncaoColaborador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para alterar um colaborador")
public class ColaboradorAtualizacaoRequest {

    @Schema(
        description = "Nova matrícula única do colaborador, com no máximo 30 caracteres",
        example = "VEN-002"
    )
    @NotBlank
    @Size(max = 30)
    private String matricula;

    @Schema(
        description = "Nova função profissional do colaborador",
        example = "GERENTE"
    )
    @NotNull
    private FuncaoColaborador funcao;

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
}
