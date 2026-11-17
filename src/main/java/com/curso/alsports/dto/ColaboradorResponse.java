package com.curso.alsports.dto;

import java.time.LocalDate;

import com.curso.alsports.model.FuncaoColaborador;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de um colaborador")
public class ColaboradorResponse {

    @Schema(description = "Identificador único do colaborador", example = "1")
    private Long id;

    @Schema(description = "Pessoa associada ao papel de colaborador")
    private PessoaResponse pessoa;

    @Schema(description = "Matrícula única do colaborador", example = "VEN-001")
    private String matricula;

    @Schema(description = "Função profissional do colaborador", example = "VENDEDOR")
    private FuncaoColaborador funcao;

    @Schema(description = "Data de admissão do colaborador", example = "2026-09-29")
    private LocalDate dataAdmissao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PessoaResponse getPessoa() {
        return pessoa;
    }

    public void setPessoa(PessoaResponse pessoa) {
        this.pessoa = pessoa;
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
