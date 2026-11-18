package com.curso.alsports.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.service.AdministradorInicialService;

/**
 * Executa o provisionamento apenas quando a flag está explicitamente ativa.
 *
 * <p>O log registra somente o login e o resultado. A senha aberta, o CPF e o
 * hash nunca são registrados.</p>
 */
@Component
@Order(200)
@ConditionalOnProperty(
        name = "app.security.initial-admin.enabled",
        havingValue = "true")
@ConditionalOnExpression(
        "'${app.security.admin-recovery.enabled:false}' == 'false'")
public class AdministradorInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            AdministradorInicialRunner.class);

    private final AdministradorInicialService service;
    private final AdministradorInicialProperties properties;

    public AdministradorInicialRunner(
            AdministradorInicialService service,
            AdministradorInicialProperties properties) {

        this.service = service;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        service.criarSeNecessario(properties)
                .ifPresentOrElse(
                        this::registrarCriacao,
                        () -> log.info(
                                "Administrador inicial não criado: já existe "
                                        + "administrador ativo"));
    }

    private void registrarCriacao(ContaAcesso conta) {
        log.info("Administrador inicial criado: login={}", conta.getLogin());
    }
}
