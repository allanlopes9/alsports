package com.curso.alsports.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centraliza as regras de quantidade usadas por produtos, movimentações de
 * estoque e itens de venda.
 *
 * <p>A quantidade é armazenada como {@code NUMERIC(18,3)}: até 15 dígitos na
 * parte inteira e exatamente 3 casas decimais. A normalização usa
 * {@link RoundingMode#UNNECESSARY} para nunca arredondar valores recebidos.</p>
 */
public final class QuantidadeValidator {

    public static final int ESCALA = 3;
    public static final int DIGITOS_INTEIROS = 15;
    public static final int PRECISAO = DIGITOS_INTEIROS + ESCALA;

    public static final BigDecimal MINIMO_POSITIVO = new BigDecimal("0.001");
    public static final BigDecimal MAXIMO = new BigDecimal("999999999999999.999");

    public static final String MENSAGEM_ESCALA =
        "A quantidade deve ter no máximo 3 casas decimais";
    public static final String MENSAGEM_LIMITE =
        "A quantidade excede o limite permitido para o estoque";

    private QuantidadeValidator() {
    }

    public static BigDecimal exigirPositiva(
            BigDecimal quantidade,
            String mensagem) {

        BigDecimal normalizada = normalizar(quantidade, mensagem);

        if (normalizada.signum() <= 0) {
            throw new IllegalArgumentException(mensagem);
        }

        return normalizada;
    }

    public static BigDecimal exigirNaoNegativa(
            BigDecimal quantidade,
            String mensagem) {

        BigDecimal normalizada = normalizar(quantidade, mensagem);

        if (normalizada.signum() < 0) {
            throw new IllegalArgumentException(mensagem);
        }

        return normalizada;
    }

    public static BigDecimal normalizar(
            BigDecimal quantidade,
            String mensagem) {

        if (quantidade == null) {
            throw new IllegalArgumentException(mensagem);
        }

        if (quantidade.scale() > ESCALA) {
            throw new IllegalArgumentException(MENSAGEM_ESCALA);
        }

        if (quantidade.abs().compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException(MENSAGEM_LIMITE);
        }

        return quantidade.setScale(ESCALA, RoundingMode.UNNECESSARY);
    }
}
