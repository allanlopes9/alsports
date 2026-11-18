package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.RefreshTokenProperties;
import com.curso.alsports.exception.CredenciaisInvalidasException;
import com.curso.alsports.exception.RefreshTokenInvalidoException;
import com.curso.alsports.exception.SessaoGeracaoDivergenteException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.MotivoRevogacaoRefreshToken;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.SessaoRefreshToken;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.SessaoRefreshTokenRepository;

import jakarta.persistence.EntityManager;

/**
 * Regras de classificação de falhas, rollback e protocolo de locks de V3/V4.
 */
@ExtendWith(MockitoExtension.class)
class SessaoAutenticacaoServiceTest {

    private static final String LOGIN = "sessao.teste";
    private static final String SENHA = "senha-do-teste-2026";
    private static final String ORIGEM = "10.0.0.5";
    private static final String GENERICO = "Login ou senha inválidos";
    private static final Instant AGORA =
            Instant.parse("2026-11-17T12:00:00Z");

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ContaAcessoRepository contaRepository;

    @Mock
    private SessaoRefreshTokenRepository sessaoRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private ProtecaoLogin protecao;

    @Mock
    private RegistradorEventosSeguranca auditoria;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Authentication authentication;

    private SessaoAutenticacaoService service;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties properties = new RefreshTokenProperties(
                Duration.ofMinutes(10), Duration.ofHours(1));

