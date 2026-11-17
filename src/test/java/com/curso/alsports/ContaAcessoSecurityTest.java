package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContaAcessoSecurityTest {

    private static final String SENHA = "uma-frase-senha-2026";
    private static final String NOVA_SENHA = "outra-frase-senha-2026";
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaAcessoRepository;

    private Pessoa pessoa;
    private ContaAcesso conta;
    private String login;

    @BeforeEach
    void setUp() {
        pessoa = pessoaRepository.save(criarPessoa("Operadora Teste"));
        login = "operador." + sufixo();
        conta = contaAcessoRepository.saveAndFlush(new ContaAcesso(
                pessoa, login, passwordEncoder.encode(SENHA)));
    }

    @Test
    void healthDeveSerPublico() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void openApiDeveSerPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AL Sports API"));
    }

    @Test
    void rotaProtegidaSemCredencialDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        "WWW-Authenticate", containsString("Bearer")))
                .andExpect(content().string(not(containsString("senha"))))
                .andExpect(content().string(not(containsString("senhaHash"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void cadastroDePessoaDeveSerPublico() throws Exception {
        String email = "publico." + sufixo() + "@example.com";
        String cpf = cpfValido(SEQUENCIA.incrementAndGet());

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Pessoa Pública",
                                  "email": "%s",
                                  "cpf": "%s"
                                }
                                """.formatted(email, cpf)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.cpf").value(cpf));
    }

    @Test
    void cadastroDeContaDeveSerPublicoEPersistirSomenteBcrypt()
            throws Exception {

        Pessoa novaPessoa = pessoaRepository.save(
                criarPessoa("Nova Operadora"));
        String novoLogin = "nova." + sufixo();

        mockMvc.perform(post("/api/contas-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pessoaId": %d,
                                  "login": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(
                                        novaPessoa.getId(),
                                        novoLogin,
                                        SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.pessoaId").value(novaPessoa.getId()))
                .andExpect(jsonPath("$.nomePessoa").value("Nova Operadora"))
                .andExpect(jsonPath("$.login").value(novoLogin))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.perfis[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.perfis[1]").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(content().string(not(containsString(SENHA))))
                .andExpect(content().string(not(containsString("$2a$"))));

        ContaAcesso salva = contaAcessoRepository
                .findByLoginIgnoreCase(novoLogin)
                .orElseThrow();

        assertThat(salva.getSenhaHash())
                .isNotEqualTo(SENHA)
                .startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(SENHA, salva.getSenhaHash()))
                .isTrue();
    }

    @Test
    void requestPublicoNaoDeveEscolherPerfil() throws Exception {
        Pessoa novaPessoa = pessoaRepository.save(
                criarPessoa("Operadora Sem Admin"));
        String novoLogin = "semadmin." + sufixo();

        mockMvc.perform(post("/api/contas-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pessoaId": %d,
                                  "login": "%s",
                                  "senha": "%s",
                                  "perfis": ["ADMINISTRADOR"]
                                }
                                """.formatted(
                                        novaPessoa.getId(),
                                        novoLogin,
                                        SENHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfis[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.perfis[1]").doesNotExist());
    }

    @Test
    void bearerValidoDevePermitirConsultarMinhaConta() throws Exception {
        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.pessoaId").value(pessoa.getId()))
                .andExpect(jsonPath("$.nomePessoa").value("Operadora Teste"))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.perfis[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(content().string(not(containsString(SENHA))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void loginDeveSerAvaliadoSemDiferenciarCaixa() throws Exception {
        String token = autenticar(
                login.toUpperCase(Locale.ROOT), SENHA);

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login));
    }

    @Test
    void senhaIncorretaNoLoginDeveRetornar401() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "%s",
                                  "senha": "senha-incorreta-2026"
                                }
                                """.formatted(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Login ou senha inválidos"))
                .andExpect(content().string(not(containsString("senha-incorreta-2026"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void loginInexistenteDeveRetornar401() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "nao.existe.%s",
                                  "senha": "%s"
                                }
                                """.formatted(sufixo(), SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Login ou senha inválidos"));
    }

    @Test
    void contaBloqueadaDeveRetornar401() throws Exception {
        conta.bloquear();
        contaAcessoRepository.saveAndFlush(conta);

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(login, SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Login ou senha inválidos"));
    }

    @Test
    void pessoaInativaDeveRetornar401() throws Exception {
        pessoa.inativar();
        pessoaRepository.saveAndFlush(pessoa);

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(login, SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Login ou senha inválidos"));
    }

    @Test
    void rotaProtegidaComBearerValidoDeveRetornar200() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void listagemDePessoasSemCredencialDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/pessoas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alteracaoDeSenhaDeveRetornar204EInvalidarSenhaAnterior()
            throws Exception {

        mockMvc.perform(put("/api/contas-acesso/me/senha")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "%s",
                                  "novaSenha": "%s"
                                }
                                """.formatted(SENHA, NOVA_SENHA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(login, SENHA)))
                .andExpect(status().isUnauthorized());

        String tokenNovo = autenticar(login, NOVA_SENHA);

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenNovo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login));
    }

    @Test
    void alteracaoComSenhaAtualIncorretaDeveRetornar400() throws Exception {
        mockMvc.perform(put("/api/contas-acesso/me/senha")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "senha-incorreta-2026",
                                  "novaSenha": "%s"
                                }
                                """.formatted(NOVA_SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Senha atual inválida"));

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void alteracaoComNovaSenhaIgualDeveRetornar400() throws Exception {
        mockMvc.perform(put("/api/contas-acesso/me/senha")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "%s",
                                  "novaSenha": "%s"
                                }
                                """.formatted(SENHA, SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Nova senha deve ser diferente da senha atual"));

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void novaSenhaComMaisDe72BytesUtf8DeveRetornar400() throws Exception {
        mockMvc.perform(put("/api/contas-acesso/me/senha")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "%s",
                                  "novaSenha": "%s"
                                }
                                """.formatted(SENHA, "á".repeat(37))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Senha deve possuir no máximo 72 bytes em UTF-8"));
    }

    @Test
    void cadastroComSenhaCurtaDeveRetornar400() throws Exception {
        Pessoa novaPessoa = pessoaRepository.save(
                criarPessoa("Senha Curta"));

        mockMvc.perform(post("/api/contas-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pessoaId": %d,
                                  "login": "senha.curta.%s",
                                  "senha": "curta"
                                }
                                """.formatted(
                                        novaPessoa.getId(),
                                        sufixo())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.senha").exists());
    }

    @Test
    void loginDuplicadoDeveRetornar409() throws Exception {
        Pessoa outraPessoa = pessoaRepository.save(
                criarPessoa("Outra Operadora"));

        mockMvc.perform(post("/api/contas-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pessoaId": %d,
                                  "login": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(
                                        outraPessoa.getId(),
                                        login.toUpperCase(Locale.ROOT),
                                        SENHA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Login já cadastrado"));
    }

    @Test
    void pessoaDuplicadaDeveRetornar409() throws Exception {
        mockMvc.perform(post("/api/contas-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pessoaId": %d,
                                  "login": "outro.%s",
                                  "senha": "%s"
                                }
                                """.formatted(
                                        pessoa.getId(),
                                        sufixo(),
                                        SENHA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Pessoa já possui conta de acesso"));
    }

    @Test
    void pessoaInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(post("/api/contas-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pessoaId": 999999999,
                                  "login": "inexistente.%s",
                                  "senha": "%s"
                                }
                                """.formatted(sufixo(), SENHA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Pessoa não encontrada: 999999999"));
    }

    @Test
    void pessoaInativaNaoDeveCriarConta() throws Exception {
        Pessoa inativa = criarPessoa("Pessoa Inativa");
        inativa.inativar();
        pessoaRepository.saveAndFlush(inativa);

        mockMvc.perform(post("/api/contas-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pessoaId": %d,
                                  "login": "inativa.%s",
                                  "senha": "%s"
                                }
                                """.formatted(
                                        inativa.getId(),
                                        sufixo(),
                                        SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Pessoa inativa não pode possuir conta de acesso"));
    }

    private String autenticar(String login, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(login, senha)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(resposta, "$.accessToken");
    }

    private Pessoa criarPessoa(String nome) {
        long sequencia = SEQUENCIA.incrementAndGet();
        String email = "pessoa." + sequencia + "." + System.nanoTime()
                + "@example.com";
        String cpf = cpfValido(sequencia);

        return new Pessoa(nome, email, cpf, LocalDate.now());
    }

    private String sufixo() {
        return Long.toString(System.nanoTime());
    }

    private String cpfValido(long semente) {
        String base = String.format(
                "%09d", Math.abs(semente) % 1_000_000_000L);
        char primeiro = base.charAt(0);
        boolean repetidos = base.chars()
                .allMatch(digito -> digito == primeiro);
        String nove = repetidos ? "1" + base.substring(1) : base;

        String dez = nove + digitoCpf(nove, 10);
        return dez + digitoCpf(dez, 11);
    }

    private int digitoCpf(String base, int pesoInicial) {
        int soma = 0;

        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i))
                    * (pesoInicial - i);
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
