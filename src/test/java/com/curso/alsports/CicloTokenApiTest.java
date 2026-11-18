package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.JwtProperties;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.MotivoRevogacaoRefreshToken;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.SessaoRefreshToken;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.curso.alsports.repository.SessaoRefreshTokenRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Integração HTTP real com PostgreSQL do profile test.
 *
 * <p>Cobre o ciclo completo: login, refresh com rotação, detecção de
 * reutilização, logout local/global, expiração controlada por relógio,
 * bloqueio de conta e concorrência com transações reais.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CicloTokenApiTest {

    private static final String SENHA = "ciclo-senha-didatica-2026";
    private static final String MSG_REFRESH_INVALIDO =
            "Refresh token inválido ou expirado";
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private SessaoRefreshTokenRepository sessaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RelogioTeste relogio;

    @Autowired
    private JwtProperties jwtProperties;

    private ContaAcesso conta;
    private Long pessoaId;
    private String login;

    @BeforeEach
    void setUp() {
        relogio.avancarPara(Instant.parse("2026-11-17T12:00:00Z"));
        Pessoa pessoa = criarPessoa();
        pessoaId = pessoa.getId();
        login = "ciclo." + sufixo();
        conta = contaRepository.saveAndFlush(new ContaAcesso(
                pessoa, login, passwordEncoder.encode(SENHA)));
    }

    /**
     * Os novos testes rodam fora da transação da classe para provar que o
     * serviço confirmou os dados no PostgreSQL. Limpamos explicitamente apenas
     * nesses casos, incluindo a pessoa criada no setUp, para não colidir com o
     * CPF determinístico de outras classes; os demais testes continuam
     * protegidos pelo rollback do Spring.
     */
    @AfterEach
    void limparDadosForaDaTransacaoDeTeste() {
        if (TestTransaction.isActive()
                || conta == null
                || conta.getId() == null) {
            return;
        }
        jdbcTemplate.update(
                "delete from sessao_refresh_token where conta_acesso_id = ?",
                conta.getId());
        jdbcTemplate.update("delete from conta_acesso where id = ?", conta.getId());
        if (pessoaId != null) {
            jdbcTemplate.update("delete from pessoa where id = ?", pessoaId);
        }
    }

    // ------------------------------------------------------------------
    // 1-6. Emissão e renovação
    // ------------------------------------------------------------------

    @Test
    void c01_loginDeveRetornarAccessERefreshToken() throws Exception {
        String corpo = login(login, SENHA);

        assertAll(
                () -> assertThat(JsonPath.<String>read(corpo, "$.accessToken"))
                        .isNotBlank(),
                () -> assertThat(JsonPath.<String>read(corpo, "$.refreshToken"))
                        .isNotBlank(),
                () -> assertThat(JsonPath.<String>read(corpo, "$.tokenType"))
                        .isEqualTo("Bearer"),
                () -> assertThat(JsonPath.<Integer>read(corpo, "$.expiresIn"))
                        .isEqualTo(1800),
                () -> assertThat(JsonPath.<String>read(corpo, "$.expiresAt"))
                        .isNotBlank(),
                () -> assertThat(JsonPath.<String>read(
                        corpo, "$.refreshTokenExpiresAt")).isNotBlank(),
                () -> assertThat(corpo).doesNotContain("tokenHash", "familiaId",
                        "contaId", SENHA));
    }

    @Test
    void c02_refreshTokenDeveSerDiferenteDoAccessToken() throws Exception {
        String corpo = login(login, SENHA);

        assertThat(JsonPath.<String>read(corpo, "$.refreshToken"))
                .isNotEqualTo(JsonPath.<String>read(corpo, "$.accessToken"));
    }

    @Test
    void c03_bancoDeveArmazenarApenasHashSha256() throws Exception {
        String refresh = JsonPath.read(login(login, SENHA), "$.refreshToken");
        String esperado = HexFormat.of().formatHex(MessageDigest
                .getInstance("SHA-256")
                .digest(refresh.getBytes(StandardCharsets.UTF_8)));

        List<String> hashes = sessaoRepository.findAll().stream()
                .filter(s -> s.getConta().getId().equals(conta.getId()))
                .map(SessaoRefreshToken::getTokenHash)
                .toList();

        assertAll(
                () -> assertThat(hashes).containsExactly(esperado),
                () -> assertThat(esperado).hasSize(64),
                () -> assertThat(esperado).matches("[0-9a-f]{64}"));
    }

    @Test
    void c04_bancoNaoDeveConterRefreshTokenOriginal() throws Exception {
        String refresh = JsonPath.read(login(login, SENHA), "$.refreshToken");

        Integer encontrados = jdbcTemplate.queryForObject(
                "select count(*) from sessao_refresh_token where token_hash = ?",
                Integer.class, refresh);

        assertThat(encontrados).isZero();
        assertThat(sessaoRepository.findAll())
                .noneMatch(s -> s.toString().contains(refresh));
    }

    @Test
    void c05_refreshValidoDeveGerarNovoPar() throws Exception {
        String primeiro = login(login, SENHA);

        String segundo = refresh(
                JsonPath.read(primeiro, "$.refreshToken"), true);

        assertAll(
                () -> assertThat(JsonPath.<String>read(segundo, "$.accessToken"))
                        .isNotBlank(),
                () -> assertThat(JsonPath.<String>read(segundo, "$.refreshToken"))
                        .isNotBlank(),
                () -> assertThat(JsonPath.<String>read(segundo, "$.accessToken"))
                        .isNotEqualTo(
                                JsonPath.read(primeiro, "$.accessToken")));
    }

    @Test
    void c06_novoRefreshTokenDeveSerDiferenteDoAnterior() throws Exception {
        String primeiro = login(login, SENHA);
        String anterior = JsonPath.read(primeiro, "$.refreshToken");

        String segundo = refresh(anterior, true);

        assertThat(JsonPath.<String>read(segundo, "$.refreshToken"))
                .isNotEqualTo(anterior);
    }

    // ------------------------------------------------------------------
    // 7-10. Rotação, reutilização e persistência da revogação
    // ------------------------------------------------------------------

    @Test
    void c07_refreshAntigoNaoPodeSerUtilizadoNovamente() throws Exception {
        String primeiro = login(login, SENHA);
        String antigo = JsonPath.read(primeiro, "$.refreshToken");
        refresh(antigo, true);

        refresh(antigo, false);
        assertThat(motivos()).contains("REUTILIZACAO");
    }

    @Test
    void c08_reutilizacaoDeveRevogarFamiliaInteira() throws Exception {
        String primeiro = login(login, SENHA);
        String antigo = JsonPath.read(primeiro, "$.refreshToken");
        String familia = familia(antigo);
        refresh(antigo, true);

        refresh(antigo, false);

        assertThat(sessaoRepository.findByFamiliaId(familia))
                .isNotEmpty()
                .allSatisfy(s -> assertAll(
                        () -> assertThat(s.getRevogadaEm()).isNotNull(),
                        () -> assertThat(s.getMotivoRevogacao())
                                .isEqualTo(
                                        com.curso.alsports.model.MotivoRevogacaoRefreshToken.REUTILIZACAO)));
    }

    @Test
    void c09_tokenMaisRecenteDeveDeixarDeRenovarAposReutilizacao()
            throws Exception {

        String primeiro = login(login, SENHA);
        String antigo = JsonPath.read(primeiro, "$.refreshToken");
        String novo = JsonPath.read(refresh(antigo, true), "$.refreshToken");

        refresh(antigo, false);
        refresh(novo, false);

        assertThat(motivos()).contains("REUTILIZACAO");
    }

    @Test
    void c10_revogacaoDevePersistirMesmoComResposta401() throws Exception {
        String primeiro = login(login, SENHA);
        String antigo = JsonPath.read(primeiro, "$.refreshToken");
        String novo = JsonPath.read(refresh(antigo, true), "$.refreshToken");
        String familia = familia(novo);

        refresh(antigo, false);

        List<SessaoRefreshToken> registros =
                sessaoRepository.findByFamiliaId(familia);
        assertAll(
                () -> assertThat(registros).hasSize(2),
                () -> assertThat(registros)
                        .allSatisfy(s -> assertThat(s.getRevogadaEm()).isNotNull()),
                () -> assertThat(motivos()).contains("REUTILIZACAO"));
    }

    // ------------------------------------------------------------------
    // 11-13. Logout
    // ------------------------------------------------------------------

    @Test
    void c11_logoutLocalDeveRevogarFamilia() throws Exception {
        String primeiro = login(login, SENHA);
        String refresh = JsonPath.read(primeiro, "$.refreshToken");
        String familia = familia(refresh);

        logout(refresh, true);
        refresh(refresh, false);

        assertThat(sessaoRepository.findByFamiliaId(familia))
                .allSatisfy(s -> assertThat(s.getMotivoRevogacao())
                        .isEqualTo(com.curso.alsports.model.MotivoRevogacaoRefreshToken.LOGOUT));
    }

    @Test
    void c12_logoutLocalNaoExigeAccessToken() throws Exception {
        String refresh = JsonPath.read(login(login, SENHA), "$.refreshToken");

        mockMvc.perform(post("/api/autenticacao/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(refresh)))
                .andExpect(status().isNoContent());
    }

    @Test
    void c13_logoutGlobalDeveRevogarTodasAsFamiliasAtivas() throws Exception {
        String primeiro = login(login, SENHA);
        String primeiroRefresh = JsonPath.read(primeiro, "$.refreshToken");
        String atual = JsonPath.read(refresh(primeiroRefresh, true), "$.refreshToken");
        String segundo = JsonPath.read(login(login, SENHA), "$.refreshToken");
        String access = JsonPath.read(login(login, SENHA), "$.accessToken");

        mockMvc.perform(post("/api/autenticacao/logout-todos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isNoContent());

        refresh(atual, false);
        refresh(segundo, false);
        assertThat(sessaoRepository.findByContaId(conta.getId()))
                .isNotEmpty()
                .allSatisfy(s -> assertThat(s.getRevogadaEm()).isNotNull());
        assertThat(motivos()).contains("LOGOUT_GLOBAL");
    }

    // ------------------------------------------------------------------
    // 14-15. Conta e pessoa inativas
    // ------------------------------------------------------------------

    @Test
    void c14_contaBloqueadaNaoRenova() throws Exception {
        String refresh = JsonPath.read(login(login, SENHA), "$.refreshToken");

        conta.bloquear();
        contaRepository.saveAndFlush(conta);
        refresh(refresh, false);

        assertThat(motivos()).contains("CONTA_INATIVA");
    }

    @Test
    void c15_pessoaInativaNaoRenova() throws Exception {
        String refresh = JsonPath.read(login(login, SENHA), "$.refreshToken");

        conta.getPessoa().inativar();
        pessoaRepository.saveAndFlush(conta.getPessoa());
        refresh(refresh, false);

        assertThat(motivos()).contains("CONTA_INATIVA");
    }

    // ------------------------------------------------------------------
    // 16-19. Expiração e tokens inválidos
    // ------------------------------------------------------------------

    @Test
    void c16_expiracaoPorInatividade() throws Exception {
        String refresh = JsonPath.read(login(login, SENHA), "$.refreshToken");

        relogio.avancar(Duration.ofMinutes(30));
        refresh(refresh, false);

        assertThat(motivos()).contains("INATIVIDADE");
    }

    @Test
    void c17_expiracaoAbsoluta() throws Exception {
        String primeiro = JsonPath.read(login(login, SENHA), "$.refreshToken");
        String atual = JsonPath.read(refresh(primeiro, true), "$.refreshToken");

        relogio.avancar(Duration.ofHours(8));
        refresh(atual, false);

        assertThat(motivos()).contains("EXPIRACAO_ABSOLUTA");
    }

    @Test
    void c18_refreshDesconhecido() throws Exception {
        refresh("token-desconhecido-" + UUID.randomUUID(), false);
    }

    @Test
    void c19_refreshVazioOuNulo() throws Exception {
        refresh("", false);
        mockMvc.perform(post("/api/autenticacao/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MSG_REFRESH_INVALIDO));

        mockMvc.perform(post("/api/autenticacao/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MSG_REFRESH_INVALIDO));
    }

    // ------------------------------------------------------------------
    // 20-23. Access token, autorização e concorrência
    // ------------------------------------------------------------------

    @Test
    void c20_accessTokenContinuaValidoAposLogoutAteExpirar() throws Exception {
        String primeiro = login(login, SENHA);
        String access = JsonPath.read(primeiro, "$.accessToken");
        String refresh = JsonPath.read(primeiro, "$.refreshToken");

        logout(refresh, true);

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login));

        relogio.avancar(Duration.ofMinutes(31));
        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void c21_usuarioNaoAutenticadoNaoConsegueLogoutGlobal() throws Exception {
        mockMvc.perform(post("/api/autenticacao/logout-todos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void c22_autorizacaoPorPerfisContinuaFuncionando() throws Exception {
        String access = JsonPath.read(login(login, SENHA), "$.accessToken");

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/contas-acesso")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // 23-24. Persistência da rotação e teto absoluto
    // ------------------------------------------------------------------

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void c23_refreshValidoDevePersistirAtividadeSemAlterarExpiracaoDaFamilia()
            throws Exception {

        Instant t0 = relogio.instante();
        String primeiroCorpo = login(login, SENHA);
        String primeiroRefresh = JsonPath.read(primeiroCorpo, "$.refreshToken");
        Instant limiteAbsoluto = Instant.parse(
                JsonPath.<String>read(primeiroCorpo, "$.refreshTokenExpiresAt"));

        Instant instanteRefresh = t0.plus(Duration.ofMinutes(10));
        relogio.avancarPara(instanteRefresh);
        String segundoCorpo = refresh(primeiroRefresh, true);
        String segundoRefresh = JsonPath.read(segundoCorpo, "$.refreshToken");

        List<SessaoRefreshToken> geracoes =
                sessaoRepository.findByContaId(conta.getId());
        SessaoRefreshToken anterior =
                sessaoPorToken(primeiroRefresh, geracoes);
        SessaoRefreshToken nova = sessaoPorToken(segundoRefresh, geracoes);

        assertAll(
                () -> assertThat(anterior.getUtilizadaEm())
                        .isEqualTo(instanteRefresh),
                () -> assertThat(anterior.getUltimaAtividadeEm())
                        .isEqualTo(instanteRefresh),
                () -> assertThat(nova.getUtilizadaEm()).isNull(),
                () -> assertThat(nova.getCriadaEm()).isEqualTo(instanteRefresh),
                () -> assertThat(nova.getUltimaAtividadeEm())
                        .isEqualTo(instanteRefresh),
                () -> assertThat(anterior.getFamiliaId())
                        .isEqualTo(nova.getFamiliaId()),
                () -> assertThat(anterior.getExpiraEm())
                        .isEqualTo(limiteAbsoluto),
                () -> assertThat(nova.getExpiraEm()).isEqualTo(limiteAbsoluto));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void c24_refreshIntermediarioNaoDeveDeslocarLimiteAbsoluto()
            throws Exception {

        Instant t0 = relogio.instante();
        String primeiroCorpo = login(login, SENHA);
        String atual = JsonPath.read(primeiroCorpo, "$.refreshToken");
        Instant limiteAbsoluto = Instant.parse(
                JsonPath.<String>read(primeiroCorpo, "$.refreshTokenExpiresAt"));

        // Idle timeout é 30 min: mantemos a família viva com rotações válidas
        // até t0+7h59 sem alterar as propriedades da aplicação.
        int totalMinutos = 7 * 60 + 59;
        int passoMinutos = 29;
        for (int decorrido = passoMinutos;
                decorrido < totalMinutos;
                decorrido += passoMinutos) {

            relogio.avancarPara(t0.plus(Duration.ofMinutes(decorrido)));
            atual = JsonPath.read(refresh(atual, true), "$.refreshToken");
        }

        Instant instanteUltimoRefresh = t0.plus(Duration.ofMinutes(totalMinutos));
        relogio.avancarPara(instanteUltimoRefresh);
        atual = JsonPath.read(refresh(atual, true), "$.refreshToken");
        String tokenValidoNoTeto = atual;

        String familia = sessaoPorToken(
                tokenValidoNoTeto, sessaoRepository.findByContaId(conta.getId()))
                .getFamiliaId();
        List<SessaoRefreshToken> antes = sessaoRepository.findByFamiliaId(familia);

        assertAll(
                () -> assertThat(antes).hasSizeGreaterThan(1),
                () -> assertThat(antes).allSatisfy(s -> assertThat(s.getExpiraEm())
                        .isEqualTo(limiteAbsoluto)),
                () -> assertThat(sessaoPorToken(tokenValidoNoTeto, antes)
                        .getUltimaAtividadeEm()).isEqualTo(instanteUltimoRefresh));

        Instant instanteTentativa = t0.plus(Duration.ofHours(8).plusMinutes(1));
        relogio.avancarPara(instanteTentativa);
        refresh(tokenValidoNoTeto, false);

        List<SessaoRefreshToken> depois = sessaoRepository.findByFamiliaId(familia);
        assertAll(
                () -> assertThat(depois).isNotEmpty(),
                () -> assertThat(depois).allSatisfy(s -> {
                    assertThat(s.getExpiraEm()).isEqualTo(limiteAbsoluto);
                    assertThat(s.getRevogadaEm()).isEqualTo(instanteTentativa);
                    assertThat(s.getMotivoRevogacao())
                            .isEqualTo(MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA);
                }));
    }

    // ------------------------------------------------------------------
    // Atividade autônoma: conta bloqueada após emissão
    // ------------------------------------------------------------------

    @Test
    void contaBloqueadaAposEmissaoDeveSerRevogadaComMotivoEAccessValidoAteExp()
            throws Exception {

        String primeiro = login(login, SENHA);
        String access = JsonPath.read(primeiro, "$.accessToken");
        String refresh = JsonPath.read(primeiro, "$.refreshToken");

        conta.bloquear();
        contaRepository.saveAndFlush(conta);

        refresh(refresh, false);

        assertThat(sessaoRepository.findByContaId(conta.getId()))
                .allSatisfy(s -> assertThat(s.getMotivoRevogacao())
                        .isEqualTo(com.curso.alsports.model.MotivoRevogacaoRefreshToken.CONTA_INATIVA));

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk());

        relogio.avancar(Duration.ofMinutes(31));
        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private String login(String login, String senha) throws Exception {
        return mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, senha)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String refresh(String refreshToken, boolean esperaSucesso)
            throws Exception {

        var resultado = mockMvc.perform(post("/api/autenticacao/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(refreshToken)));

        if (esperaSucesso) {
            return resultado.andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }

        resultado.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MSG_REFRESH_INVALIDO));
        return "";
    }

    private void logout(String refreshToken, boolean esperaSucesso)
            throws Exception {

        var resultado = mockMvc.perform(post("/api/autenticacao/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(refreshToken)));

        if (esperaSucesso) {
            resultado.andExpect(status().isNoContent());
        } else {
            resultado.andExpect(status().isNoContent());
        }
    }

    private String body(String refreshToken) {
        if (refreshToken == null) {
            return "{}";
        }
        return """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
    }

    private String credenciais(String login, String senha) {
        return """
                {"login": "%s", "senha": "%s"}
                """.formatted(login, senha);
    }

    private String familia(String refreshToken) {
        return sessaoRepository.buscarPorHashParaAtualizacao(hashSha256(refreshToken))
                .orElseThrow().getFamiliaId();
    }

    /**
     * Consulta sem lock e sem transação de teste: adequada aos cenários que
     * precisam observar o que o serviço efetivamente confirmou no banco.
     */
    private SessaoRefreshToken sessaoPorToken(
            String refreshToken, List<SessaoRefreshToken> geracoes) {

        String hash = hashSha256(refreshToken);
        return geracoes.stream()
                .filter(s -> hash.equals(s.getTokenHash()))
                .findFirst()
                .orElseThrow();
    }

    private static String hashSha256(String refreshToken) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não disponível", ex);
        }
    }

    private List<String> motivos() {
        return sessaoRepository.findByContaId(conta.getId()).stream()
                .map(s -> s.getMotivoRevogacao() == null
                        ? "" : s.getMotivoRevogacao().name())
                .toList();
    }

    private Pessoa criarPessoa() {
        long semente = SEQUENCIA.incrementAndGet();
        return pessoaRepository.save(new Pessoa(
                "Ciclo " + semente,
                "ciclo." + semente + "." + System.nanoTime() + "@example.com",
                cpfValido(semente),
                LocalDate.of(1990, 1, 1)));
    }

    private String sufixo() {
        return Long.toString(System.nanoTime());
    }

    private static String cpfValido(long semente) {
        String base = String.format("%09d", Math.abs(semente) % 1_000_000_000L);
        char primeiro = base.charAt(0);
        boolean repetidos = base.chars().allMatch(d -> d == primeiro);
        String nove = repetidos ? "1" + base.substring(1) : base;
        String dez = nove + digito(nove, 10);
        return dez + digito(dez, 11);
    }

    private static int digito(String base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i))
                    * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Relógio mutável e decoder que usa o mesmo relógio para validar exp/nbf.
     * Mantém a validação temporal real, apenas com o tempo controlado pelo teste.
     */
    @TestConfiguration
    static class ConfiguracaoRelogio {

        @Bean
        @Primary
        RelogioTeste relogioTeste() {
            return new RelogioTeste(Instant.parse("2026-11-17T12:00:00Z"));
        }

        @Bean("clockControlado")
        @Primary
        Clock clockControlado(RelogioTeste relogioTeste) {
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return relogioTeste.instante();
                }
            };
        }

    }

    static class RelogioTeste {
        private volatile Instant instante;

        RelogioTeste(Instant inicial) {
            this.instante = inicial;
        }

        Instant instante() {
            return instante;
        }

        void avancarPara(Instant novo) {
            this.instante = novo;
        }

        void avancar(Duration duration) {
            this.instante = instante.plus(duration);
        }
    }
}
