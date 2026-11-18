package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.MotivoRevogacaoRefreshToken;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.SessaoRefreshToken;

/**
 * Testes puros de domínio da sessão de refresh token.
 *
 * <p>Não utilizam Spring, PostgreSQL nem sleep: todos os instantes são fixos.</p>
 */
class SessaoRefreshTokenTest {

    private static final Instant CRIACAO =
            Instant.parse("2026-11-17T12:00:00Z");
    private static final Duration IDLE = Duration.ofMinutes(30);
    private static final Duration ABSOLUTO = Duration.ofHours(8);

    private ContaAcesso conta;
    private String familiaId;
    private String hash;

    @BeforeEach
    void setUp() {
        Pessoa pessoa = new Pessoa(
                "Operadora Sessão",
                "sessao." + UUID.randomUUID() + "@example.com",
                "52998224725",
                java.time.LocalDate.of(1990, 1, 1));
        conta = new ContaAcesso(
                pessoa,
                "sessao." + UUID.randomUUID(),
                PasswordEncoderFactories.createDelegatingPasswordEncoder()
                        .encode("senha-didatica-2026"));
        familiaId = UUID.randomUUID().toString();
        hash = "a".repeat(64);
    }

    @Test
    void criacaoDeveRegistrarEstadoInicial() {
        SessaoRefreshToken sessao = nova();

        assertAll(
                () -> assertThat(sessao.getId()).isNull(),
                () -> assertThat(sessao.getConta()).isSameAs(conta),
                () -> assertThat(sessao.getFamiliaId()).isEqualTo(familiaId),
                () -> assertThat(sessao.getTokenHash()).isEqualTo(hash),
                () -> assertThat(sessao.getCriadaEm()).isEqualTo(CRIACAO),
                () -> assertThat(sessao.getUltimaAtividadeEm()).isEqualTo(CRIACAO),
                () -> assertThat(sessao.getExpiraEm())
                        .isEqualTo(CRIACAO.plus(ABSOLUTO)),
                () -> assertThat(sessao.getUtilizadaEm()).isNull(),
                () -> assertThat(sessao.getRevogadaEm()).isNull(),
                () -> assertThat(sessao.getMotivoRevogacao()).isNull(),
                () -> assertThat(sessao.podeRenovar(CRIACAO, IDLE)).isTrue(),
                () -> assertThat(sessao.toString()).doesNotContain(hash));
    }

    @Test
    void marcarUtilizadaDeveRegistrarInstanteERenovarAtividade() {
        SessaoRefreshToken sessao = nova();
        Instant uso = CRIACAO.plus(Duration.ofMinutes(5));

        sessao.marcarUtilizada(uso);

        assertAll(
                () -> assertThat(sessao.getUtilizadaEm()).isEqualTo(uso),
                () -> assertThat(sessao.getUltimaAtividadeEm()).isEqualTo(uso),
                () -> assertThat(sessao.getCriadaEm()).isEqualTo(CRIACAO),
                () -> assertThat(sessao.getFamiliaId()).isEqualTo(familiaId),
                () -> assertThat(sessao.getExpiraEm())
                        .isEqualTo(CRIACAO.plus(ABSOLUTO)),
                () -> assertThat(sessao.podeRenovar(uso, IDLE)).isFalse());
    }

