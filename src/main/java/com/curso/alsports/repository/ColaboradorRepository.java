package com.curso.alsports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.curso.alsports.model.Colaborador;

public interface ColaboradorRepository extends
        JpaRepository<Colaborador, Long>,
        JpaSpecificationExecutor<Colaborador> {

    boolean existsByPessoaId(Long pessoaId);

    boolean existsByMatricula(String matricula);

    boolean existsByMatriculaAndIdNot(String matricula, Long id);

    @EntityGraph(attributePaths = "pessoa")
    Optional<Colaborador> findOneById(Long id);

    @EntityGraph(attributePaths = "pessoa")
    @Override
    Optional<Colaborador> findById(Long id);

    @EntityGraph(attributePaths = "pessoa")
    @Override
    List<Colaborador> findAll();

    @EntityGraph(attributePaths = "pessoa")
    @Override
    Page<Colaborador> findAll(
            Specification<Colaborador> specification,
            Pageable pageable);
}
