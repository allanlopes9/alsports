package com.curso.alsports;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.SessaoRefreshToken;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.curso.alsports.repository.SessaoRefreshTokenRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Concorrência real com PostgreSQL: duas requisições simultâneas disputam o
 * mesmo refresh token e o bloqueio pessimista garante um único sucessor válido.
 *
 * <p>Sem {@code @Transactional} na classe: cada chamada MockMvc participa de
 * sua própria transação, como acontece em produção.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CicloTokenConcorrenciaTest {

    private static final String SENHA = "concorrencia-senha-2026";

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
    private TransactionTemplate transactionTemplate;

    private Long contaId;

    @BeforeEach
    void setUp() {
        long semente = Math.abs(UUID.randomUUID().getMostSignificantBits());
        Pessoa pessoa = pessoaRepository.save(new Pessoa(
                "Concorrência",
                "concorrencia." + UUID.randomUUID() + "@example.com",
                cpfValido(semente),
                LocalDate.of(1990, 1, 1)));
        ContaAcesso conta = contaRepository.saveAndFlush(new ContaAcesso(
                pessoa,
                "concorrencia." + UUID.randomUUID(),
                PasswordEncoderFactories.createDelegatingPasswordEncoder()
                        .encode(SENHA)));
        contaId = conta.getId();
    }

    @AfterEach
    void limpar() {
        jdbcTemplate.update(
                "delete from sessao_refresh_token where conta_acesso_id = ?",
                contaId);
        jdbcTemplate.update("delete from conta_acesso where id = ?", contaId);
    }

    @Test
    void duasRenovacoesConcorrentesNaoCriamDoisSucessoresValidos()
            throws Exception {

        String corpo = mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "%s"}
                                """.formatted(
                                        contaRepository.findById(contaId)
                                                .orElseThrow().getLogin(),
                                        SENHA)))
                .andReturn().getResponse().getContentAsString();

        String refresh = JsonPath.read(corpo, "$.refreshToken");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(refresh.getBytes(StandardCharsets.UTF_8)));
        String familia = transactionTemplate.execute(status ->
                sessaoRepository.buscarPorHashParaAtualizacao(hash)
                        .orElseThrow().getFamiliaId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            Callable<Integer> tarefa = () -> {
                largada.await();
                return mockMvc.perform(post("/api/autenticacao/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken": "%s"}
                                        """.formatted(refresh)))
                        .andReturn().getResponse().getStatus();
            };

            Future<Integer> f1 = executor.submit(tarefa);
            Future<Integer> f2 = executor.submit(tarefa);
            largada.countDown();

            List<Integer> status = List.of(
                    f1.get(60, SECONDS), f2.get(60, SECONDS));

            List<SessaoRefreshToken> registros =
                    sessaoRepository.findByFamiliaId(familia);

            assertThat(status).containsExactlyInAnyOrder(200, 401);
            assertThat(registros).hasSize(2);
            assertThat(registros).filteredOn(s -> s.getUtilizadaEm() != null)
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private static String cpfValido(long semente) {
        String base = String.format("%09d", semente % 1_000_000_000L);
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
}
