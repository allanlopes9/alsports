package com.curso.alsports.service;

import java.time.Instant;

/**
 * Resultado da emissão de um access token.
 *
 * @param valor            JWT assinado
 * @param expiraEm         instante absoluto de expiração
 * @param duracaoSegundos  duração total em segundos
 */
public record TokenAcesso(
        String valor,
        Instant expiraEm,
        long duracaoSegundos) {
}
