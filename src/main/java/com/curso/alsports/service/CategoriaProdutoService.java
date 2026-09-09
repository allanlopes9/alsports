package com.curso.alsports.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.repository.CategoriaProdutoRepository;

@Service
public class CategoriaProdutoService {

    private final CategoriaProdutoRepository repository;

    public CategoriaProdutoService(CategoriaProdutoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CategoriaProduto salvar(CategoriaProduto categoria) {
        String nome = categoria.getNome().trim();

        repository.findByNomeIgnoreCase(nome)
                .ifPresent(existente -> {
                    throw new RecursoDuplicadoException(
                            "Já existe uma categoria com o nome: " + nome);
                });

        categoria.setNome(nome);

        return repository.save(categoria);
    }

    @Transactional(readOnly = true)
    public List<CategoriaProduto> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public CategoriaProduto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + id));
    }

    @Transactional
    public CategoriaProduto atualizar(Long id, CategoriaProduto categoria) {
        CategoriaProduto categoriaExistente = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + id));

        String nome = categoria.getNome().trim();

        repository.findByNomeIgnoreCase(nome)
                .ifPresent(outraCategoria -> {
                    if (!outraCategoria.getId().equals(id)) {
                        throw new RecursoDuplicadoException(
                                "Já existe uma categoria com o nome: " + nome);
                    }
                });

        categoriaExistente.setNome(nome);
        categoriaExistente.setAtivo(categoria.getAtivo());

        return categoriaExistente;
    }

    @Transactional
    public boolean excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Categoria não encontrada: " + id);
        }

        repository.deleteById(id);
        return true;
    }
}