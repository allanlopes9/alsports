package com.curso.alsports.dto;

import org.springframework.stereotype.Component;

import com.curso.alsports.model.ContaAcesso;

@Component
public class ContaAcessoMapper {

    public ContaAcessoResponse toResponse(ContaAcesso conta) {
        return new ContaAcessoResponse(
                conta.getId(),
                conta.getPessoa().getId(),
                conta.getPessoa().getNome(),
                conta.getLogin(),
                conta.getStatus(),
                conta.getPerfis());
    }
}
