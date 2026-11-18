package com.curso.alsports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração temporária da recuperação operacional do administrador.
 *
 * <p>O mecanismo é desabilitado por padrão. {@code login} e {@code confirm}
 * protegem contra acionamento acidental; não são credenciais de autenticação.
 * Nenhum valor é registrado em log.</p>
 */
@ConfigurationProperties(prefix = "app.security.admin-recovery")
public record AdministradorRecuperacaoProperties(
        boolean enabled,
        String login,
        String confirm) {
}
