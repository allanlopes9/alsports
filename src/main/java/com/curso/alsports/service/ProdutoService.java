package com.curso.alsports.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Fornecedor;
import com.curso.alsports.model.Produto;
import com.curso.alsports.model.Status;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.FornecedorRepository;
import com.curso.alsports.repository.ProdutoRepository;
import com.curso.alsports.repository.ProdutoSpecification;

@Service
public class ProdutoService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
        "id",
        "codigoBarras",
        "nome",
        "quantidade",
        "estoqueMinimo",
        "preco",
        "dataCadastro",
        "ativo",
        "unidadeMedida"
    );

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
    public Page<Produto> pesquisar(
        String nome,
        Boolean ativo,
        Long categoriaId,
        Long fornecedorId,
        Integer quantidadeMinima,
        Integer quantidadeMaxima,
        Boolean abaixoEstoqueMinimo,
        Pageable pageable) {

        validarOrdenacao(pageable);

        return repository.findAll(
            ProdutoSpecification.comFiltros(
                nome,
                ativo,
                categoriaId,
                fornecedorId,
                quantidadeMinima,
                quantidadeMaxima,
                abaixoEstoqueMinimo),
            pageable);
    }

    private void validarOrdenacao(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                "A paginação deve ser informada");
        }

        pageable.getSort().forEach(ordem -> {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw new IllegalArgumentException(
                    "Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
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
        String nome,
        BigDecimal estoqueMinimo,
        BigDecimal preco,
        Long categoriaId,
        Long fornecedorId) {

        Produto produtoExistente = repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto não encontrado: " + id));

        String nomeNormalizado = nome == null ? null : nome.trim();

        if (!produtoExistente.getNome().equalsIgnoreCase(nomeNormalizado)
            && repository.existsByNomeIgnoreCase(nomeNormalizado)) {

            throw new RecursoDuplicadoException(
                "Já existe um produto com o nome: " + nomeNormalizado);
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

        produtoExistente.alterarNome(nomeNormalizado);
        produtoExistente.alterarEstoqueMinimo(estoqueMinimo);
        produtoExistente.alterarPreco(preco);
        produtoExistente.alterarCategoria(categoria);
        produtoExistente.alterarFornecedor(fornecedor);

        return produtoExistente;
    }

    @Transactional
    public Produto receberEstoque(Long id, Integer quantidade) {
        Produto produto = buscarPorId(id);
        produto.receberEstoque(quantidade);
        return produto;
    }

    @Transactional
    public Produto retirarEstoque(Long id, Integer quantidade) {
        Produto produto = buscarPorId(id);
        produto.retirarEstoque(quantidade);
        return produto;
    }

    @Transactional
    public Produto alterarStatus(Long id, Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status obrigatório");
        }

        Produto produto = repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto não encontrado: " + id));

        if (status == Status.ATIVO) {
            produto.ativar();
        } else {
            produto.inativar();
        }

        return produto;
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
