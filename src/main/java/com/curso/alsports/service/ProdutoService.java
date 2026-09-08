package com.curso.alsports.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Fornecedor;
import com.curso.alsports.model.Produto;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.FornecedorRepository;
import com.curso.alsports.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final CategoriaProdutoRepository categoriaProdutoRepository;
    private final FornecedorRepository fornecedorRepository;

    public ProdutoService(
            ProdutoRepository repository,
            CategoriaProdutoRepository categoriaProdutoRepository,
            FornecedorRepository fornecedorRepository) {

        this.repository = repository;
        this.categoriaProdutoRepository = categoriaProdutoRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public Produto cadastrar(Produto produto, Long categoriaId, Long fornecedorId) {
        if (repository.existsByCodigoBarras(produto.getCodigoBarras())) {
            throw new RecursoDuplicadoException(
                    "Já existe um produto com o código de barras: " + produto.getCodigoBarras());
        }

        if (repository.existsByNomeIgnoreCase(produto.getNome())) {
            throw new RecursoDuplicadoException(
                    "Já existe um produto com o nome: " + produto.getNome());
        }

        CategoriaProduto categoria = categoriaProdutoRepository
                .findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + categoriaId));

        Fornecedor fornecedor = null;

        if (fornecedorId != null) {
            fornecedor = fornecedorRepository.findById(fornecedorId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Fornecedor não encontrado: " + fornecedorId));
        }

        produto.setCategoria(categoria);
        produto.setFornecedor(fornecedor);

        return repository.save(produto);
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
        repository.save(produto);

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
    public Produto atualizar(
            Long id,
            Produto produto,
            Long categoriaId,
            Long fornecedorId) {

        Produto produtoExistente = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado: " + id));

        if (!produtoExistente.getCodigoBarras().equalsIgnoreCase(produto.getCodigoBarras())
                && repository.existsByCodigoBarras(produto.getCodigoBarras())) {

            throw new RecursoDuplicadoException(
                    "Já existe um produto com o código de barras: " + produto.getCodigoBarras());
        }

        if (!produtoExistente.getNome().equalsIgnoreCase(produto.getNome())
                && repository.existsByNomeIgnoreCase(produto.getNome())) {

            throw new RecursoDuplicadoException(
                    "Já existe um produto com o nome: " + produto.getNome());
        }

        CategoriaProduto categoria = categoriaProdutoRepository
                .findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + categoriaId));

        Fornecedor fornecedor = null;

        if (fornecedorId != null) {
            fornecedor = fornecedorRepository.findById(fornecedorId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Fornecedor não encontrado: " + fornecedorId));
        }

        produtoExistente.setCodigoBarras(produto.getCodigoBarras());
        produtoExistente.setNome(produto.getNome());
        produtoExistente.setQuantidade(produto.getQuantidade());
        produtoExistente.setEstoqueMinimo(produto.getEstoqueMinimo());
        produtoExistente.setPreco(produto.getPreco());
        produtoExistente.setDataCadastro(produto.getDataCadastro());
        produtoExistente.setAtivo(produto.getAtivo());
        produtoExistente.setUnidadeMedida(produto.getUnidadeMedida());
        produtoExistente.setCategoria(categoria);
        produtoExistente.setFornecedor(fornecedor);

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