package com.curso.alsports.exception;

/**
 * Recusa de uma operação administrativa por uma regra de segurança.
 *
 * <p>Estende {@link IllegalArgumentException} para preservar o contrato HTTP
 * 400 já adotado pelas regras de negócio. O tipo específico permite que a
 * auditoria da recusa seja confirmada com {@code noRollbackFor} sem preservar
 * falhas técnicas.</p>
 */
public class OperacaoSegurancaRecusadaException
        extends IllegalArgumentException {

    public OperacaoSegurancaRecusadaException(String message) {
        super(message);
    }
}
