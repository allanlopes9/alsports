package com.curso.alsports.dto;

import org.springframework.stereotype.Component;

import com.curso.alsports.model.Cliente;

@Component
public class ClienteMapper {

    private final PessoaMapper pessoaMapper;

    public ClienteMapper(PessoaMapper pessoaMapper) {
        this.pessoaMapper = pessoaMapper;
    }

    public ClienteResponse toResponse(Cliente cliente) {
        ClienteResponse response = new ClienteResponse();

        response.setId(cliente.getId());
        response.setTelefone(cliente.getTelefone());

        if (cliente.getPessoa() != null) {
            response.setPessoa(pessoaMapper.toResponse(cliente.getPessoa()));
        }

        return response;
    }
}
