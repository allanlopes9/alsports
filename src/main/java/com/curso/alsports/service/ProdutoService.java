package com.curso.alsports.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.curso.alsports.model.Produto;
import com.curso.alsports.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public Produto salvar(Produto produto) {
        return repository.save(produto);
    }

    public List<Produto> listar() {
        return repository.findAll();
    }

    public Produto buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Produto atualizar(Long id, Produto produto) {
        Produto produtoExistente = repository.findById(id).orElse(null);

        if (produtoExistente == null) {
            return null;
        }

        produtoExistente.setNome(produto.getNome());
        produtoExistente.setQuantidade(produto.getQuantidade());
        produtoExistente.setPreco(produto.getPreco());
        produtoExistente.setDataCadastro(produto.getDataCadastro());
        produtoExistente.setAtivo(produto.getAtivo());
        produtoExistente.setCategoria(produto.getCategoria());

        return repository.save(produtoExistente);
    }

    public boolean excluir(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }

        repository.deleteById(id);
        return true;
    }


}