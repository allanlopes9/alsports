package com.curso.alsports.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.RefreshTokenProperties;
import com.curso.alsports.exception.CredenciaisInvalidasException;
import com.curso.alsports.exception.RefreshTokenInvalidoException;
import com.curso.alsports.exception.SessaoGeracaoDivergenteException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.MotivoRevogacaoRefreshToken;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.SessaoRefreshToken;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.SessaoRefreshTokenRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Ciclo de vida das sessões de autenticação do AL Sports.
 *
 * <p>O access token permanece um JWT HS256 stateless. O refresh token é opaco,
 * gerado com 256 bits de entropia e persistido apenas como hash SHA-256. O
 * identificador da família viaja no claim {@code sid} do access token para
 * rastreabilidade; o Resource Server não consulta a tabela de sessões a cada
 * requisição.</p>
 *
 * <p>A geração da conta protege apenas criação e rotação de sessões refresh.
 * JWTs já emitidos continuam válidos até expirar.</p>
 */
@Service
public class SessaoAutenticacaoService {

    private static final int TAMANHO_REFRESH_TOKEN_BYTES = 32;
    private static final String TIPO_TOKEN = "Bearer";
    private static final String PREFIXO_PERFIL = "ROLE_";
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
            // sem efeito: apenas para construtores unitários legados
        }

        @Override
        public void registrarSucesso(
                String login, String enderecoOrigem, Long contaId) {
            // sem efeito: apenas para construtores unitários legados
        }
    };

    private static final RegistradorEventosSeguranca AUDITORIA_NEUTRA =
            (tipo, instante, ator, alvo, hash, origem) -> {
                // sem efeito: apenas para construtores unitários legados
            };

    private final AuthenticationManager authenticationManager;
    private final ContaAcessoRepository contaRepository;
    private final SessaoRefreshTokenRepository sessaoRepository;
    private final TokenService tokenService;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final ProtecaoLogin protecao;
    private final RegistradorEventosSeguranca auditoria;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Construtor de compatibilidade usado por testes unitários que não sobem
     * o contexto Spring. Em produção, o construtor com proteção e auditoria é
     * o selecionado.
     */
    public SessaoAutenticacaoService(
            AuthenticationManager authenticationManager,
            ContaAcessoRepository contaRepository,
            SessaoRefreshTokenRepository sessaoRepository,
            TokenService tokenService,
            RefreshTokenProperties properties,
            Clock clock) {

        this(authenticationManager, contaRepository, sessaoRepository,
                tokenService, properties, clock, new SecureRandom(),
                PROTECAO_NEUTRA, AUDITORIA_NEUTRA);
    }

    @Autowired
    public SessaoAutenticacaoService(
            AuthenticationManager authenticationManager,
            ContaAcessoRepository contaRepository,
            SessaoRefreshTokenRepository sessaoRepository,
            TokenService tokenService,
            RefreshTokenProperties properties,
            Clock clock,
            ProtecaoLogin protecao,
            RegistradorEventosSeguranca auditoria) {

        this(authenticationManager, contaRepository, sessaoRepository,
                tokenService, properties, clock, new SecureRandom(),
                protecao, auditoria);
    }

    SessaoAutenticacaoService(
            AuthenticationManager authenticationManager,
            ContaAcessoRepository contaRepository,
            SessaoRefreshTokenRepository sessaoRepository,
            TokenService tokenService,
            RefreshTokenProperties properties,
            Clock clock,
            SecureRandom secureRandom,
            ProtecaoLogin protecao,
            RegistradorEventosSeguranca auditoria) {

        this.authenticationManager = Objects.requireNonNull(authenticationManager);
        this.contaRepository = Objects.requireNonNull(contaRepository);
        this.sessaoRepository = Objects.requireNonNull(sessaoRepository);
        this.tokenService = Objects.requireNonNull(tokenService);
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
        this.secureRandom = Objects.requireNonNull(secureRandom);
        this.protecao = Objects.requireNonNull(protecao);
        this.auditoria = Objects.requireNonNull(auditoria);
    }

    /**
     * Autentica login e senha pelo provider existente e cria uma nova família.
     *
     * <p>Somente credenciais inválidas são preservadas pelo
     * {@code noRollbackFor}. Falhas técnicas, divergência de geração e
     * qualquer outro erro revertem a transação inteira.</p>
     */
    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokensAutenticacao iniciar(String login, String senha) {
        return iniciar(login, senha, null);
    }

    @Transactional(noRollbackFor = CredenciaisInvalidasException.class)
    public TokensAutenticacao iniciar(
            String login, String senha, String enderecoOrigem) {

        Long geracaoCapturada = contaRepository.buscarGeracaoPorLogin(login);

        if (!protecao.permitirTentativa(login, enderecoOrigem)) {
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            login, senha));
        } catch (AuthenticationException ex) {
            if (ehCredencialInvalida(ex)) {
                registrarFalha(login, enderecoOrigem, null);
                throw new CredenciaisInvalidasException(
                        MENSAGEM_CREDENCIAIS_INVALIDAS);
            }
            throw ex;
        }

        // O UserDetailsService carregou a conta com perfis no mesmo EntityManager.
        // Desanexar evita que o lock seja decidido com estado obsoleto.
        limparContexto();

        Long contaId = contaRepository.buscarIdPorLogin(authentication.getName());
        if (contaId == null) {
            registrarFalha(login, enderecoOrigem, null);
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        ContaAcesso conta = carregarContaBloqueadaOpcional(contaId);

        if (conta == null) {
            registrarFalha(login, enderecoOrigem, contaId);
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        if (!conta.podeAutenticar()
                || conta.getPerfis().isEmpty()) {
            registrarFalha(login, enderecoOrigem, conta.getId());
            throw new CredenciaisInvalidasException(
                    MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        if (geracaoCapturada != null
                && conta.getSessaoGeracao() != geracaoCapturada) {
            throw new SessaoGeracaoDivergenteException(
                    "Geração de sessão divergente no login");
        }

        Instant agora = clock.instant();
        String familiaId = UUID.randomUUID().toString();
        Instant expiraEm = agora.plus(properties.absoluteExpiration());

        TokensAutenticacao tokens = emitirPar(
                conta, familiaId, agora, expiraEm);

        protecao.registrarSucesso(login, enderecoOrigem, conta.getId());
        return tokens;
    }

    /**
     * Renova o par de tokens usando rotação de uso único.
     *
     * <p>A revogação decorrente de reutilização, inatividade, expiração
     * absoluta ou conta inativa precisa ser confirmada mesmo quando a resposta
     * HTTP é 401. Por isso a exceção pública não provoca rollback.</p>
     */
    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public TokensAutenticacao renovar(String refreshToken) {
        return renovar(refreshToken, null);
    }

    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public TokensAutenticacao renovar(
            String refreshToken, String enderecoOrigem) {

        SessaoRefreshToken sessao = null;
        try {
            String hash = exigirHash(refreshToken);
            Long sessaoId = sessaoRepository.buscarIdPorHash(hash);
            Long contaId = sessaoRepository.buscarContaIdPorHash(hash);

            if (sessaoId == null || contaId == null) {
                throw new RefreshTokenInvalidoException();
            }

            ContaAcesso conta = carregarContaBloqueada(contaId);
            sessao = sessaoRepository.buscarPorIdParaAtualizacao(sessaoId)
                    .orElseThrow(RefreshTokenInvalidoException::new);

            Instant agora = clock.instant();

            if (sessao.getRevogadaEm() != null) {
                throw new RefreshTokenInvalidoException();
            }

            if (sessao.getUtilizadaEm() != null) {
                revogarFamilia(
                        conta,
                        sessao.getFamiliaId(),
                        agora,
                        MotivoRevogacaoRefreshToken.REUTILIZACAO);
                throw new RefreshTokenInvalidoException();
            }

            // O limite absoluto prevalece sobre a inatividade: quando ambos
            // foram atingidos, o motivo auditável é a expiração absoluta da
            // família.
            if (sessao.expirouPorLimiteAbsoluto(agora)) {
                revogarFamilia(
                        conta,
                        sessao.getFamiliaId(),
                        agora,
                        MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA);
                throw new RefreshTokenInvalidoException();
            }

            if (sessao.expirouPorInatividade(agora, properties.idleTimeout())) {
                revogarFamilia(
                        conta,
                        sessao.getFamiliaId(),
                        agora,
                        MotivoRevogacaoRefreshToken.INATIVIDADE);
                throw new RefreshTokenInvalidoException();
            }

            if (!conta.podeAutenticar() || conta.getPerfis().isEmpty()) {
                revogarFamilia(
                        conta,
                        sessao.getFamiliaId(),
                        agora,
                        MotivoRevogacaoRefreshToken.CONTA_INATIVA);
                throw new RefreshTokenInvalidoException();
            }

            sessao.marcarUtilizada(agora);
            sessaoRepository.save(sessao);

            TokensAutenticacao tokens =
                    emitirPar(conta, sessao.getFamiliaId(), agora,
                            sessao.getExpiraEm());

            auditoria.registrar(
                    TipoEventoSeguranca.REFRESH_SUCESSO,
                    agora,
                    conta.getId(),
                    conta.getId(),
                    null,
                    enderecoOrigem);

            return tokens;
        } catch (RefreshTokenInvalidoException ex) {
            auditoria.registrar(
                    TipoEventoSeguranca.REFRESH_FALHA,
                    clock.instant(),
                    null,
                    sessao == null || sessao.getConta() == null
                            ? null : sessao.getConta().getId(),
                    null,
                    enderecoOrigem);
            throw ex;
        }
    }

    /**
     * Logout local: revoga a família do refresh token informado.
     *
     * <p>Sempre termina em sucesso do ponto de vista do cliente. Não informa
     * se o token existia, estava expirado ou já havia sido revogado.</p>
     */
    @Transactional
    public void encerrar(String refreshToken) {
        encerrar(refreshToken, null);
    }

    @Transactional
    public void encerrar(String refreshToken, String enderecoOrigem) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String hash = calcularHash(refreshToken);
        Long sessaoId = sessaoRepository.buscarIdPorHash(hash);
        Long contaId = sessaoRepository.buscarContaIdPorHash(hash);

        if (sessaoId == null || contaId == null) {
            return;
        }

        ContaAcesso conta = carregarContaBloqueada(contaId);
        SessaoRefreshToken sessao = sessaoRepository
                .buscarPorIdParaAtualizacao(sessaoId)
                .orElse(null);

        if (sessao == null) {
            return;
        }

        Instant agora = clock.instant();
        revogarFamilia(
                conta,
                sessao.getFamiliaId(),
                agora,
                MotivoRevogacaoRefreshToken.LOGOUT);

        auditoria.registrar(
                TipoEventoSeguranca.LOGOUT,
                agora,
                conta.getId(),
                conta.getId(),
                null,
                enderecoOrigem);
    }

    /**
     * Logout global: revoga todas as famílias ainda ativas da conta e incrementa
     * a geração sob o lock da conta.
     */
    @Transactional
    public void encerrarTodas(String login) {
        encerrarTodas(login, null);
    }

    @Transactional
    public void encerrarTodas(String login, String enderecoOrigem) {
        if (login == null || login.isBlank()) {
            throw new RefreshTokenInvalidoException();
        }

        Long contaId = contaRepository.buscarIdPorLogin(login);
        if (contaId == null) {
            throw new RefreshTokenInvalidoException();
        }

        ContaAcesso conta = carregarContaBloqueada(contaId);
        Instant agora = clock.instant();

        List<Long> ids = sessaoRepository.buscarIdsAtivasPorContaId(contaId)
                .stream()
                .sorted()
                .distinct()
                .toList();
        List<SessaoRefreshToken> ativas = new java.util.ArrayList<>();
        for (Long id : ids) {
            sessaoRepository.buscarPorIdParaAtualizacao(id)
                    .ifPresent(ativas::add);
        }

        for (SessaoRefreshToken sessao : ativas) {
            if (sessao.getRevogadaEm() == null) {
                sessao.revogar(
                        agora, MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL);
            }
        }
        sessaoRepository.saveAll(ativas);

        conta.incrementarSessaoGeracao();

        auditoria.registrar(
                TipoEventoSeguranca.LOGOUT_GLOBAL,
                agora,
                conta.getId(),
                conta.getId(),
                null,
                enderecoOrigem);
    }

    /**
     * Preserva apenas as exceções públicas equivalentes a credencial inválida.
     * Falhas técnicas, incluindo {@code AuthenticationServiceException} e sua
     * subclasse {@code InternalAuthenticationServiceException}, propagam e
     * provocam rollback.
     */
    private static boolean ehCredencialInvalida(AuthenticationException ex) {
        return ex instanceof BadCredentialsException
                || ex instanceof UsernameNotFoundException
                || ex instanceof DisabledException
                || ex instanceof LockedException
                || ex instanceof AccountExpiredException
                || ex instanceof CredentialsExpiredException;
    }

    private void registrarFalha(
            String login, String enderecoOrigem, Long contaId) {

        Long alvo = contaId != null
                ? contaId
                : contaRepository.buscarIdPorLogin(login);
        protecao.registrarFalha(login, enderecoOrigem, alvo);
    }

    private ContaAcesso carregarContaBloqueada(Long contaId) {
        ContaAcesso conta = carregarContaBloqueadaOpcional(contaId);
        if (conta == null) {
            throw new RefreshTokenInvalidoException();
        }
        return conta;
    }

    private ContaAcesso carregarContaBloqueadaOpcional(Long contaId) {
        if (contaRepository.buscarPorIdParaAtualizacao(contaId).isEmpty()) {
            return null;
        }

        limparContexto();

        return contaRepository.findById(contaId).orElse(null);
    }

    private void limparContexto() {
        if (entityManager != null) {
            entityManager.clear();
        }
    }

    private TokensAutenticacao emitirPar(
            ContaAcesso conta,
            String familiaId,
            Instant agora,
            Instant expiraEm) {

        String refreshToken = gerarRefreshToken();
        String hash = calcularHash(refreshToken);

        sessaoRepository.save(SessaoRefreshToken.nova(
                conta, familiaId, hash, agora, expiraEm));

        TokenAcesso accessToken = tokenService.emitir(
                criarAuthentication(conta), familiaId);

        return new TokensAutenticacao(
                accessToken.valor(),
                TIPO_TOKEN,
                accessToken.duracaoSegundos(),
                accessToken.expiraEm(),
                refreshToken,
                expiraEm);
    }

    private Authentication criarAuthentication(ContaAcesso conta) {
        List<GrantedAuthority> autoridades = conta.getPerfis().stream()
                .map(PerfilAcesso::name)
                .map(perfil -> PREFIXO_PERFIL + perfil)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return UsernamePasswordAuthenticationToken.authenticated(
                conta.getLogin(), null, autoridades);
    }

    /**
     * Pré-condição: a conta já está bloqueada pela transação chamadora.
     * As sessões da família são descobertas por ID e bloqueadas em ordem
     * crescente antes da decisão final de revogação.
     */
    private void revogarFamilia(
            ContaAcesso conta,
            String familiaId,
            Instant agora,
            MotivoRevogacaoRefreshToken motivo) {

        if (conta == null) {
            throw new IllegalStateException(
                    "Revogação de família exige conta bloqueada");
        }

        List<Long> ids = sessaoRepository.buscarIdsPorFamiliaId(familiaId)
                .stream()
                .sorted()
                .distinct()
                .toList();
        List<SessaoRefreshToken> registros = new java.util.ArrayList<>();
        for (Long id : ids) {
            sessaoRepository.buscarPorIdParaAtualizacao(id)
                    .ifPresent(registros::add);
        }

        for (SessaoRefreshToken sessao : registros) {
            if (sessao.getRevogadaEm() == null) {
                sessao.revogar(agora, motivo);
            }
        }
        sessaoRepository.saveAll(registros);
    }

    private String exigirHash(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenInvalidoException();
        }
        return calcularHash(refreshToken);
    }

    String gerarRefreshToken() {
        byte[] bytes = new byte[TAMANHO_REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    String calcularHash(String refreshToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não disponível", ex);
        }
    }
}
