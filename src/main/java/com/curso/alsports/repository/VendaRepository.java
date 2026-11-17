package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.curso.alsports.model.Venda;

public interface VendaRepository extends
        JpaRepository<Venda, Long>,
        JpaSpecificationExecutor<Venda> {

    @EntityGraph(attributePaths = {
        "cliente.pessoa",
        "vendedor.pessoa",
        "itens.produto"
    })
    Optional<Venda> findOneById(Long id);

    @EntityGraph(attributePaths = {
        "cliente.pessoa",
        "vendedor.pessoa"
    })
    @Override
    Page<Venda> findAll(
            Specification<Venda> specification,
            Pageable pageable);
}
