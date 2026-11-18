package com.curso.alsports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.curso.alsports.model.SessaoRefreshToken;

import jakarta.persistence.LockModeType;

/**
 * Persistência das sessões de refresh token.
 */
public interface SessaoRefreshTokenRepository
        extends JpaRepository<SessaoRefreshToken, Long> {

    /**
     * Localiza a sessão pelo hash e bloqueia a linha para atualização.
     *
     * <p>O lock pessimista impede que duas requisições simultâneas usem a
     * mesma geração para criar dois sucessores válidos. Os relacionamentos
     * necessários à validação são carregados no mesmo SELECT para evitar
     * LazyInitializationException.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sessao
            from SessaoRefreshToken sessao
            join fetch sessao.conta conta
            join fetch conta.pessoa
            where sessao.tokenHash = :tokenHash
            """)
    Optional<SessaoRefreshToken> buscarPorHashParaAtualizacao(
            @Param("tokenHash") String tokenHash);

    List<SessaoRefreshToken> findByFamiliaId(String familiaId);

    List<SessaoRefreshToken> findByContaId(Long contaId);
}
