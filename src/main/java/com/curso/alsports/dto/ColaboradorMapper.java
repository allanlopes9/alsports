package com.curso.alsports.dto;

import org.springframework.stereotype.Component;

import com.curso.alsports.model.Colaborador;

@Component
public class ColaboradorMapper {

    private final PessoaMapper pessoaMapper;

    public ColaboradorMapper(PessoaMapper pessoaMapper) {
        this.pessoaMapper = pessoaMapper;
    }

    public ColaboradorResponse toResponse(Colaborador colaborador) {
        ColaboradorResponse response = new ColaboradorResponse();

        response.setId(colaborador.getId());
        response.setMatricula(colaborador.getMatricula());
        response.setFuncao(colaborador.getFuncao());
        response.setDataAdmissao(colaborador.getDataAdmissao());

        if (colaborador.getPessoa() != null) {
            response.setPessoa(pessoaMapper.toResponse(colaborador.getPessoa()));
        }

        return response;
    }
}
