package com.curso.alsports.model;

/**
 * Motivo registrado quando uma sessão de refresh token é encerrada.
 *
 * <p>O valor é persistido como texto ( EnumType.STRING ) e também é validado
 * por um constraint CHECK no banco, na migration da Aula 16.</p>
 */
public enum MotivoRevogacaoRefreshToken {

    LOGOUT,
    LOGOUT_GLOBAL,
    INATIVIDADE,
    EXPIRACAO_ABSOLUTA,
    REUTILIZACAO,
    CONTA_INATIVA
}
