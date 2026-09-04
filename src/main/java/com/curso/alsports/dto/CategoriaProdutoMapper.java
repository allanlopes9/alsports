package com.curso.alsports.dto;

import com.curso.alsports.model.CategoriaProduto;

public final class CategoriaProdutoMapper {

    private CategoriaProdutoMapper() {
    }

    public static CategoriaProdutoResponse toResponse(CategoriaProduto categoria) {
        CategoriaProdutoResponse response = new CategoriaProdutoResponse();
        response.setId(categoria.getId());
        response.setNome(categoria.getNome());
        response.setAtivo(categoria.getAtivo());
        return response;
    }

    public static CategoriaProduto toEntity(CategoriaProdutoRequest request) {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome(request.getNome());
        categoria.setAtivo(request.getAtivo());
        return categoria;
    }
}