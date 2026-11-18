package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class EventoSegurancaTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T12:00:00Z");
    private static final String HASH = "b".repeat(64);

    @Test
    void eventoValidoDeveConterApenasMetadadosEstruturados() {
        EventoSeguranca evento = EventoSeguranca.criar(
                TipoEventoSeguranca.LOGIN_FALHA,
                AGORA,
                null,
                42L,
                HASH,
                "127.0.0.1");

        assertThat(evento.getTipo()).isEqualTo(TipoEventoSeguranca.LOGIN_FALHA);
        assertThat(evento.getOcorridoEm()).isEqualTo(AGORA);
        assertThat(evento.getAlvoContaId()).isEqualTo(42L);
        assertThat(evento.getIdentificadorHash()).isEqualTo(HASH);
        assertThat(evento.getEnderecoOrigem()).isEqualTo("127.0.0.1");
        assertThat(evento.toString())
                .contains("LOGIN_FALHA", "127.0.0.1")
                .doesNotContain(HASH, "senha", "token", "Authorization");
    }

    @Test
    void naoDeveExistirCampoDeMensagemLivre() {
        assertThat(Arrays.stream(EventoSeguranca.class.getDeclaredFields())
                .map(field -> field.getName().toLowerCase())
                .anyMatch(nome -> nome.contains("mensagem")))
                .isFalse();
    }

    @Test
    void deveRejeitarTipoOuInstanteAusentes() {
        assertThatThrownBy(() -> EventoSeguranca.criar(
                null, AGORA, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventoSeguranca.criar(
                TipoEventoSeguranca.LOGIN_SUCESSO, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarIdentificadoresDeContaInvalidos() {
        assertThatThrownBy(() -> EventoSeguranca.criar(
                TipoEventoSeguranca.LOGOUT, AGORA, 0L, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventoSeguranca.criar(
                TipoEventoSeguranca.LOGOUT, AGORA, null, -1L, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarHashForaDoFormato() {
        assertThatThrownBy(() -> EventoSeguranca.criar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA, null, null, "abc", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> EventoSeguranca.criar(
                TipoEventoSeguranca.LOGIN_FALHA, AGORA, null, null, HASH, null))
                .doesNotThrowAnyException();
    }

    @Test
    void origemDeveRejeitarQuebraDeLinhaEMaisDe45Caracteres() {
        assertThatThrownBy(() -> EventoSeguranca.validarOrigem("127.0.0.1\r\nX"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventoSeguranca.validarOrigem("x".repeat(46)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> EventoSeguranca.validarOrigem("x".repeat(45)))
                .doesNotThrowAnyException();
    }
}