        service = new SessaoAutenticacaoService(
                authenticationManager,
                contaRepository,
                sessaoRepository,
                tokenService,
                properties,
                Clock.fixed(AGORA, ZoneOffset.UTC),
                new SecureRandom(),
                protecao,
                auditoria);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("credenciaisInvalidas")
    void credenciaisInvalidasDevemRegistrarFalhaSemEmitirTokens(
            String descricao, AuthenticationException ex) {

        when(protecao.permitirTentativa(LOGIN, ORIGEM)).thenReturn(true);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(ex);
        when(contaRepository.buscarIdPorLogin(LOGIN)).thenReturn(10L);

        assertThatThrownBy(() -> service.iniciar(LOGIN, SENHA, ORIGEM))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage(GENERICO)
                .hasNoCause();

        verify(protecao).registrarFalha(LOGIN, ORIGEM, 10L);
        verify(protecao, never()).registrarSucesso(
                anyString(), anyString(), anyLong());
        verifyNoInteractions(tokenService, auditoria, sessaoRepository);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("falhasTecnicas")
    void falhasTecnicasDevemPropagarSemContadorOuEvento(
            String descricao, Throwable falha) {
        when(protecao.permitirTentativa(LOGIN, ORIGEM)).thenReturn(true);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(falha);

        assertThatThrownBy(() -> service.iniciar(LOGIN, SENHA, ORIGEM))
                .isSameAs(falha);

        verify(protecao).permitirTentativa(LOGIN, ORIGEM);
        verify(protecao, never()).registrarFalha(
                anyString(), anyString(), any());
        verifyNoInteractions(tokenService, auditoria, sessaoRepository);
    }

    @Test
    void loginDevePreservarSomenteCredenciaisInvalidasNoRollback()
            throws Exception {

        Transactional transacao = SessaoAutenticacaoService.class
                .getMethod("iniciar", String.class, String.class, String.class)
                .getAnnotation(Transactional.class);

        assertThat(transacao).isNotNull();
        assertThat(transacao.noRollbackFor())
                .containsExactly(CredenciaisInvalidasException.class);
        assertThat(transacao.rollbackFor()).isEmpty();
        assertThat(transacao.noRollbackFor()).doesNotContain(
                SessaoGeracaoDivergenteException.class);
    }

    @Test
    void protecaoNaoDeveAbrirTransacaoIndependente() throws Exception {
        List<MetodoProtecao> metodos = List.of(
                new MetodoProtecao(
                        "permitirTentativa", List.of(String.class, String.class)),
                new MetodoProtecao(
                        "registrarFalha",
                        List.of(String.class, String.class, Long.class)),
                new MetodoProtecao(
                        "registrarSucesso",
                        List.of(String.class, String.class, Long.class)));

        for (MetodoProtecao metodo : metodos) {
            Transactional transacao = ProtecaoAutenticacaoService.class
                    .getMethod(
                            metodo.nome, metodo.parametros.toArray(Class<?>[]::new))
                    .getAnnotation(Transactional.class);

            assertThat(transacao)
                    .as("@Transactional em %s", metodo.nome)
                    .isNotNull();
            assertThat(transacao.propagation())
                    .as("propagação em %s", metodo.nome)
                    .isEqualTo(Propagation.REQUIRED);
        }
    }

    @Test
    void geracaoDivergenteNaoDeveEmitirTokensNemContarComoCredencial() {
        ContaAcesso conta = contaValida();
        conta.incrementarSessaoGeracao();
        prepararAutenticacaoValida(conta, 0L);

        assertThatThrownBy(() -> service.iniciar(LOGIN, SENHA, ORIGEM))
                .isInstanceOf(SessaoGeracaoDivergenteException.class)
                .hasMessage("Geração de sessão divergente no login");

        assertThat(CredenciaisInvalidasException.class.isAssignableFrom(
                SessaoGeracaoDivergenteException.class)).isFalse();
        verify(protecao, never()).registrarFalha(
                anyString(), anyString(), any());
        verify(protecao, never()).registrarSucesso(
                anyString(), anyString(), anyLong());
        verifyNoInteractions(tokenService, auditoria, sessaoRepository);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contasInvalidas")
    void contaPessoaEPerfisDevemSerRevalidadosAposOLock(
            String descricao, Consumer<ContaAcesso> mutacao) {

        ContaAcesso conta = contaValida();
        mutacao.accept(conta);
        prepararAutenticacaoValida(conta, conta.getSessaoGeracao());

        assertThatThrownBy(() -> service.iniciar(LOGIN, SENHA, ORIGEM))
                .as(descricao)
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage(GENERICO);

        verify(protecao).registrarFalha(LOGIN, ORIGEM, 10L);
        verify(protecao, never()).registrarSucesso(
                anyString(), anyString(), anyLong());
        verifyNoInteractions(tokenService, auditoria, sessaoRepository);
    }

    @Test
    void logoutGlobalDeveBloquearContaESessoesEmOrdemAntesDeIncrementarGeracao() {
        ContaAcesso conta = contaValida();
        Map<Long, SessaoRefreshToken> sessoes = Map.of(
                30L, sessao(30L, conta, "familia-3", "hash-3"),
                10L, sessao(10L, conta, "familia-1", "hash-1"),
                20L, sessao(20L, conta, "familia-2", "hash-2"));

        when(contaRepository.buscarIdPorLogin(LOGIN)).thenReturn(10L);
        when(contaRepository.buscarPorIdParaAtualizacao(10L))
                .thenReturn(Optional.of(conta));
        when(contaRepository.findById(10L)).thenReturn(Optional.of(conta));
        when(sessaoRepository.buscarIdsAtivasPorContaId(10L))
                .thenReturn(List.of(30L, 10L, 20L));
        when(sessaoRepository.buscarPorIdParaAtualizacao(anyLong()))
                .thenAnswer(invocacao -> Optional.ofNullable(
                        sessoes.get(invocacao.getArgument(0, Long.class))));

        service.encerrarTodas(LOGIN, ORIGEM);

        assertThat(conta.getSessaoGeracao()).isEqualTo(1L);

        InOrder ordem = inOrder(
                contaRepository, entityManager, sessaoRepository, auditoria);
        ordem.verify(contaRepository).buscarPorIdParaAtualizacao(10L);
        ordem.verify(entityManager).clear();
        ordem.verify(contaRepository).findById(10L);
        ordem.verify(sessaoRepository).buscarIdsAtivasPorContaId(10L);
        ordem.verify(sessaoRepository).buscarPorIdParaAtualizacao(10L);
        ordem.verify(sessaoRepository).buscarPorIdParaAtualizacao(20L);
        ordem.verify(sessaoRepository).buscarPorIdParaAtualizacao(30L);
        ordem.verify(sessaoRepository).saveAll(anyList());
        ordem.verify(auditoria).registrar(
                eq(TipoEventoSeguranca.LOGOUT_GLOBAL),
                eq(AGORA), eq(10L), eq(10L), isNull(), eq(ORIGEM));

        ArgumentCaptor<List<SessaoRefreshToken>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(sessaoRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(sessao -> {
            assertThat(sessao.getRevogadaEm()).isEqualTo(AGORA);
            assertThat(sessao.getMotivoRevogacao())
                    .isEqualTo(MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL);
        });
    }

    @Test
    void renovarDeveLerHashAntesDoLockDaContaELinhaDaSessao() {
        ContaAcesso conta = contaValida();
        String refresh = "refresh-token-opaco-do-teste";
        String hash = service.calcularHash(refresh);
        SessaoRefreshToken sessao = sessao(
                50L, conta, "familia-renovacao", hash);

        when(sessaoRepository.buscarIdPorHash(hash)).thenReturn(50L);
        when(sessaoRepository.buscarContaIdPorHash(hash)).thenReturn(10L);
        when(contaRepository.buscarPorIdParaAtualizacao(10L))
                .thenReturn(Optional.of(conta));
        when(contaRepository.findById(10L)).thenReturn(Optional.of(conta));
        when(sessaoRepository.buscarPorIdParaAtualizacao(50L))
                .thenReturn(Optional.of(sessao));
        when(tokenService.emitir(any(Authentication.class), anyString()))
                .thenReturn(new TokenAcesso("jwt.teste", AGORA.plusSeconds(900), 900));

        TokensAutenticacao tokens = service.renovar(refresh, ORIGEM);

        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotEqualTo(refresh);
        assertThat(sessao.getUtilizadaEm()).isEqualTo(AGORA);

        InOrder ordem = inOrder(
                sessaoRepository, contaRepository, entityManager,
                tokenService, auditoria);
        ordem.verify(sessaoRepository).buscarIdPorHash(hash);
        ordem.verify(sessaoRepository).buscarContaIdPorHash(hash);
        ordem.verify(contaRepository).buscarPorIdParaAtualizacao(10L);
        ordem.verify(entityManager).clear();
        ordem.verify(contaRepository).findById(10L);
        ordem.verify(sessaoRepository).buscarPorIdParaAtualizacao(50L);
        ordem.verify(sessaoRepository).save(sessao);
        ordem.verify(sessaoRepository).save(any(SessaoRefreshToken.class));
        ordem.verify(tokenService).emitir(any(Authentication.class), anyString());
        ordem.verify(auditoria).registrar(
                eq(TipoEventoSeguranca.REFRESH_SUCESSO),
                eq(AGORA), eq(10L), eq(10L), isNull(), eq(ORIGEM));
    }

    @Test
    void renovarComContaInativaDeveRevogarFamiliaSemEmitirTokens() {
        ContaAcesso conta = contaValida();
        conta.bloquear();
        String refresh = "refresh-conta-bloqueada";
        String hash = service.calcularHash(refresh);
        SessaoRefreshToken sessao = sessao(
                50L, conta, "familia-bloqueada", hash);

        when(sessaoRepository.buscarIdPorHash(hash)).thenReturn(50L);
        when(sessaoRepository.buscarContaIdPorHash(hash)).thenReturn(10L);
        when(contaRepository.buscarPorIdParaAtualizacao(10L))
                .thenReturn(Optional.of(conta));
        when(contaRepository.findById(10L)).thenReturn(Optional.of(conta));
        when(sessaoRepository.buscarPorIdParaAtualizacao(50L))
                .thenReturn(Optional.of(sessao));
        when(sessaoRepository.buscarIdsPorFamiliaId("familia-bloqueada"))
                .thenReturn(List.of(50L));

        assertThatThrownBy(() -> service.renovar(refresh, ORIGEM))
                .isInstanceOf(RefreshTokenInvalidoException.class);

        assertThat(sessao.getRevogadaEm()).isEqualTo(AGORA);
        assertThat(sessao.getMotivoRevogacao())
                .isEqualTo(MotivoRevogacaoRefreshToken.CONTA_INATIVA);
        verifyNoInteractions(tokenService);
        verify(auditoria).registrar(
                eq(TipoEventoSeguranca.REFRESH_FALHA),
                eq(AGORA), isNull(), eq(10L), isNull(), eq(ORIGEM));
    }

    private void prepararAutenticacaoValida(
            ContaAcesso conta, long geracaoCapturada) {

        when(contaRepository.buscarGeracaoPorLogin(LOGIN))
                .thenReturn(geracaoCapturada);
        when(protecao.permitirTentativa(LOGIN, ORIGEM)).thenReturn(true);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(LOGIN);
        when(contaRepository.buscarIdPorLogin(LOGIN)).thenReturn(10L);
        when(contaRepository.buscarPorIdParaAtualizacao(10L))
                .thenReturn(Optional.of(conta));
        when(contaRepository.findById(10L)).thenReturn(Optional.of(conta));
    }

    private static ContaAcesso contaValida() {
        Pessoa pessoa = new Pessoa(
                "Pessoa de Teste",
                "sessao@example.com",
                "52998224725",
                LocalDate.of(1990, 1, 1));
        ContaAcesso conta = new ContaAcesso(
                pessoa, LOGIN, "{bcrypt}hash-preservado");
        ReflectionTestUtils.setField(conta, "id", 10L);
        conta.substituirPerfis(Set.of(PerfilAcesso.OPERADOR));
        return conta;
    }

    private static SessaoRefreshToken sessao(
            Long id, ContaAcesso conta, String familia, String hash) {

        SessaoRefreshToken sessao = SessaoRefreshToken.nova(
                conta,
                familia,
                hash,
                AGORA.minusSeconds(60),
                AGORA.plusSeconds(3600));
        ReflectionTestUtils.setField(sessao, "id", id);
        return sessao;
    }

    private static Stream<Arguments> credenciaisInvalidas() {
        return Stream.of(
                Arguments.of("BadCredentialsException",
                        new BadCredentialsException("senha divergente")),
                Arguments.of("UsernameNotFoundException",
                        new UsernameNotFoundException("inexistente")),
                Arguments.of("DisabledException",
                        new DisabledException("desabilitada")),
                Arguments.of("LockedException",
                        new LockedException("bloqueada")),
                Arguments.of("AccountExpiredException",
                        new AccountExpiredException("expirada")),
                Arguments.of("CredentialsExpiredException",
                        new CredentialsExpiredException("credencial expirada")));
    }

    private static Stream<Arguments> falhasTecnicas() {
        return Stream.of(
                Arguments.of("AuthenticationServiceException",
                        new AuthenticationServiceException("indisponível")),
                Arguments.of("InternalAuthenticationServiceException",
                        new InternalAuthenticationServiceException("interno")),
                Arguments.of("ProviderNotFoundException",
                        new ProviderNotFoundException("sem provider")),
                Arguments.of("AuthenticationCredentialsNotFoundException",
                        new AuthenticationCredentialsNotFoundException("sem contexto")),
                Arguments.of("IllegalStateException",
                        new IllegalStateException("falha técnica")),
                Arguments.of("AssertionError",
                        new AssertionError("falha técnica grave")));
    }

    private static Stream<Arguments> contasInvalidas() {
        return Stream.of(
                Arguments.of("conta bloqueada",
                        (Consumer<ContaAcesso>) ContaAcesso::bloquear),
                Arguments.of("pessoa inativa",
                        (Consumer<ContaAcesso>) conta ->
                                conta.getPessoa().inativar()),
                Arguments.of("sem perfis",
                        (Consumer<ContaAcesso>) conta ->
                                ReflectionTestUtils.setField(
                                        conta, "perfis", new LinkedHashSet<>())));
    }

    private record MetodoProtecao(
            String nome, List<Class<?>> parametros) {
    }
}
