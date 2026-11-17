package com.curso.alsports.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de um cliente")
public class ClienteResponse {

    @Schema(description = "Identificador único do cliente", example = "1")
    private Long id;

    @Schema(description = "Pessoa associada ao papel de cliente")
    private PessoaResponse pessoa;

    @Schema(description = "Telefone do cliente", example = "(11) 99999-0000", nullable = true)
    private String telefone;

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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
