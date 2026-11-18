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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.RefreshTokenProperties;
import com.curso.alsports.exception.CredenciaisInvalidasException;
import com.curso.alsports.exception.RefreshTokenInvalidoException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.MotivoRevogacaoRefreshToken;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.SessaoRefreshToken;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.SessaoRefreshTokenRepository;

/**
 * Ciclo de vida das sessões de autenticação do AL Sports.
 *
 * <p>O access token permanece um JWT HS256 stateless. O refresh token é opaco,
 * gerado com 256 bits de entropia e persistido apenas como hash SHA-256. O
 * identificador da família viaja no claim {@code sid} do access token para
 * rastreabilidade; o Resource Server não consulta a tabela de sessões a cada
 * requisição.</p>
 */
@Service
public class SessaoAutenticacaoService {

    private static final int TAMANHO_REFRESH_TOKEN_BYTES = 32;
    private static final String TIPO_TOKEN = "Bearer";
    private static final String PREFIXO_PERFIL = "ROLE_";
    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS =
            "Login ou senha inválidos";

    private final AuthenticationManager authenticationManager;
    private final ContaAcessoRepository contaRepository;
    private final SessaoRefreshTokenRepository sessaoRepository;
    private final TokenService tokenService;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public SessaoAutenticacaoService(
            AuthenticationManager authenticationManager,
            ContaAcessoRepository contaRepository,
            SessaoRefreshTokenRepository sessaoRepository,
            TokenService tokenService,
            RefreshTokenProperties properties,
            Clock clock) {

        this(
                authenticationManager,
                contaRepository,
                sessaoRepository,
                tokenService,
                properties,
                clock,
                new SecureRandom());
    }

    SessaoAutenticacaoService(
            AuthenticationManager authenticationManager,
            ContaAcessoRepository contaRepository,
            SessaoRefreshTokenRepository sessaoRepository,
            TokenService tokenService,
            RefreshTokenProperties properties,
            Clock clock,
            SecureRandom secureRandom) {

        this.authenticationManager = Objects.requireNonNull(authenticationManager);
        this.contaRepository = Objects.requireNonNull(contaRepository);
        this.sessaoRepository = Objects.requireNonNull(sessaoRepository);
        this.tokenService = Objects.requireNonNull(tokenService);
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
        this.secureRandom = Objects.requireNonNull(secureRandom);
    }

    /**
     * Autentica login e senha pelo provider existente e cria uma nova família.
     */
    @Transactional
    public TokensAutenticacao iniciar(String login, String senha) {
        Authentication authentication = autenticar(login, senha);

        ContaAcesso conta = contaRepository
                .findByLoginIgnoreCase(authentication.getName())
                .orElseThrow(() -> new CredenciaisInvalidasException(
                        MENSAGEM_CREDENCIAIS_INVALIDAS));

        validarConta(conta);

        Instant agora = clock.instant();
        String familiaId = UUID.randomUUID().toString();
        Instant expiraEm = agora.plus(properties.absoluteExpiration());

        return emitirPar(conta, familiaId, agora, expiraEm);
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
        SessaoRefreshToken sessao = buscarSessao(refreshToken);
        Instant agora = clock.instant();

        if (sessao.getRevogadaEm() != null) {
            throw new RefreshTokenInvalidoException();
        }

        if (sessao.getUtilizadaEm() != null) {
            revogarFamilia(
                    sessao.getFamiliaId(),
                    agora,
                    MotivoRevogacaoRefreshToken.REUTILIZACAO);
            throw new RefreshTokenInvalidoException();
        }

        // O limite absoluto prevalece sobre a inatividade: quando ambos foram
        // atingidos, o motivo auditável é a expiração absoluta da família.
        if (sessao.expirouPorLimiteAbsoluto(agora)) {
            revogarFamilia(
                    sessao.getFamiliaId(),
                    agora,
                    MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA);
            throw new RefreshTokenInvalidoException();
        }

        if (sessao.expirouPorInatividade(agora, properties.idleTimeout())) {
            revogarFamilia(
                    sessao.getFamiliaId(),
                    agora,
                    MotivoRevogacaoRefreshToken.INATIVIDADE);
            throw new RefreshTokenInvalidoException();
        }

        ContaAcesso conta = sessao.getConta();
        if (!conta.podeAutenticar()) {
            revogarFamilia(
                    sessao.getFamiliaId(),
                    agora,
                    MotivoRevogacaoRefreshToken.CONTA_INATIVA);
            throw new RefreshTokenInvalidoException();
        }

        sessao.marcarUtilizada(agora);
        sessaoRepository.save(sessao);

        return emitirPar(conta, sessao.getFamiliaId(), agora, sessao.getExpiraEm());
    }

    /**
     * Logout local: revoga a família do refresh token informado.
     *
     * <p>Sempre termina em sucesso do ponto de vista do cliente. Não informa
     * se o token existia, estava expirado ou já havia sido revogado.</p>
     */
    @Transactional
    public void encerrar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String hash = calcularHash(refreshToken);
        SessaoRefreshToken sessao = sessaoRepository
                .buscarPorHashParaAtualizacao(hash)
                .orElse(null);

        if (sessao == null) {
            return;
        }

        revogarFamilia(
                sessao.getFamiliaId(),
                clock.instant(),
                MotivoRevogacaoRefreshToken.LOGOUT);
    }

    /**
     * Logout global: revoga todas as famílias ainda ativas da conta.
     */
    @Transactional
    public void encerrarTodas(String login) {
        if (login == null || login.isBlank()) {
            throw new RefreshTokenInvalidoException();
        }

        ContaAcesso conta = contaRepository
                .findByLoginIgnoreCase(login)
                .orElseThrow(RefreshTokenInvalidoException::new);

        Instant agora = clock.instant();
        List<SessaoRefreshToken> ativas = sessaoRepository.findByContaId(conta.getId())
                .stream()
                .filter(sessao -> sessao.getRevogadaEm() == null)
                .toList();

        for (SessaoRefreshToken sessao : ativas) {
            sessao.revogar(agora, MotivoRevogacaoRefreshToken.LOGOUT_GLOBAL);
        }
        sessaoRepository.saveAll(ativas);
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

    private Authentication autenticar(String login, String senha) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login, senha));
        } catch (AuthenticationException ex) {
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }
    }

    private void validarConta(ContaAcesso conta) {
        if (!conta.podeAutenticar()) {
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }
    }

    private SessaoRefreshToken buscarSessao(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenInvalidoException();
        }

        return sessaoRepository
                .buscarPorHashParaAtualizacao(calcularHash(refreshToken))
                .orElseThrow(RefreshTokenInvalidoException::new);
    }

    private void revogarFamilia(
            String familiaId,
            Instant agora,
            MotivoRevogacaoRefreshToken motivo) {

        List<SessaoRefreshToken> registros = sessaoRepository.findByFamiliaId(familiaId)
                .stream()
                .filter(sessao -> sessao.getRevogadaEm() == null)
                .toList();

        for (SessaoRefreshToken sessao : registros) {
            sessao.revogar(agora, motivo);
        }
        sessaoRepository.saveAll(registros);
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
