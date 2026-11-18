package com.curso.alsports.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RefreshTokenPropertiesTest {

    @Test
    void deveAceitarDuracoesValidas() {
        RefreshTokenProperties properties = new RefreshTokenProperties(
                Duration.ofMinutes(30), Duration.ofHours(8));

        assertAll(
                () -> assertThat(properties.idleTimeout())
                        .isEqualTo(Duration.ofMinutes(30)),
                () -> assertThat(properties.absoluteExpiration())
                        .isEqualTo(Duration.ofHours(8)));
    }

    @Test
    void idleTimeoutNaoPodeSerNuloOuNaoPositivo() {
        assertThatThrownBy(() -> new RefreshTokenProperties(
                null, Duration.ofHours(8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(
                Duration.ZERO, Duration.ofHours(8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(
                Duration.ofSeconds(-1), Duration.ofHours(8)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiracaoAbsolutaNaoPodeSerNulaNaoPositivaNemMenorQueInatividade() {
        assertThatThrownBy(() -> new RefreshTokenProperties(
                Duration.ofMinutes(30), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(
                Duration.ofMinutes(30), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(
                Duration.ofMinutes(30), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(
                Duration.ofMinutes(30), Duration.ofMinutes(30)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RefreshTokenProperties(
                Duration.ofMinutes(30), Duration.ofMinutes(29)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
