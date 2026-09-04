package com.curso.alsports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.alsports.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @EntityGraph(attributePaths = {"categoria", "fornecedor"})
    @Override
    List<Produto> findAll();

    @EntityGraph(attributePaths = {"categoria", "fornecedor"})
    @Override
    Optional<Produto> findById(Long id);

    Optional<Produto> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);
}