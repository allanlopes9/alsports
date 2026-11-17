package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.StatusContaAcesso;

public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, Long> {

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByPessoaId(Long pessoaId);

    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    Optional<ContaAcesso> findByLoginIgnoreCase(String login);

    /**
     * Carrega a conta com pessoa e perfis para que a representação pública
     * possa ser montada fora da transação, sem depender de open-in-view.
     */
    @Override
    @EntityGraph(attributePaths = {"pessoa", "perfis"})
    Optional<ContaAcesso> findById(Long id);

    /**
     * Conta administradores ativos. A consulta considera o status da conta,
     * não o status da pessoa: a invariante protege a existência de uma conta
     * administrativa capaz de autenticar.
     */
    @Query("""
            select count(distinct conta)
            from ContaAcesso conta
            join conta.perfis perfil
            where perfil = :perfil
              and conta.status = :status
            """)
    long contarPorPerfilEStatus(
            @Param("perfil") PerfilAcesso perfil,
            @Param("status") StatusContaAcesso status);
}
