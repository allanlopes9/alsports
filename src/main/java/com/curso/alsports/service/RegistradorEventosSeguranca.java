package com.curso.alsports.service;

import java.time.Instant;

import com.curso.alsports.model.TipoEventoSeguranca;

/**
 * Registro estruturado de eventos, sem exposição de segredos.
 */
public interface RegistradorEventosSeguranca {

    void registrar(
            TipoEventoSeguranca tipo,
            Instant ocorridoEm,
            Long atorContaId,
            Long alvoContaId,
            String identificadorHash,
            String enderecoOrigem);
}
