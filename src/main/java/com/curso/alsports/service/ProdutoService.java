package com.curso.alsports.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.Produto;
import com.curso.alsports.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Produto salvar(Produto produto) {
        if (repository.existsByNomeIgnoreCase(produto.getNome())) {
            throw new RecursoDuplicadoException(
                    "Já existe um produto com o nome: " + produto.getNome());
        }

        return repository.save(produto);
    }

    @Transactional
    public Produto salvarEFalhar(Produto produto) {
        Produto produtoSalvo = repository.save(produto);

        throw new RuntimeException("Erro proposital para testar rollback");
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Produto buscarPorNome(String nome) {
        return repository.findByNomeIgnoreCase(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado: " + nome));
    }

    @Transactional(readOnly = true)
    public boolean existePorNome(String nome) {
        return repository.existsByNomeIgnoreCase(nome);
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado: " + id));
    }

    @Transactional
    public Produto atualizar(Long id, Produto produto) {
        Produto produtoExistente = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado: " + id));

        produtoExistente.setNome(produto.getNome());
        produtoExistente.setQuantidade(produto.getQuantidade());
        produtoExistente.setPreco(produto.getPreco());
        produtoExistente.setDataCadastro(produto.getDataCadastro());
        produtoExistente.setAtivo(produto.getAtivo());
        produtoExistente.setUnidadeMedida(produto.getUnidadeMedida());
        produtoExistente.setCategoria(produto.getCategoria());

        return produtoExistente;
    }

    @Transactional
    public boolean excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Produto não encontrado: " + id);
        }

        repository.deleteById(id);
        return true;
    }
}