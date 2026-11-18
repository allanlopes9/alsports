package com.curso.alsports;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.curso.alsports.exception.OperacaoSegurancaRecusadaException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Status;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.curso.alsports.service.ContaAcessoService;

/**
 * Duas transações reais disputam a remoção do último administrador efetivo.
 *
 * <p>Cada teste usa um database PostgreSQL exclusivo, criado antes da abertura
 * do contexto. A sincronização é feita apenas com {@link CountDownLatch}; não
 * há {@code Thread.sleep} nem espera por tempo arbitrário.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class AdministradorInvarianteConcorrenciaTest {

    private static final String BASE =
            "jdbc:postgresql://localhost:5432/alsports_test";
    private static final String NOME_BANCO = "alsports_a17_inv_"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private static final String URL = "jdbc:postgresql://localhost:5432/"
            + NOME_BANCO;
    private static final String SENHA = "invariante-senha-2026";

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private ContaAcessoService contaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String loginPrimeiro;
    private String loginSegundo;
    private Long idPrimeiro;
    private Long idSegundo;

    @DynamicPropertySource
    static void datasourceExclusiva(DynamicPropertyRegistry registry) {
        criarBanco();
        registry.add("spring.datasource.url", () -> URL);
    }

    @AfterAll
    static void removerBanco() {
        executarNaBase("drop database if exists " + NOME_BANCO + " with (force)");
    }

    @BeforeEach
    void setUp() {
        loginPrimeiro = "inv.admin.um." + sufixo();
        loginSegundo = "inv.admin.dois." + sufixo();

        idPrimeiro = criarAdministrador(
                "Primeiro", loginPrimeiro, 810_000_001L).getId();
        idSegundo = criarAdministrador(
                "Segundo", loginSegundo, 810_000_002L).getId();

        assertThat(contarAdministradoresEfetivos()).isEqualTo(2L);
    }

    @Test
    void remocoesConcorrentesDeAdministradoresDiferentesDevemPreservarUm()
            throws Exception {
        CountDownLatch prontas = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Resultado> primeiro = executor.submit(tarefa(
                    idSegundo, loginPrimeiro, prontas));
            Future<Resultado> segundo = executor.submit(tarefa(
                    idPrimeiro, loginSegundo, prontas));

            List<Resultado> resultados = List.of(
                    primeiro.get(60, SECONDS), segundo.get(60, SECONDS));

            assertThat(resultados)
                    .extracting(Resultado::desfecho)
                    .containsExactlyInAnyOrder("OK", "RECUSADO");

            Resultado recusado = resultados.stream()
                    .filter(resultado -> "RECUSADO".equals(resultado.desfecho()))
                    .findFirst()
                    .orElseThrow();
            Resultado aceito = resultados.stream()
                    .filter(resultado -> "OK".equals(resultado.desfecho()))
                    .findFirst()
                    .orElseThrow();

            assertThat(contaRepository.findById(aceito.contaId())
                    .orElseThrow().getStatus())
                    .isEqualTo(StatusContaAcesso.BLOQUEADA);
            assertThat(contaRepository.findById(recusado.contaId())
                    .orElseThrow().getStatus())
                    .isEqualTo(StatusContaAcesso.ATIVA);

            assertThat(contarAdministradoresEfetivos()).isEqualTo(1L);
            assertThat(contarEventosRecusados()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
            SecurityContextHolder.clearContext();
        }
    }

    private Callable<Resultado> tarefa(
            Long alvoId, String loginAdministrador, CountDownLatch prontas) {

        return () -> {
            TransactionTemplate transacao = new TransactionTemplate(transactionManager);
            transacao.setPropagationBehavior(
                    TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transacao.setTimeout(60);

            return transacao.execute(status -> {
                instalarAdministrador(loginAdministrador);
                prontas.countDown();

                try {
                    aguardar(prontas);

                    contaService.alterarStatus(
                            alvoId,
                            StatusContaAcesso.BLOQUEADA,
                            loginAdministrador,
                            "10.0.0.17");
                    return new Resultado("OK", alvoId);
                } catch (OperacaoSegurancaRecusadaException ex) {
                    return new Resultado("RECUSADO", alvoId);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        };
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(30, SECONDS)) {
                throw new IllegalStateException(
                        "As duas transações não alcançaram a largada");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static void instalarAdministrador(String login) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        login,
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"))));
        SecurityContextHolder.setContext(contexto);
    }

    private ContaAcesso criarAdministrador(
            String nome, String login, long semente) {

        Pessoa pessoa = pessoaRepository.save(new Pessoa(
                "Administrador " + nome,
                login + "@example.com",
                cpfValido(semente),
                LocalDate.of(1990, 1, 1)));
        ContaAcesso conta = new ContaAcesso(
                pessoa,
                login,
                PasswordEncoderFactories.createDelegatingPasswordEncoder()
                        .encode(SENHA));
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        return contaRepository.saveAndFlush(conta);
    }

    private long contarAdministradoresEfetivos() {
        Long total = jdbcTemplate.queryForObject(
                "select count(distinct ca.id) "
                + "from conta_acesso ca "
                + "join conta_acesso_perfil p on p.conta_acesso_id = ca.id "
                + "join pessoa pe on pe.id = ca.pessoa_id "
                + "where p.perfil = 'ADMINISTRADOR' "
                + "and ca.status = 'ATIVA' and pe.status = 'ATIVO'",
                Long.class);
        return total == null ? 0L : total;
    }

    private long contarEventosRecusados() {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from evento_seguranca "
                + "where tipo = 'OPERACAO_RECUSADA'",
                Long.class);
        return total == null ? 0L : total;
    }

    private static void criarBanco() {
        executarNaBase("create database " + NOME_BANCO);
    }

    private static void executarNaBase(String sql) {
        String usuario = System.getenv("DB_TEST_USERNAME");
        if (usuario == null || usuario.isBlank()) {
            usuario = "alsports_app";
        }
        String senha = System.getenv("DB_TEST_PASSWORD");
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException("DB_TEST_PASSWORD não disponível");
        }

        try (Connection conexao = DriverManager.getConnection(BASE, usuario, senha);
             Statement st = conexao.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Falha ao preparar database de teste: " + sql, ex);
        }
    }

    private static String sufixo() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static String cpfValido(long semente) {
        String base = String.format(Locale.ROOT, "%09d", semente % 1_000_000_000L);
        char primeiro = base.charAt(0);
        boolean repetidos = base.chars().allMatch(digito -> digito == primeiro);
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

    private record Resultado(String desfecho, Long contaId) {
    }
}
