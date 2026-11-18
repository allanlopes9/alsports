package com.curso.alsports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Status;
import com.curso.alsports.model.StatusContaAcesso;

import jakarta.persistence.LockModeType;

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
     * Conta administradores ativos pelo status da conta. Mantido para o seed,
     * que é creation-only e não deve criar uma segunda conta quando a pessoa
     * do administrador existente estiver inativa.
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

    /**
     * Conta somente administradores capazes de autenticar: conta ativa e
     * pessoa ativa. Usada pela invariante do último administrador.
     */
    @Query("""
            select count(distinct conta)
            from ContaAcesso conta
            join conta.perfis perfil
            join conta.pessoa pessoa
            where perfil = :perfil
              and conta.status = :status
              and pessoa.status = :pessoaStatus
            """)
    long contarAdministradoresEfetivos(
            @Param("perfil") PerfilAcesso perfil,
            @Param("status") StatusContaAcesso status,
            @Param("pessoaStatus") Status pessoaStatus);

    /**
     * Descoberta de IDs sem carregar entidades. Usada antes da aquisição
     * ordenada de locks.
     */
    @Query(value = """
            select ca.id
            from conta_acesso ca
            where ca.pessoa_id = :pessoaId
            order by ca.id
            """, nativeQuery = true)
    List<Long> buscarIdsPorPessoaId(@Param("pessoaId") Long pessoaId);

    @Query(value = """
            select distinct ca.id
            from conta_acesso ca
            join conta_acesso_perfil perfil
              on perfil.conta_acesso_id = ca.id
            where perfil.perfil = 'ADMINISTRADOR'
            order by ca.id
            """, nativeQuery = true)
    List<Long> buscarIdsAdministradores();

    @Query(value = """
            select ca.id
            from conta_acesso ca
            where lower(btrim(ca.login)) = lower(btrim(:login))
            """, nativeQuery = true)
    Long buscarIdPorLogin(@Param("login") String login);

    /**
     * Leitura escalar da geração antes da autenticação. Não carrega a entidade
     * nem adquire lock.
     */
    @Query(value = """
            select ca.sessao_geracao
            from conta_acesso ca
            where lower(btrim(ca.login)) = lower(btrim(:login))
            """, nativeQuery = true)
    Long buscarGeracaoPorLogin(@Param("login") String login);

    /**
     * Aquisição do lock da conta. Não faz fetch de pessoa nem de perfis.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select conta
            from ContaAcesso conta
            where conta.id = :id
            """)
    Optional<ContaAcesso> buscarPorIdParaAtualizacao(@Param("id") Long id);
}
