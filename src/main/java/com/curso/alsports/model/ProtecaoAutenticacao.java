package com.curso.alsports.model;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Estado persistente da proteção on-line de um identificador de login.
 *
 * <p>A chave é o SHA-256 hexadecimal do login normalizado; o texto original
 * nunca é gravado. O bloqueio representado aqui é automático e temporário,
 * distinto de {@link StatusContaAcesso#BLOQUEADA}, que é uma decisão
 * administrativa persistente.</p>
 */
@Entity
@Table(name = "protecao_autenticacao")
public class ProtecaoAutenticacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_login", nullable = false, unique = true, length = 64)
    private String chaveLogin;

    @Column(name = "falhas_consecutivas", nullable = false)
    private int falhasConsecutivas;

    @Column(name = "janela_iniciada_em")
    private Instant janelaIniciadaEm;

    @Column(name = "bloqueada_ate")
    private Instant bloqueadaAte;

    protected ProtecaoAutenticacao() {
        // JPA
    }

    public ProtecaoAutenticacao(String chaveLogin) {
        if (chaveLogin == null || !chaveLogin.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Hash do login deve possuir 64 caracteres hexadecimais");
        }
        this.chaveLogin = chaveLogin;
        this.falhasConsecutivas = 0;
    }

    public Long getId() {
        return id;
    }

    public String getChaveLogin() {
        return chaveLogin;
    }

    public int getFalhasConsecutivas() {
        return falhasConsecutivas;
    }

    public Instant getJanelaIniciadaEm() {
        return janelaIniciadaEm;
    }

    public Instant getBloqueadaAte() {
        return bloqueadaAte;
    }

    /**
     * Registra uma falha e devolve {@code true} somente quando esta falha
     * ativou o bloqueio, para que um único evento de ativação seja emitido.
     */
    public boolean registrarFalha(
            Instant agora,
            int maximoTentativas,
            Duration janela,
            Duration duracaoBloqueio) {

        if (maximoTentativas < 1) {
            throw new IllegalArgumentException(
                    "Máximo de falhas deve ser positivo");
        }
        if (janela == null || janela.isZero() || janela.isNegative()) {
            throw new IllegalArgumentException("Janela deve ser positiva");
        }
        if (duracaoBloqueio == null
                || duracaoBloqueio.isZero()
                || duracaoBloqueio.isNegative()) {
            throw new IllegalArgumentException(
                    "Duração do bloqueio deve ser positiva");
        }

        if (janelaIniciadaEm == null
                || !agora.isBefore(janelaIniciadaEm.plus(janela))) {
            falhasConsecutivas = 0;
            janelaIniciadaEm = agora;
            bloqueadaAte = null;
        }

        falhasConsecutivas++;
        if (falhasConsecutivas >= maximoTentativas) {
            bloqueadaAte = agora.plus(duracaoBloqueio);
            return true;
        }
        return false;
    }

    public boolean estaBloqueada(Instant agora) {
        return bloqueadaAte != null && agora.isBefore(bloqueadaAte);
    }

    public void registrarSucesso() {
        falhasConsecutivas = 0;
        janelaIniciadaEm = null;
        bloqueadaAte = null;
    }
}
