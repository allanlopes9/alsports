package com.curso.alsports.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoEmUsoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Status;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.ProdutoRepository;

@Service
public class CategoriaProdutoService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
        "id",
        "nome",
        "ativo"
    );

    private final CategoriaProdutoRepository repository;
    private final ProdutoRepository produtoRepository;

    public CategoriaProdutoService(
        CategoriaProdutoRepository repository,
        ProdutoRepository produtoRepository) {
        this.repository = repository;
        this.produtoRepository = produtoRepository;
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
    public Page<CategoriaProduto> pesquisar(
        String nome,
        Boolean ativo,
        Pageable pageable) {

        validarOrdenacao(pageable);

        Specification<CategoriaProduto> filtros =
            (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (nome != null && !nome.isBlank()) {
            String trecho = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";

            filtros = filtros.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("nome")), trecho));
        }

        if (ativo != null) {
            filtros = filtros.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("ativo"), ativo));
        }

        return repository.findAll(filtros, pageable);
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
    public CategoriaProduto buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria não encontrada: " + id));
    }

    @Transactional
    public CategoriaProduto alterar(Long id, String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                "Nome da categoria é obrigatório");
        }

        String nomeNormalizado = nome.trim();

        if (repository.existsByNomeIgnoreCaseAndIdNot(nomeNormalizado, id)) {
            throw new RecursoDuplicadoException(
                "Já existe uma categoria com o nome: " + nomeNormalizado);
        }

        CategoriaProduto categoria = buscarPorId(id);
        categoria.alterarNome(nomeNormalizado);

        return categoria;
    }

    @Transactional
    public CategoriaProduto alterarStatus(Long id, Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status obrigatório");
        }

        CategoriaProduto categoria = buscarPorId(id);

        if (status == Status.ATIVO) {
            categoria.ativar();
        } else {
            categoria.inativar();
        }

        return categoria;
    }

    @Transactional
    public boolean excluir(Long id) {
        CategoriaProduto categoria = repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria não encontrada: " + id));

        if (produtoRepository.existsByCategoriaId(categoria.getId())) {
            throw new RecursoEmUsoException(
                "Não é possível excluir uma categoria que possui produtos.");
        }

        repository.delete(categoria);
        repository.flush();

        return true;
    }
}
