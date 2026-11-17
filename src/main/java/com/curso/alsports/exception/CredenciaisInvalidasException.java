package com.curso.alsports.exception;

/**
 * Falha pública de autenticação no endpoint de login.
 *
 * <p>A mensagem é intencionalmente genérica: login inexistente, senha
 * incorreta, conta bloqueada e pessoa inativa produzem a mesma resposta.</p>
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
