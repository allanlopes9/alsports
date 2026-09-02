package com.curso.alsports.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.alsports.model.Fornecedor;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    boolean existsByCnpj(String cnpj);
}