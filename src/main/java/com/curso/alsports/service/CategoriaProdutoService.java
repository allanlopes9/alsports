package com.curso.alsports.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.repository.CategoriaProdutoRepository;

@Service
public class CategoriaProdutoService {

    private final CategoriaProdutoRepository repository;

    public CategoriaProdutoService(CategoriaProdutoRepository repository) {
        this.repository = repository;
    }

    public CategoriaProduto salvar(CategoriaProduto categoria) {
        return repository.save(categoria);
    }

    public List<CategoriaProduto> listar() {
        return repository.findAll();
    }

    public CategoriaProduto buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public CategoriaProduto atualizar(Long id, CategoriaProduto categoria) {
        CategoriaProduto categoriaExistente = repository.findById(id).orElse(null);

        if (categoriaExistente == null) {
            return null;
        }

        categoriaExistente.setNome(categoria.getNome());
        categoriaExistente.setAtivo(categoria.getAtivo());

        return repository.save(categoriaExistente);
    }

    public boolean excluir(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }

        repository.deleteById(id);
        return true;
    }


}