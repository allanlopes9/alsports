package com.curso.alsports.exception;

/**
 * Falha pública de renovação ou encerramento de sessão.
 *
 * <p>A mensagem é intencionalmente genérica para não permitir enumeração de
 * tokens: ausente, desconhecido, já utilizado, revogado, expirado ou ligado a
 * conta inativa produzem exatamente a mesma resposta.</p>
 */
public class RefreshTokenInvalidoException extends RuntimeException {

    public static final String MENSAGEM_GENERICA =
            "Refresh token inválido ou expirado";

    public RefreshTokenInvalidoException() {
        super(MENSAGEM_GENERICA);
    }
}
