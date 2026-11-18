package com.curso.alsports.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.curso.alsports.service.AdministradorRecuperacaoService;

/**
 * Recuperação operacional executada antes do seed inicial.
 *
 * <p>Somente o login informado é registrado em log, nunca a confirmação ou
 * qualquer credencial.</p>
 */
@Component
@Order(100)
@ConditionalOnProperty(
        name = "app.security.admin-recovery.enabled",
        havingValue = "true")
public class AdministradorRecuperacaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            AdministradorRecuperacaoRunner.class);

    private final AdministradorRecuperacaoService service;
    private final AdministradorRecuperacaoProperties properties;

    public AdministradorRecuperacaoRunner(
            AdministradorRecuperacaoService service,
            AdministradorRecuperacaoProperties properties) {

        this.service = service;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean reativada = service.recuperar(properties);
        log.info("Recuperação administrativa concluída: login={}, resultado={}",
                properties.login(), reativada ? "PESSOA_REATIVADA" : "SEM_ALTERACAO");
    }
}
