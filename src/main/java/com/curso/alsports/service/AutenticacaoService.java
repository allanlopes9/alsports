package com.curso.alsports.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.curso.alsports.exception.CredenciaisInvalidasException;
import com.curso.alsports.repository.ContaAcessoRepository;

/**
 * Caso de uso legado de autenticação, preservado da Aula 13.
 *
 * <p>O endpoint HTTP usa {@link SessaoAutenticacaoService}; este serviço
 * continua disponível e também passa pela proteção da Aula 17. O
 * {@code AuthenticationManager}, o BCrypt e o {@link TokenService} originais
 * não foram substituídos.</p>
 */
@Service
public class AutenticacaoService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS =
            "Login ou senha inválidos";

    private static final ProtecaoLogin PROTECAO_NEUTRA = new ProtecaoLogin() {
        @Override
        public boolean permitirTentativa(String login, String enderecoOrigem) {
            return true;
        }

        @Override
        public void registrarFalha(
                String login, String enderecoOrigem, Long alvoContaId) {
            // sem efeito: apenas para o construtor unitário legado
        }

        @Override
        public void registrarSucesso(
                String login, String enderecoOrigem, Long contaId) {
            // sem efeito: apenas para o construtor unitário legado
        }
    };

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final ContaAcessoRepository contaRepository;
    private final ProtecaoLogin protecao;

    public AutenticacaoService(
            AuthenticationManager authenticationManager,
            TokenService tokenService) {

        this(authenticationManager, tokenService, null, PROTECAO_NEUTRA);
    }

    @Autowired
    public AutenticacaoService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            ContaAcessoRepository contaRepository,
            ProtecaoLogin protecao) {

        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.contaRepository = contaRepository;
        this.protecao = protecao;
    }

    public TokenAcesso autenticar(String login, String senha) {
        return autenticar(login, senha, null);
    }

    public TokenAcesso autenticar(
            String login, String senha, String enderecoOrigem) {

        if (!protecao.permitirTentativa(login, enderecoOrigem)) {
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            login, senha));

            TokenAcesso token = tokenService.emitir(authentication);
            Long contaId = contaRepository == null
                    ? null
                    : contaRepository.findByLoginIgnoreCase(authentication.getName())
                            .map(conta -> conta.getId())
                            .orElse(null);

            protecao.registrarSucesso(login, enderecoOrigem, contaId);
            return token;
        } catch (AuthenticationException ex) {
            Long alvo = contaRepository == null
                    ? null
                    : contaRepository.findByLoginIgnoreCase(login)
                            .map(conta -> conta.getId())
                            .orElse(null);

            protecao.registrarFalha(login, enderecoOrigem, alvo);
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }
    }
}
