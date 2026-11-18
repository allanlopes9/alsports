package com.curso.alsports.service;

/**
 * Fronteira da proteção on-line usada pelos casos de uso de autenticação.
 *
 * <p>A interface permite que testes unitários legados, que não sobem o
 * contexto Spring, continuem construindo o serviço com uma implementação
 * neutra. Em produção, somente {@link ProtecaoAutenticacaoService} a
 * implementa.</p>
 */
public interface ProtecaoLogin {

    boolean permitirTentativa(String login, String enderecoOrigem);

    void registrarFalha(String login, String enderecoOrigem, Long alvoContaId);

    void registrarSucesso(String login, String enderecoOrigem, Long contaId);
}
