package com.curso.alsports.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.curso.alsports.model.ProtecaoAutenticacao;

import jakarta.persistence.LockModeType;

public interface ProtecaoAutenticacaoRepository
        extends JpaRepository<ProtecaoAutenticacao, Long> {

    /**
     * Cria a linha inaugural de forma atômica. O upsert é específico do
     * PostgreSQL e evita a corrida de insert duplicado na primeira falha.
     */
    @Modifying
    @Query(value = """
            INSERT INTO protecao_autenticacao
                (chave_login, falhas_consecutivas)
            VALUES (:chaveLogin, 0)
            ON CONFLICT (chave_login) DO NOTHING
            """, nativeQuery = true)
    void criarSeNecessario(@Param("chaveLogin") String chaveLogin);

    /**
     * Busca sob lock pessimista para que duas falhas simultâneas não percam
     * atualização. Deve ser chamada após o upsert e dentro da transação que
     * incrementa o contador.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select protecao
            from ProtecaoAutenticacao protecao
            where protecao.chaveLogin = :chaveLogin
            """)
    Optional<ProtecaoAutenticacao> buscarParaAtualizacao(
            @Param("chaveLogin") String chaveLogin);

    Optional<ProtecaoAutenticacao> findByChaveLogin(String chaveLogin);
}
