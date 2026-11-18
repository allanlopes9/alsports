package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.curso.alsports.config.ProtecaoAutenticacaoProperties;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.EventoSeguranca;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.ProtecaoAutenticacao;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.EventoSegurancaRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.curso.alsports.repository.ProtecaoAutenticacaoRepository;
import com.curso.alsports.service.AuditoriaSegurancaService;
import com.curso.alsports.service.ProtecaoAutenticacaoService;
import com.curso.alsports.service.RegistradorEventosSeguranca;
import com.jayway.jsonpath.JsonPath;

/**
 * Integração HTTP real da Aula 17 com PostgreSQL do profile test.
 *
 * <p>Todos os dados são exclusivos desta classe e removidos explicitamente ao
 * final de cada teste. O relógio é controlado para provar janela, expiração e
 * bloqueio sem espera real. Nenhuma credencial ou token é registrado nos
 * eventos consultados pela API.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Aula17ProtecaoAuditoriaTest {

    private static final String SENHA = "aula17-senha-didatica-2026";
    private static final String SENHA_ERRADA = "senha-errada-aula17-2026";
    private static final String MSG_GENERICA = "Login ou senha inválidos";
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
    private EventoSegurancaRepository eventoRepository;

    @Autowired
    private ProtecaoAutenticacaoRepository protecaoRepository;

    @Autowired
    private AuditoriaSegurancaService auditoriaConsulta;

    @Autowired
    private RegistradorEventosSeguranca auditoria;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RelogioTeste relogio;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ProtecaoAutenticacaoProperties properties;

    private final Set<String> hashes = new LinkedHashSet<>();
    private final Set<Long> contaIds = new LinkedHashSet<>();
    private final Set<Long> pessoaIds = new LinkedHashSet<>();

    private long eventoIdInicial;
    private String login;
    private String loginAdmin;

    @BeforeEach
    void setUp() {
        eventoIdInicial = maiorIdEvento();
        relogio.avancarPara(Instant.parse("2026-11-17T12:00:00Z"));

        Pessoa pessoaAdmin = criarPessoa("admin");
        loginAdmin = "aula17.admin." + sufixo();
        criarConta(pessoaAdmin, loginAdmin, Set.of(PerfilAcesso.ADMINISTRADOR));

        Pessoa pessoa = criarPessoa("operador");
        login = "aula17.op." + sufixo();
        criarConta(pessoa, login, Set.of(PerfilAcesso.OPERADOR));
    }

    @AfterEach
    void limparDadosExclusivos() {
        for (String hash : hashes) {
            jdbcTemplate.update(
                    "delete from evento_seguranca where identificador_hash = ?", hash);
        }
        for (Long id : contaIds) {
            jdbcTemplate.update(
                    "delete from evento_seguranca where ator_conta_id = ? or alvo_conta_id = ?",
                    id, id);
        }
        for (String hash : hashes) {
            jdbcTemplate.update(
                    "delete from protecao_autenticacao where chave_login = ?", hash);
        }
        for (Long id : contaIds) {
            jdbcTemplate.update(
                    "delete from sessao_refresh_token where conta_acesso_id = ?", id);
            jdbcTemplate.update("delete from conta_acesso where id = ?", id);
        }
        for (Long id : pessoaIds) {
            jdbcTemplate.update("delete from pessoa where id = ?", id);
        }
        jdbcTemplate.update(
                "delete from evento_seguranca where id > ?", eventoIdInicial);
        hashes.clear();
        contaIds.clear();
        pessoaIds.clear();
    }

    @Test
    void loginInexistenteDeveSerGenericoEPersistirFalhaSemVazarSegredos() throws Exception {
        String loginInexistente = "aula17.inexistente." + sufixo();
        hashes.add(hash(loginInexistente));

        String corpo = tentarLogin(loginInexistente, SENHA_ERRADA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MSG_GENERICA))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(loginInexistente))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(SENHA_ERRADA))))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo)
                .doesNotContain("$2a$", "accessToken", "refreshToken",
                        "identificadorHash", "Authorization");

        ProtecaoAutenticacao protecao = protecaoRepository
                .findByChaveLogin(hash(loginInexistente))
                .orElseThrow();
        assertThat(protecao.getFalhasConsecutivas()).isEqualTo(1);
        assertThat(contar(TipoEventoSeguranca.LOGIN_FALHA)).isEqualTo(1);
        assertThat(eventosDoTeste())
                .noneMatch(evento -> evento.toString().contains(loginInexistente));
    }

    @Test
    void senhaIncorretaEContaBloqueadaAdministrativamenteDevemSerGenericas() throws Exception {
        tentarLogin(login, SENHA_ERRADA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MSG_GENERICA));

        ContaAcesso conta = contaRepository.findByLoginIgnoreCase(login).orElseThrow();
        conta.bloquear();
        contaRepository.saveAndFlush(conta);

        tentarLogin(login, SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MSG_GENERICA));

        assertThat(protecao().getFalhasConsecutivas()).isEqualTo(2);
        assertThat(contar(TipoEventoSeguranca.LOGIN_FALHA)).isEqualTo(2);
    }

    @Test
    void cincoFalhasDevemBloquearESextaTentativaCorretaDeveContinuar401() throws Exception {
        for (int i = 0; i < 5; i++) {
            tentarLogin(login, SENHA_ERRADA)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(MSG_GENERICA));
        }

        assertThat(protecao().getFalhasConsecutivas()).isEqualTo(5);
        assertThat(protecao().estaBloqueada(relogio.instante())).isTrue();
        assertThat(contar(TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO)).isEqualTo(1);

        tentarLogin(login, SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MSG_GENERICA));

        assertThat(protecao().getFalhasConsecutivas()).isEqualTo(5);
        assertThat(contar(TipoEventoSeguranca.LOGIN_RECUSADO_BLOQUEIO)).isEqualTo(1);

        relogio.avancar(Duration.ofMinutes(16));

        String corpo = tentarLogin(login, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(corpo, "$.accessToken")).isNotBlank();
        assertThat(protecao().getFalhasConsecutivas()).isZero();
        assertThat(protecao().getBloqueadaAte()).isNull();
        assertThat(contar(TipoEventoSeguranca.LOGIN_SUCESSO)).isEqualTo(1);
    }

    @Test
    void falhaAposExpiracaoDaJanelaDeveReiniciarContador() throws Exception {
        for (int i = 0; i < 4; i++) {
            tentarLogin(login, SENHA_ERRADA).andExpect(status().isUnauthorized());
        }
        relogio.avancar(Duration.ofMinutes(16));
        tentarLogin(login, SENHA_ERRADA).andExpect(status().isUnauthorized());

        assertThat(protecao().getFalhasConsecutivas()).isEqualTo(1);
        assertThat(contar(TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO)).isZero();

        for (int i = 0; i < 4; i++) {
            tentarLogin(login, SENHA_ERRADA).andExpect(status().isUnauthorized());
        }

        assertThat(protecao().getFalhasConsecutivas()).isEqualTo(5);
        assertThat(contar(TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO)).isEqualTo(1);
    }

    @Test
    void auditoriaDeveExigirAdministradorFiltrarPaginarEOcultarHash() throws Exception {
        String tokenOperador = tokenDe(tentarLogin(login, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        tentarLogin(login, SENHA_ERRADA).andExpect(status().isUnauthorized());
        tentarLogin(login, SENHA_ERRADA).andExpect(status().isUnauthorized());

        String corpo = mockMvc.perform(get("/api/auditoria-seguranca")
                        .param("tipo", "LOGIN_FALHA")
                        .param("page", "0")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("LOGIN_FALHA"))
                .andExpect(jsonPath("$.content[0].identificadorHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].senha").doesNotExist())
                .andExpect(jsonPath("$.content[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$.content[0].refreshToken").doesNotExist())
                .andExpect(jsonPath("$.content[0].mensagem").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(login))))
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<Integer>read(corpo, "$.totalElements")).isGreaterThanOrEqualTo(2);

        mockMvc.perform(get("/api/auditoria-seguranca")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/auditoria-seguranca")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOperador))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auditoria-seguranca").param("tipo", "TIPO_INVALIDO")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/auditoria-seguranca").param("page", "-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/auditoria-seguranca").param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());

        assertThatThrownBy(() -> auditoriaConsulta.consultar(null, 0, 20))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void servicoDeAuditoriaDeveExigirAdministradorViaPreAuthorize() {
        assertThatThrownBy(() -> auditoriaConsulta.consultar(null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void operadorSemPerfilAdministradorNaoDeveConsultarAuditoria() throws Exception {
        String tokenOperador = tokenDe(tentarLogin(login, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/auditoria-seguranca")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOperador))
                .andExpect(status().isForbidden());
    }

    @Test
    void operacoesAdministrativasDevemRegistrarEventosNaMesmaTransacao() throws Exception {
        String adminToken = tokenAdmin();
        Pessoa novaPessoa = criarPessoa("nova-conta");
        String novoLogin = "aula17.nova." + sufixo();
        hashes.add(hash(novoLogin));

        String criada = mockMvc.perform(post("/api/contas-acesso")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pessoaId": %d, "login": "%s", "senha": "%s"}
                                """.formatted(novaPessoa.getId(), novoLogin, SENHA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long novaContaId = ((Number) JsonPath.read(criada, "$.id")).longValue();
        contaIds.add(novaContaId);

        String tokenOperador = tokenDe(tentarLogin(login, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(put("/api/contas-acesso/me/senha")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOperador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senhaAtual": "%s", "novaSenha": "aula17-senha-trocada-2026"}
                                """.formatted(SENHA)))
                .andExpect(status().isNoContent());

        ContaAcesso alvo = contaRepository.findByLoginIgnoreCase(login).orElseThrow();
        mockMvc.perform(put("/api/contas-acesso/" + alvo.getId() + "/perfis")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"perfis\": [\"OPERADOR\", \"ADMINISTRADOR\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/contas-acesso/" + alvo.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLOQUEADA\"}"))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(eventosDoTeste()).anyMatch(e ->
                        e.getTipo() == TipoEventoSeguranca.CONTA_CRIADA
                                && novaContaId.equals(e.getAlvoContaId())),
                () -> assertThat(contar(TipoEventoSeguranca.SENHA_ALTERADA)).isEqualTo(1),
                () -> assertThat(contar(TipoEventoSeguranca.PERFIS_ALTERADOS)).isEqualTo(1),
                () -> assertThat(contar(TipoEventoSeguranca.STATUS_ALTERADO)).isEqualTo(1));
    }

    @Test
    void refreshLogoutELogoutGlobalDevemGerarEventosSemSegredos() throws Exception {
        long idInicial = maiorIdEvento();

        String primeiro = tentarLogin(login, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refresh = JsonPath.read(primeiro, "$.refreshToken");
        String access = JsonPath.read(primeiro, "$.accessToken");

        String renovado = mockMvc.perform(post("/api/autenticacao/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"%s\"}".formatted(refresh)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String novoRefresh = JsonPath.read(renovado, "$.refreshToken");

        mockMvc.perform(post("/api/autenticacao/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"refresh-inexistente-aula17\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/autenticacao/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"%s\"}".formatted(novoRefresh)))
                .andExpect(status().isNoContent());

        String outro = tentarLogin(login, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/autenticacao/logout-todos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + JsonPath.read(outro, "$.accessToken")))
                .andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(contarDesde(
                        TipoEventoSeguranca.REFRESH_SUCESSO, idInicial)).isEqualTo(1),
                () -> assertThat(contarDesde(
                        TipoEventoSeguranca.REFRESH_FALHA, idInicial)).isEqualTo(1),
                () -> assertThat(contarDesde(
                        TipoEventoSeguranca.LOGOUT, idInicial)).isEqualTo(1),
                () -> assertThat(contarDesde(
                        TipoEventoSeguranca.LOGOUT_GLOBAL, idInicial)).isEqualTo(1),
                () -> assertThat(eventosDoTeste())
                        .noneMatch(evento -> evento.toString().contains(refresh)
                                || evento.toString().contains(novoRefresh)
                                || evento.toString().contains(access)));
    }

    @Test
    void duasFalhasConcorrentesNaoDevemPerderAtualizacaoDoContador() throws Exception {
        for (int i = 0; i < 3; i++) {
            tentarLogin(login, SENHA_ERRADA).andExpect(status().isUnauthorized());
        }
        assertThat(protecao().getFalhasConsecutivas()).isEqualTo(3);

        String chave = hash(login);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch liberar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> primeira = executor.submit(
                    () -> registrarFalhaCoordenada(chave, prontas, liberar));
            Future<Boolean> segunda = executor.submit(
                    () -> registrarFalhaCoordenada(chave, prontas, liberar));

            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            liberar.countDown();

            assertThat(primeira.get(30, TimeUnit.SECONDS)).isNotNull();
            assertThat(segunda.get(30, TimeUnit.SECONDS)).isNotNull();
        } finally {
            liberar.countDown();
            executor.shutdownNow();
        }

        assertThat(protecao().getFalhasConsecutivas()).isEqualTo(5);
        assertThat(contar(TipoEventoSeguranca.LOGIN_FALHA)).isEqualTo(5);
        assertThat(contar(TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO)).isEqualTo(1);
    }

    private boolean registrarFalhaCoordenada(
            String chave, CountDownLatch prontas, CountDownLatch liberar) throws Exception {

        TransactionTemplate transacao = new TransactionTemplate(transactionManager);
        transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return transacao.execute(status -> {
            prontas.countDown();
            try {
                if (!liberar.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Liberação coordenada expirou");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }

            ProtecaoAutenticacao protecao = protecaoRepository
                    .buscarParaAtualizacao(chave)
                    .orElseThrow(() -> new IllegalStateException(
                            "Contador não encontrado"));

            boolean ativou = protecao.registrarFalha(
                    relogio.instante(),
                    properties.maxFailedAttempts(),
                    properties.failureWindow(),
                    properties.lockDuration());
            protecaoRepository.save(protecao);

            auditoria.registrar(
                    TipoEventoSeguranca.LOGIN_FALHA,
                    relogio.instante(),
                    null,
                    null,
                    chave,
                    "127.0.0.1");
            if (ativou) {
                auditoria.registrar(
                        TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO,
                        relogio.instante(),
                        null,
                        null,
                        chave,
                        "127.0.0.1");
            }
            return ativou;
        });
    }

    private ResultActions tentarLogin(String login, String senha) throws Exception {
        return mockMvc.perform(post("/api/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credenciais(login, senha)));
    }

    private String tokenAdmin() throws Exception {
        return tokenDe(tentarLogin(loginAdmin, SENHA)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private static String tokenDe(String corpo) {
        return JsonPath.read(corpo, "$.accessToken");
    }

    private static String credenciais(String login, String senha) {
        return """
                {"login": "%s", "senha": "%s"}
                """.formatted(login, senha);
    }

    private ProtecaoAutenticacao protecao() {
        return protecaoRepository.findByChaveLogin(hash(login)).orElseThrow();
    }

    private long contar(TipoEventoSeguranca tipo) {
        return eventosDoTeste().stream()
                .filter(evento -> evento.getTipo() == tipo)
                .count();
    }

    /**
     * Conta eventos criados a partir de um ID, inclusive os que não carregam
     * correlacionador (falha de refresh com token inexistente).
     */
    private long contarDesde(TipoEventoSeguranca tipo, long idInicial) {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from evento_seguranca "
                + "where id > ? and tipo = ?",
                Long.class, idInicial, tipo.name());
        return total == null ? 0L : total;
    }

    private long maiorIdEvento() {
        Long maior = jdbcTemplate.queryForObject(
                "select coalesce(max(id), 0) from evento_seguranca",
                Long.class);
        return maior == null ? 0L : maior;
    }

    private List<EventoSeguranca> eventosDoTeste() {
        return eventoRepository.findAll().stream()
                .filter(evento -> evento.getIdentificadorHash() != null
                        && hashes.contains(evento.getIdentificadorHash())
                        || evento.getAtorContaId() != null
                        && contaIds.contains(evento.getAtorContaId())
                        || evento.getAlvoContaId() != null
                        && contaIds.contains(evento.getAlvoContaId()))
                .toList();
    }

    private String hash(String login) {
        return ProtecaoAutenticacaoService.calcularHashLogin(login);
    }

    private Pessoa criarPessoa(String prefixo) {
        long semente = 700_000_000L + SEQUENCIA.incrementAndGet();
        Pessoa pessoa = pessoaRepository.save(new Pessoa(
                "Aula17 " + prefixo + " " + semente,
                prefixo + "." + semente + "." + System.nanoTime() + "@example.com",
                cpfValido(semente),
                LocalDate.of(1990, 1, 1)));
        pessoaIds.add(pessoa.getId());
        return pessoa;
    }

    private ContaAcesso criarConta(
            Pessoa pessoa, String login, Set<PerfilAcesso> perfis) {

        ContaAcesso conta = new ContaAcesso(
                pessoa, login, passwordEncoder.encode(SENHA));
        conta.substituirPerfis(perfis);
        ContaAcesso salva = contaRepository.saveAndFlush(conta);
        contaIds.add(salva.getId());
        hashes.add(hash(login));
        return salva;
    }

    private static String sufixo() {
        return Long.toString(System.nanoTime());
    }

    private static String cpfValido(long semente) {
        String base = String.format(Locale.ROOT, "%09d", semente % 1_000_000_000L);
        char primeiro = base.charAt(0);
        boolean repetidos = base.chars().allMatch(d -> d == primeiro);
        String nove = repetidos ? "1" + base.substring(1) : base;
        String dez = nove + digito(nove, 10);
        return dez + digito(dez, 11);
    }

    private static int digito(String base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Relógio mutável usado por emissão e validação JWT e pela política de
     * bloqueio. O tempo é controlado pelo teste, sem Thread.sleep.
     */
    @TestConfiguration
    static class ConfiguracaoRelogio {

        @Bean
        @Primary
        RelogioTeste relogioTeste() {
            return new RelogioTeste(Instant.parse("2026-11-17T12:00:00Z"));
        }

        @Bean
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
