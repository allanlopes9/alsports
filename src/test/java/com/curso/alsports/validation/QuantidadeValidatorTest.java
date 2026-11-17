package com.curso.alsports.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class QuantidadeValidatorTest {

    @Test
    void deveAceitarQuantidadeInteira() {
        assertThat(QuantidadeValidator.exigirPositiva(
                new BigDecimal("10"), "mensagem"))
                .isEqualByComparingTo("10.000");
    }

    @Test
    void deveAceitarQuantidadeFracionadaComTresCasas() {
        assertThat(QuantidadeValidator.exigirPositiva(
                new BigDecimal("1.500"), "mensagem"))
                .isEqualByComparingTo("1.500");
    }

    @Test
    void deveAceitarQuantidadeMinimaPositiva() {
        assertThat(QuantidadeValidator.exigirPositiva(
                new BigDecimal("0.001"), "mensagem"))
                .isEqualByComparingTo("0.001");
    }

    @Test
    void deveAceitarZeroQuandoNaoNegativa() {
        assertThat(QuantidadeValidator.exigirNaoNegativa(
                BigDecimal.ZERO, "mensagem"))
                .isEqualByComparingTo("0.000");
    }

    @Test
    void deveRejeitarZeroQuandoPositiva() {
        assertThatThrownBy(() -> QuantidadeValidator.exigirPositiva(
                BigDecimal.ZERO, "Quantidade do item deve ser positiva"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade do item deve ser positiva");
    }

    @Test
    void deveRejeitarQuantidadeNegativa() {
        assertThatThrownBy(() -> QuantidadeValidator.exigirNaoNegativa(
                new BigDecimal("-1.000"), "Quantidade não pode ser negativa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade não pode ser negativa");
    }

    @Test
    void deveRejeitarMaisDeTresCasasDecimais() {
        assertThatThrownBy(() -> QuantidadeValidator.exigirPositiva(
                new BigDecimal("1.0001"), "mensagem"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A quantidade deve ter no máximo 3 casas decimais");
    }

    @Test
    void deveRejeitarValorAcimaDaPrecisao() {
        assertThatThrownBy(() -> QuantidadeValidator.exigirPositiva(
                new BigDecimal("1000000000000000.000"), "mensagem"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "A quantidade excede o limite permitido para o estoque");
    }

    @Test
    void deveAceitarValorMaximoDaPrecisao() {
        assertThat(QuantidadeValidator.exigirPositiva(
                QuantidadeValidator.MAXIMO, "mensagem"))
                .isEqualByComparingTo("999999999999999.999");
    }
}
