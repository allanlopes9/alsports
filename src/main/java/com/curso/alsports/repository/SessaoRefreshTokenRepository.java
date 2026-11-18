package com.curso.alsports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.curso.alsports.model.SessaoRefreshToken;

/**
 * Persistência das sessões de refresh token.
 *
 * <p>Os locks de linha são explícitos ({@code FOR UPDATE OF s}) para não
 * bloquear joins de conta ou pessoa. A conta já é bloqueada pelo protocolo do
 * serviço antes de qualquer lock de sessão.</p>
 */
public interface SessaoRefreshTokenRepository
        extends JpaRepository<SessaoRefreshToken, Long> {

    /**
     * Localiza a sessão pelo hash e bloqueia somente a linha da sessão.
     * Mantido como API de lock explícita para os fluxos de refresh/logout e
     * para os testes de concorrência.
     */
    @Query(value = """
            select s.*
            from sessao_refresh_token s
            where s.token_hash = :tokenHash
            for update of s
            """, nativeQuery = true)
    Optional<SessaoRefreshToken> buscarPorHashParaAtualizacao(
            @Param("tokenHash") String tokenHash);

    @Query(value = """
            select s.id
            from sessao_refresh_token s
            where s.token_hash = :tokenHash
            """, nativeQuery = true)
    Long buscarIdPorHash(@Param("tokenHash") String tokenHash);

    @Query(value = """
            select s.conta_acesso_id
            from sessao_refresh_token s
            where s.token_hash = :tokenHash
            """, nativeQuery = true)
    Long buscarContaIdPorHash(@Param("tokenHash") String tokenHash);

    @Query(value = """
            select s.id
            from sessao_refresh_token s
            where s.familia_id = :familiaId
            order by s.id
            """, nativeQuery = true)
    List<Long> buscarIdsPorFamiliaId(@Param("familiaId") String familiaId);

    @Query(value = """
            select s.id
            from sessao_refresh_token s
            where s.conta_acesso_id = :contaId
              and s.revogada_em is null
            order by s.id
            """, nativeQuery = true)
    List<Long> buscarIdsAtivasPorContaId(@Param("contaId") Long contaId);

    @Query(value = """
            select s.*
            from sessao_refresh_token s
            where s.id = :id
            for update of s
            """, nativeQuery = true)
    Optional<SessaoRefreshToken> buscarPorIdParaAtualizacao(@Param("id") Long id);

    List<SessaoRefreshToken> findByFamiliaId(String familiaId);

    List<SessaoRefreshToken> findByContaId(Long contaId);
}
