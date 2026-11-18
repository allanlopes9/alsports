package com.curso.alsports.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ProtecaoAutenticacaoPropertiesTest {

    @Test
    void valoresDidaticosDevemSerAceitos() {
        ProtecaoAutenticacaoProperties properties =
                new ProtecaoAutenticacaoProperties(5,
                        Duration.ofMinutes(15), Duration.ofMinutes(15));

        assertThat(properties.maxFailedAttempts()).isEqualTo(5);
        assertThat(properties.failureWindow()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.lockDuration()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void maximumDeFalhasDeveSerPositivo() {
        assertThatThrownBy(() -> new ProtecaoAutenticacaoProperties(
                0, Duration.ofMinutes(15), Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duracoesDevemSerPositivas() {
        assertThatThrownBy(() -> new ProtecaoAutenticacaoProperties(
                5, null, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProtecaoAutenticacaoProperties(
                5, Duration.ZERO, Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProtecaoAutenticacaoProperties(
                5, Duration.ofMinutes(15), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new ProtecaoAutenticacaoProperties(
                1, Duration.ofSeconds(1), Duration.ofSeconds(1)))
                .doesNotThrowAnyException();
    }
}
