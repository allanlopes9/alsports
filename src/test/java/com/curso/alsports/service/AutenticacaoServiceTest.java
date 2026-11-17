package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.curso.alsports.exception.CredenciaisInvalidasException;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    private static final String LOGIN = "ana.souza";
    private static final String SENHA = "uma-frase-senha-2026";
    private static final String MENSAGEM_GENERICA = "Login ou senha inválidos";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private Authentication authentication;

    private AutenticacaoService service;

    @BeforeEach
    void setUp() {
        service = new AutenticacaoService(authenticationManager, tokenService);
    }

    @Test
    void deveDelegarAutenticacaoAoManagerEEmitirToken() {
        TokenAcesso esperado = new TokenAcesso(
                "token.jwt.assinado",
                Instant.parse("2026-11-17T18:30:00Z"),
                1800);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(tokenService.emitir(authentication)).thenReturn(esperado);

        TokenAcesso token = service.autenticar(LOGIN, SENHA);

        assertThat(token).isSameAs(esperado);

        ArgumentCaptor<Authentication> captor =
                ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());

        Authentication enviada = captor.getValue();
        assertThat(enviada).isInstanceOf(
                UsernamePasswordAuthenticationToken.class);
        assertThat(enviada.getPrincipal()).isEqualTo(LOGIN);
        assertThat(enviada.getCredentials()).isEqualTo(SENHA);
        assertThat(enviada.isAuthenticated()).isFalse();

        verify(tokenService).emitir(authentication);
    }

    @Test
    void deveConverterBadCredentialsEmErroGenericoSemEmitirToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Senha incorreta"));

        assertThatThrownBy(() -> service.autenticar(LOGIN, "senha-errada"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage(MENSAGEM_GENERICA)
                .hasMessageNotContaining("Senha incorreta");

        verify(tokenService, never()).emitir(any(Authentication.class));
    }

    @Test
    void deveConverterContaDesabilitadaEmErroGenericoSemEmitirToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new DisabledException("Conta bloqueada"));

        assertThatThrownBy(() -> service.autenticar(LOGIN, SENHA))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage(MENSAGEM_GENERICA);

        verify(tokenService, never()).emitir(any(Authentication.class));
    }

    @Test
    void deveConverterLoginInexistenteEmErroGenericoSemEmitirToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new UsernameNotFoundException("Não encontrado"));

        assertThatThrownBy(() -> service.autenticar("nao.existe", SENHA))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage(MENSAGEM_GENERICA);

        verify(tokenService, never()).emitir(any(Authentication.class));
    }

    @Test
    void naoDeveEmitirTokenQuandoManagerNaoAutentica() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThatThrownBy(() -> service.autenticar(LOGIN, SENHA))
                .isInstanceOf(CredenciaisInvalidasException.class);

        verifyNoInteractions(tokenService);
    }
}
