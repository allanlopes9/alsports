package com.curso.alsports.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Política de limitação de tentativas on-line de autenticação.
 *
 * <p>A validação no construtor impede que a aplicação suba com uma política
 * indefinida. Os valores didáticos são 5 falhas, janela de 15 minutos e
 * bloqueio de 15 minutos.</p>
 */
@ConfigurationProperties(prefix = "app.security.authentication-protection")
public record ProtecaoAutenticacaoProperties(
        int maxFailedAttempts,
        Duration failureWindow,
        Duration lockDuration) {

    public ProtecaoAutenticacaoProperties {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException(
                    "Máximo de falhas deve ser positivo");
        }
        validarDuracao(failureWindow, "Janela de falhas deve ser positiva");
        validarDuracao(lockDuration, "Duração do bloqueio deve ser positiva");
    }

    private static void validarDuracao(Duration valor, String mensagem) {
        if (valor == null || valor.isZero() || valor.isNegative()) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
