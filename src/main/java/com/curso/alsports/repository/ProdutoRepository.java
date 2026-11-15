package com.curso.alsports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.curso.alsports.model.Produto;

public interface ProdutoRepository extends
        JpaRepository<Produto, Long>,
        JpaSpecificationExecutor<Produto> {

    @EntityGraph(attributePaths = {"categoria", "fornecedor"})
    @Override
    List<Produto> findAll();

    @EntityGraph(attributePaths = {"categoria", "fornecedor"})
    @Override
    Optional<Produto> findById(Long id);

    @EntityGraph(attributePaths = {"categoria", "fornecedor"})
    @Override
    Page<Produto> findAll(
            Specification<Produto> specification,
            Pageable pageable);

    Optional<Produto> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByCodigoBarras(String codigoBarras);

    List<Produto> findByCategoriaId(Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);
}