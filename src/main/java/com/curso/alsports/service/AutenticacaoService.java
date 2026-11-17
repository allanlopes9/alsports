package com.curso.alsports.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.curso.alsports.exception.CredenciaisInvalidasException;

/**
 * Autentica credenciais reutilizando o {@link AuthenticationManager} da
 * Aula 12 e delega a emissão do token ao {@link TokenService}.
 *
 * <p>Não há segunda comparação BCrypt nem consulta paralela de conta: o
 * provider existente continua responsável por carregar a conta, comparar o
 * hash e verificar o status da conta e da pessoa.</p>
 */
@Service
public class AutenticacaoService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS =
            "Login ou senha inválidos";

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AutenticacaoService(
            AuthenticationManager authenticationManager,
            TokenService tokenService) {

        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public TokenAcesso autenticar(String login, String senha) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            login, senha));

            return tokenService.emitir(authentication);
        } catch (AuthenticationException ex) {
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }
    }
}
