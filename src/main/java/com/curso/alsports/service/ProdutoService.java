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
}