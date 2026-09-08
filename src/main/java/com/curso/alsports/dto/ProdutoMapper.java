package com.curso.alsports.dto;

import org.springframework.stereotype.Component;

import com.curso.alsports.model.Produto;

@Component
public class ProdutoMapper {

    private final CategoriaProdutoMapper categoriaProdutoMapper;
    private final FornecedorMapper fornecedorMapper;

    public ProdutoMapper(
            CategoriaProdutoMapper categoriaProdutoMapper,
            FornecedorMapper fornecedorMapper) {

        this.categoriaProdutoMapper = categoriaProdutoMapper;
        this.fornecedorMapper = fornecedorMapper;
    }

    public ProdutoResponse toResponse(Produto produto) {
        ProdutoResponse response = new ProdutoResponse();

        response.setId(produto.getId());
        response.setCodigoBarras(produto.getCodigoBarras());
        response.setNome(produto.getNome());
        response.setQuantidade(produto.getQuantidade());
        response.setEstoqueMinimo(produto.getEstoqueMinimo());
        response.setPreco(produto.getPreco());
        response.setDataCadastro(produto.getDataCadastro());
        response.setAtivo(produto.getAtivo());
        response.setUnidadeMedida(produto.getUnidadeMedida());

        if (produto.getCategoria() != null) {
            response.setCategoria(
                    categoriaProdutoMapper.toResponse(produto.getCategoria()));
        }

        if (produto.getFornecedor() != null) {
            response.setFornecedor(
                    fornecedorMapper.toResponse(produto.getFornecedor()));
        }

        return response;
    }

    public Produto toEntity(ProdutoRequest request) {
        Produto produto = new Produto();

        produto.setCodigoBarras(request.getCodigoBarras());
        produto.setNome(request.getNome());
        produto.setQuantidade(request.getQuantidade());
        produto.setEstoqueMinimo(request.getEstoqueMinimo());
        produto.setPreco(request.getPreco());
        produto.setDataCadastro(request.getDataCadastro());
        produto.setAtivo(request.getAtivo());
        produto.setUnidadeMedida(request.getUnidadeMedida());

        return produto;
    }
}
