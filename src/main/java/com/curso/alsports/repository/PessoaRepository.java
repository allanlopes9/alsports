package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.curso.alsports.model.Pessoa;

import jakarta.persistence.LockModeType;

public interface PessoaRepository extends
        JpaRepository<Pessoa, Long>,
        JpaSpecificationExecutor<Pessoa> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Lock da pessoa depois do lock da conta, respeitando a hierarquia
     * conta -&gt; pessoa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pessoa
            from Pessoa pessoa
            where pessoa.id = :id
            """)
    Optional<Pessoa> buscarPorIdParaAtualizacao(@Param("id") Long id);
}
