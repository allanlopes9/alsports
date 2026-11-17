package com.curso.alsports.dto;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.curso.alsports.model.Pessoa;

@Component
public class PessoaMapper {

    public PessoaResponse toResponse(Pessoa pessoa) {
        PessoaResponse response = new PessoaResponse();

        response.setId(pessoa.getId());
        response.setNome(pessoa.getNome());
        response.setEmail(pessoa.getEmail());
        response.setCpf(pessoa.getCpf());
        response.setDataCadastro(pessoa.getDataCadastro());
        response.setStatus(pessoa.getStatus());

        return response;
    }

    public Pessoa toEntity(PessoaRequest request) {
        return new Pessoa(
                request.getNome(),
                request.getEmail(),
                request.getCpf(),
                LocalDate.now());
    }
}
