package com.curso.alsports.model;

/**
 * Vocabulário fechado dos eventos de segurança do AL Sports.
 *
 * <p>O tipo é um enum, nunca texto livre enviado pelo cliente. Isso reduz a
 * superfície de injeção em logs e mantém consultas previsíveis.</p>
 */
public enum TipoEventoSeguranca {
    LOGIN_SUCESSO,
    LOGIN_FALHA,
    LOGIN_RECUSADO_BLOQUEIO,
    BLOQUEIO_TEMPORARIO_ATIVADO,
    CONTA_CRIADA,
    SENHA_ALTERADA,
    PERFIS_ALTERADOS,
    STATUS_ALTERADO,
    REFRESH_SUCESSO,
    REFRESH_FALHA,
    LOGOUT,
    LOGOUT_GLOBAL
}
