package com.curso.alsports.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.alsports.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
