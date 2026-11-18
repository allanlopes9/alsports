package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.curso.alsports.service.ProtecaoAutenticacaoService;

class ProtecaoAutenticacaoTest {

    private static final String HASH = "a".repeat(64);
    private static final Instant INICIO =
            Instant.parse("2026-11-17T12:00:00Z");
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    @Test
    void quintaFalhaDentroDaJanelaDeveAtivarBloqueioUmaUnicaVez() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(HASH);

        assertThat(protecao.registrarFalha(INICIO, 5, JANELA, BLOQUEIO)).isFalse();
        assertThat(protecao.registrarFalha(INICIO.plusSeconds(1), 5, JANELA, BLOQUEIO)).isFalse();
        assertThat(protecao.registrarFalha(INICIO.plusSeconds(2), 5, JANELA, BLOQUEIO)).isFalse();
        assertThat(protecao.registrarFalha(INICIO.plusSeconds(3), 5, JANELA, BLOQUEIO)).isFalse();
        assertThat(protecao.registrarFalha(INICIO.plusSeconds(4), 5, JANELA, BLOQUEIO)).isTrue();

        assertThat(protecao.getFalhasConsecutivas()).isEqualTo(5);
        assertThat(protecao.estaBloqueada(INICIO.plusSeconds(4))).isTrue();
        assertThat(protecao.estaBloqueada(INICIO.plusSeconds(4).plus(BLOQUEIO))).isFalse();
    }

    @Test
    void falhaNoLimiteDaJanelaDeveReiniciarContador() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(HASH);
        for (int i = 0; i < 4; i++) {
            assertThat(protecao.registrarFalha(INICIO.plusSeconds(i), 5, JANELA, BLOQUEIO))
                    .isFalse();
        }

        Instant limite = INICIO.plus(JANELA);
        assertThat(protecao.registrarFalha(limite, 5, JANELA, BLOQUEIO)).isFalse();

        assertThat(protecao.getFalhasConsecutivas()).isEqualTo(1);
        assertThat(protecao.getJanelaIniciadaEm()).isEqualTo(limite);
        assertThat(protecao.getBloqueadaAte()).isNull();
    }

    @Test
    void sucessoDeveLimparContadorJanelaEBloqueio() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(HASH);
        for (int i = 0; i < 5; i++) {
            protecao.registrarFalha(INICIO.plusSeconds(i), 5, JANELA, BLOQUEIO);
        }
        assertThat(protecao.estaBloqueada(INICIO.plusSeconds(5))).isTrue();

        protecao.registrarSucesso();

        assertThat(protecao.getFalhasConsecutivas()).isZero();
        assertThat(protecao.getJanelaIniciadaEm()).isNull();
        assertThat(protecao.getBloqueadaAte()).isNull();
        assertThat(protecao.estaBloqueada(INICIO.plusSeconds(5))).isFalse();
    }

    @Test
    void construtorDeveRejeitarHashForaDoFormatoHexadecimal() {
        assertThatThrownBy(() -> new ProtecaoAutenticacao(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProtecaoAutenticacao("ABC"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProtecaoAutenticacao("g".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarFalhaDeveRejeitarPoliticaInvalida() {
        ProtecaoAutenticacao protecao = new ProtecaoAutenticacao(HASH);

        assertThatThrownBy(() -> protecao.registrarFalha(INICIO, 0, JANELA, BLOQUEIO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> protecao.registrarFalha(INICIO, 5, null, BLOQUEIO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> protecao.registrarFalha(INICIO, 5, Duration.ZERO, BLOQUEIO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> protecao.registrarFalha(INICIO, 5, JANELA, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hashDoLoginDeveSerSha256HexadecimalDeTextoNormalizado() throws Exception {
        String esperado = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest("usuario.2026".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(ProtecaoAutenticacaoService.calcularHashLogin("  USUARIO.2026  "))
                .isEqualTo(esperado)
                .matches("[0-9a-f]{64}");
        assertThatThrownBy(() -> ProtecaoAutenticacaoService.calcularHashLogin("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
