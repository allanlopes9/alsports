package com.curso.alsports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.curso.alsports.model.Pessoa;

public interface PessoaRepository extends
        JpaRepository<Pessoa, Long>,
        JpaSpecificationExecutor<Pessoa> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
