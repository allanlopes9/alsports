package com.curso.alsports.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do emissor e do Resource Server JWT.
 *
 * <p>O segredo é lido da variável de ambiente {@code JWT_SECRET}, nunca do
 * código-fonte ou de arquivos versionados. A validação estrutural ocorre na
 * inicialização para impedir que a aplicação suba com uma configuração
 * incompleta.</p>
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        Duration expiration,
        String secret) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.issuer é obrigatório");
        }

        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException(
                    "app.security.jwt.expiration deve ser uma duração positiva");
        }

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET é obrigatória para assinar e validar tokens");
        }
    }
}
