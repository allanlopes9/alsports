package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.curso.alsports.model.CategoriaProduto;

public interface CategoriaProdutoRepository extends
        JpaRepository<CategoriaProduto, Long>,
        JpaSpecificationExecutor<CategoriaProduto> {

    Optional<CategoriaProduto> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
