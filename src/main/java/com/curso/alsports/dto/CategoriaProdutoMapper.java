package com.curso.alsports.dto;

import org.springframework.stereotype.Component;

import com.curso.alsports.model.CategoriaProduto;

@Component
public class CategoriaProdutoMapper {

    public CategoriaProdutoResponse toResponse(CategoriaProduto categoria) {
        CategoriaProdutoResponse response = new CategoriaProdutoResponse();
        response.setId(categoria.getId());
        response.setNome(categoria.getNome());
        response.setAtivo(categoria.getAtivo());
        return response;
    }

    public CategoriaProduto toEntity(CategoriaProdutoRequest request) {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome(request.getNome());
        categoria.setAtivo(request.getAtivo());
        return categoria;
    }
}
