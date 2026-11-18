package com.curso.alsports.exception;

/**
 * Falha interna de consistência na geração da sessão.
 *
 * <p>Não herda de {@link CredenciaisInvalidasException}: a divergência não é
 * uma credencial inválida, não incrementa o contador de força bruta e deve
 * provocar rollback da transação de login.</p>
 */
public class SessaoGeracaoDivergenteException extends RuntimeException {

    public SessaoGeracaoDivergenteException(String message) {
        super(message);
    }
}