    @Test
    void naoDevePermitirReutilizacaoDaMesmaGeracao() {
        SessaoRefreshToken sessao = nova();
        sessao.marcarUtilizada(CRIACAO.plusSeconds(1));

        assertThatThrownBy(() -> sessao.marcarUtilizada(CRIACAO.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Esta geração já foi utilizada");
    }

    @Test
    void revogacaoDeveRegistrarMotivoEImpedirRenovacao() {
        SessaoRefreshToken sessao = nova();
        Instant revogacao = CRIACAO.plusSeconds(1);

        sessao.revogar(revogacao, MotivoRevogacaoRefreshToken.REUTILIZACAO);

        assertAll(
                () -> assertThat(sessao.getRevogadaEm()).isEqualTo(revogacao),
                () -> assertThat(sessao.getMotivoRevogacao())
                        .isEqualTo(MotivoRevogacaoRefreshToken.REUTILIZACAO),
                () -> assertThat(sessao.podeRenovar(revogacao, IDLE)).isFalse());
    }

    @Test
    void naoDevePermitirRevogacaoDupla() {
        SessaoRefreshToken sessao = nova();
        sessao.revogar(CRIACAO, MotivoRevogacaoRefreshToken.LOGOUT);

        assertThatThrownBy(() -> sessao.revogar(
                CRIACAO.plusSeconds(1), MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Esta geração já está revogada");
    }

    @Test
    void marcarUtilizadaNaoDevePermitirGeracaoRevogada() {
        SessaoRefreshToken sessao = nova();
        sessao.revogar(CRIACAO, MotivoRevogacaoRefreshToken.LOGOUT);

        assertThatThrownBy(() -> sessao.marcarUtilizada(CRIACAO.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Esta geração está revogada");
    }

    @ParameterizedTest
    @CsvSource({
        "PT29M, false",
        "PT30M, true"
    })
    void inatividadeDeveExpirarExatamenteNoLimite(
            String duration, boolean expirada) {

        SessaoRefreshToken sessao = nova();
        Instant instante = CRIACAO.plus(Duration.parse(duration));

        assertThat(sessao.expirouPorInatividade(instante, IDLE))
                .isEqualTo(expirada);
    }

    @Test
    void atividadeRecenteDeveDeslocarOLimiteDeInatividade() {
        SessaoRefreshToken sessao = nova();
        Instant uso = CRIACAO.plus(Duration.ofMinutes(20));
        sessao.marcarUtilizada(uso);

        assertAll(
                () -> assertThat(sessao.expirouPorInatividade(
                        CRIACAO.plus(Duration.ofMinutes(31)), IDLE)).isFalse(),
                () -> assertThat(sessao.expirouPorInatividade(
                        uso.plus(Duration.ofMinutes(30)), IDLE)).isTrue());
    }

    @Test
    void expiracaoAbsolutaDeveAtingirLimiteExato() {
        SessaoRefreshToken sessao = nova();
        Instant limite = CRIACAO.plus(ABSOLUTO);

        assertAll(
                () -> assertThat(sessao.expirouPorLimiteAbsoluto(
                        limite.minusNanos(1))).isFalse(),
                () -> assertThat(sessao.expirouPorLimiteAbsoluto(limite)).isTrue(),
                () -> assertThat(sessao.expirouPorLimiteAbsoluto(
                        limite.plusSeconds(1))).isTrue());
    }

    @Test
    void naoDevePermitirMarcarUtilizadaAposLimiteAbsoluto() {
        SessaoRefreshToken sessao = nova();

        assertThatThrownBy(() -> sessao.marcarUtilizada(
                CRIACAO.plus(ABSOLUTO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Esta geração atingiu a expiração absoluta");
    }

    @Test
    void expiracaoAbsolutaNaoDeveSerDeslocadaPorRotacao() {
        SessaoRefreshToken sessao = nova();
        Instant uso = CRIACAO.plus(Duration.ofHours(7));
        sessao.marcarUtilizada(uso);

        assertThat(sessao.getExpiraEm()).isEqualTo(CRIACAO.plus(ABSOLUTO));
        assertThat(sessao.expirouPorLimiteAbsoluto(
                CRIACAO.plus(ABSOLUTO))).isTrue();
    }

    @ParameterizedTest
    @CsvSource(value = {"''", "'   '"})
    void familiaIdObrigatorio(String valor) {
        assertThatThrownBy(() -> SessaoRefreshToken.nova(
                conta, valor, hash, CRIACAO, CRIACAO.plus(ABSOLUTO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Família é obrigatório");
    }

    @Test
    void tokenHashObrigatorio() {
        assertThatThrownBy(() -> SessaoRefreshToken.nova(
                conta, familiaId, null, CRIACAO, CRIACAO.plus(ABSOLUTO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hash do token é obrigatório");
    }

    @Test
    void expiraçãoDeveSerPosteriorACriacao() {
        assertThatThrownBy(() -> SessaoRefreshToken.nova(
                conta, familiaId, hash, CRIACAO, CRIACAO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A expiração absoluta deve ser posterior à criação");
    }

    private SessaoRefreshToken nova() {
        return SessaoRefreshToken.nova(
                conta, familiaId, hash, CRIACAO, CRIACAO.plus(ABSOLUTO));
    }
}
