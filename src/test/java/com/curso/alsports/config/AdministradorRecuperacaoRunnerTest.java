package com.curso.alsports.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;

class AdministradorRecuperacaoRunnerTest {

    @Test
    void recuperacaoDeveExecutarAntesDoSeedEImpedirSeedQuandoAtiva() {
        Order recuperacao = AdministradorRecuperacaoRunner.class
                .getAnnotation(Order.class);
        Order seed = AdministradorInicialRunner.class
                .getAnnotation(Order.class);

        assertThat(recuperacao).isNotNull();
        assertThat(seed).isNotNull();
        assertThat(recuperacao.value()).isEqualTo(100);
        assertThat(seed.value()).isEqualTo(200);
        assertThat(recuperacao.value()).isLessThan(seed.value());

        ConditionalOnProperty condicaoRecuperacao =
                AdministradorRecuperacaoRunner.class
                        .getAnnotation(ConditionalOnProperty.class);
        assertThat(condicaoRecuperacao).isNotNull();
        assertThat(condicaoRecuperacao.name())
                .containsExactly("app.security.admin-recovery.enabled");
        assertThat(condicaoRecuperacao.havingValue()).isEqualTo("true");

        ConditionalOnExpression condicaoSeed = AdministradorInicialRunner.class
                .getAnnotation(ConditionalOnExpression.class);
        assertThat(condicaoSeed).isNotNull();
        assertThat(condicaoSeed.value())
                .contains("app.security.admin-recovery.enabled")
                .contains("false");
    }
}
