package com.curso.alsports.dto;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Fornecedor;
import com.curso.alsports.model.Produto;

public final class ProdutoMapper {

    private ProdutoMapper() {
    }

    public static ProdutoResponse toResponse(Produto produto) {
        ProdutoResponse response = new ProdutoResponse();

        response.setId(produto.getId());
        response.setNome(produto.getNome());
        response.setQuantidade(produto.getQuantidade());
        response.setEstoqueMinimo(produto.getEstoqueMinimo());
        response.setPreco(produto.getPreco());
        response.setDataCadastro(produto.getDataCadastro());
        response.setAtivo(produto.getAtivo());
        response.setUnidadeMedida(produto.getUnidadeMedida());

        if (produto.getCategoria() != null) {
            response.setCategoria(
                    CategoriaProdutoMapper.toResponse(produto.getCategoria()));
        }

        if (produto.getFornecedor() != null) {
            response.setFornecedor(
                    FornecedorMapper.toResponse(produto.getFornecedor()));
        }

        return response;
    }

    public static Produto toEntity(
            ProdutoRequest request,
            CategoriaProduto categoria,
            Fornecedor fornecedor) {

        Produto produto = new Produto();

        produto.setNome(request.getNome());
        produto.setQuantidade(request.getQuantidade());
        produto.setEstoqueMinimo(request.getEstoqueMinimo());
        produto.setPreco(request.getPreco());
        produto.setDataCadastro(request.getDataCadastro());
        produto.setAtivo(request.getAtivo());
        produto.setUnidadeMedida(request.getUnidadeMedida());
        produto.setCategoria(categoria);
        produto.setFornecedor(fornecedor);

        return produto;
    }
}