package com.curso.alsports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.curso.alsports.model.Cliente;

public interface ClienteRepository extends
        JpaRepository<Cliente, Long>,
        JpaSpecificationExecutor<Cliente> {

    boolean existsByPessoaId(Long pessoaId);

    @EntityGraph(attributePaths = "pessoa")
    Optional<Cliente> findOneById(Long id);

    @EntityGraph(attributePaths = "pessoa")
    @Override
    Optional<Cliente> findById(Long id);

    @EntityGraph(attributePaths = "pessoa")
    @Override
    List<Cliente> findAll();

    @EntityGraph(attributePaths = "pessoa")
    @Override
    Page<Cliente> findAll(
            Specification<Cliente> specification,
            Pageable pageable);
}
