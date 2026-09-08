package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.alsports.model.CategoriaProduto;

public interface CategoriaProdutoRepository extends JpaRepository<CategoriaProduto, Long> {

    Optional<CategoriaProduto> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);
}
