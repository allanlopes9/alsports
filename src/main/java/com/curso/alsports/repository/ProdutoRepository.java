package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.alsports.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);
}