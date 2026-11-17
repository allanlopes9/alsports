package com.curso.alsports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do provisionamento da primeira conta administrativa.
 *
 * <p>Nenhum valor possui senha padrão no código. Quando a flag está ativa,
 * todas as demais propriedades são validadas pelo service antes de qualquer
 * gravação. O mecanismo permanece desativado por padrão.</p>
 */
@ConfigurationProperties(prefix = "app.security.initial-admin")
public record AdministradorInicialProperties(
        boolean enabled,
        String nome,
        String email,
        String cpf,
        String login,
        String senha) {
}
