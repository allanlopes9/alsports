package com.curso.alsports.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma geração de refresh token dentro de uma família de sessão.
 *
 * <p>O valor opaco do token nunca é persistido. Apenas o hash SHA-256 em
 * hexadecimal é armazenado em {@code tokenHash}. Registros já utilizados são
 * mantidos para que a reapresentação do token antigo seja reconhecida como
 * reutilização e revoque a família inteira.</p>
 */
@Entity
@Table(name = "sessao_refresh_token")
public class SessaoRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "conta_acesso_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sessao_refresh_token_conta"))
    private ContaAcesso conta;

    @Column(name = "familia_id", nullable = false, length = 36)
    private String familiaId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "ultima_atividade_em", nullable = false)
    private Instant ultimaAtividadeEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "utilizada_em")
    private Instant utilizadaEm;

    @Column(name = "revogada_em")
    private Instant revogadaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_revogacao", length = 30)
    private MotivoRevogacaoRefreshToken motivoRevogacao;

    protected SessaoRefreshToken() {
    }

    private SessaoRefreshToken(
            ContaAcesso conta,
            String familiaId,
            String tokenHash,
            Instant criadaEm,
            Instant expiraEm) {

        this.conta = Objects.requireNonNull(conta, "Conta de acesso é obrigatória");
        this.familiaId = validarTexto(familiaId, 36, "Família");
        this.tokenHash = validarTexto(tokenHash, 64, "Hash do token");
        this.criadaEm = Objects.requireNonNull(criadaEm, "Criação é obrigatória");
        this.ultimaAtividadeEm = criadaEm;
        this.expiraEm = Objects.requireNonNull(expiraEm, "Expiração é obrigatória");

        if (!expiraEm.isAfter(criadaEm)) {
            throw new IllegalArgumentException(
                    "A expiração absoluta deve ser posterior à criação");
        }
    }

    /**
     * Cria a primeira geração de uma família de sessão.
     */
    public static SessaoRefreshToken nova(
            ContaAcesso conta,
            String familiaId,
            String tokenHash,
            Instant criadaEm,
            Instant expiraEm) {

        return new SessaoRefreshToken(
                conta, familiaId, tokenHash, criadaEm, expiraEm);
    }

    /**
     * Registra o consumo desta geração e renova o marco de atividade.
     */
    public void marcarUtilizada(Instant agora) {
        Objects.requireNonNull(agora, "Instante é obrigatório");
        if (utilizadaEm != null) {
            throw new IllegalStateException("Esta geração já foi utilizada");
        }
        if (revogadaEm != null) {
            throw new IllegalStateException("Esta geração está revogada");
        }
        if (expirouPorLimiteAbsoluto(agora)) {
            throw new IllegalStateException(
                    "Esta geração atingiu a expiração absoluta");
        }

        this.utilizadaEm = agora;
        this.ultimaAtividadeEm = agora;
    }

    /**
     * Encerra esta geração com um motivo auditável.
     */
    public void revogar(Instant agora, MotivoRevogacaoRefreshToken motivo) {
        Objects.requireNonNull(agora, "Instante é obrigatório");
        Objects.requireNonNull(motivo, "Motivo da revogação é obrigatório");
        if (revogadaEm != null) {
            throw new IllegalStateException("Esta geração já está revogada");
        }

        this.revogadaEm = agora;
        this.motivoRevogacao = motivo;
    }

    /**
     * Inatividade usa uma comparação literal com o limite: no instante exato
     * em que o prazo é atingido a sessão já está expirada.
     */
    public boolean expirouPorInatividade(Instant agora, Duration idleTimeout) {
        Objects.requireNonNull(agora, "Instante é obrigatório");
        Objects.requireNonNull(idleTimeout, "Timeout de inatividade é obrigatório");
        return !agora.isBefore(ultimaAtividadeEm.plus(idleTimeout));
    }

    /**
     * O limite absoluto é herdado da família e não é deslocado pela rotação.
     */
    public boolean expirouPorLimiteAbsoluto(Instant agora) {
        Objects.requireNonNull(agora, "Instante é obrigatório");
        return !agora.isBefore(expiraEm);
    }

    /**
     * Indica se esta geração ainda pode ser usada para renovar a sessão.
     */
    public boolean podeRenovar(Instant agora, Duration idleTimeout) {
        return utilizadaEm == null
                && revogadaEm == null
                && !expirouPorInatividade(agora, idleTimeout)
                && !expirouPorLimiteAbsoluto(agora);
    }

    public Long getId() {
        return id;
    }

    public ContaAcesso getConta() {
        return conta;
    }

    public String getFamiliaId() {
        return familiaId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getUltimaAtividadeEm() {
        return ultimaAtividadeEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getUtilizadaEm() {
        return utilizadaEm;
    }

    public Instant getRevogadaEm() {
        return revogadaEm;
    }

    public MotivoRevogacaoRefreshToken getMotivoRevogacao() {
        return motivoRevogacao;
    }

    @Override
    public String toString() {
        return "SessaoRefreshToken[id=" + id
                + ", familiaId=" + familiaId
                + ", criadaEm=" + criadaEm
                + ", utilizadaEm=" + utilizadaEm
                + ", revogadaEm=" + revogadaEm
                + ", motivoRevogacao=" + motivoRevogacao + "]";
    }

    private static String validarTexto(String valor, int tamanho, String nome) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nome + " é obrigatório");
        }
        String texto = valor.trim();
        if (texto.length() > tamanho) {
            throw new IllegalArgumentException(
                    nome + " deve possuir no máximo " + tamanho + " caracteres");
        }
        return texto;
    }
}
