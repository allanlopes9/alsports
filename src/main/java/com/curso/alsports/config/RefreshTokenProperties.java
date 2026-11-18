package com.curso.alsports.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Durações do refresh token, vinculadas ao prefixo
 * {@code app.security.refresh-token}.
 *
 * <p>Nenhum segredo é armazenado aqui: as duas durações são valores não
 * sensíveis e podem ser sobrescritas por variáveis de ambiente.</p>
 */
@ConfigurationProperties(prefix = "app.security.refresh-token")
public record RefreshTokenProperties(
        Duration idleTimeout,
        Duration absoluteExpiration) {

    public RefreshTokenProperties {
        if (idleTimeout == null || idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Idle timeout do refresh token deve ser positivo");
        }
        if (absoluteExpiration == null
                || absoluteExpiration.isZero()
                || absoluteExpiration.isNegative()
                || absoluteExpiration.compareTo(idleTimeout) <= 0) {
            throw new IllegalArgumentException(
                    "A expiração absoluta do refresh token deve ser maior que o idle timeout");
        }
    }
}
