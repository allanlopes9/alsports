package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.curso.alsports.model.ContaAcesso;

public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, Long> {

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByPessoaId(Long pessoaId);

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    Optional<ContaAcesso> findByLoginIgnoreCase(String login);
}
