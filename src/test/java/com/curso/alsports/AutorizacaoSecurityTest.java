package com.curso.alsports;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Verifica a política HTTP: 401 para ausência de autenticação, 403 para
 * autenticação sem privilégio e negação por padrão fora de /api.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AutorizacaoSecurityTest {

    private static final String SENHA = "uma-frase-senha-2026";
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaAcessoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String operadorLogin;
    private String adminLogin;
    private ContaAcesso operadorConta;

    @BeforeEach
    void setUp() {
        ContaAcesso operador = criarConta("Operador de Teste", false);
        operadorLogin = operador.getLogin();
        operadorConta = operador;
        adminLogin = criarConta("Administrador de Teste", true).getLogin();
    }

    @Test
    void operadorDeveAcessarOperacaoCotidiana() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenOperador()))
                .andExpect(status().isOk());
    }

    @Test
    void administradorDeveAcessarOperacaoCotidiana() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void rotaProtegidaSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenMalformadoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer nao.e.um.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorNaoDeveExcluirCategoria() throws Exception {
        mockMvc.perform(delete("/api/categorias/999999")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenOperador()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void administradorAutorizadoDeveReceber404AoExcluirInexistente()
            throws Exception {

        mockMvc.perform(delete("/api/categorias/999999")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void operadorNaoDeveExcluirItemDeVenda() throws Exception {
        mockMvc.perform(delete("/api/vendas/999999/itens/999999")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenOperador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void operadorNaoDeveAdministrarStatusDeConta() throws Exception {
        mockMvc.perform(put(
                        "/api/contas-acesso/{id}/status",
                        operadorConta.getId())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenOperador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BLOQUEADA"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void rotaForaDaPoliticaDeveSerNegadaPorPadrao() throws Exception {
        mockMvc.perform(get("/nao-classificado")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenOperador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEOpenApiDevemContinuarPublicos() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AL Sports API"));
    }

    @Test
    void operadorDeveCadastrarPessoaMasNaoConta() throws Exception {
        mockMvc.perform(post("/api/pessoas")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenOperador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Pessoa do Operador",
                                  "email": "pessoa.%s@example.com",
                                  "cpf": "%s"
                                }
                                """.formatted(sufixo(), cpfValido(
                                        SEQUENCIA.incrementAndGet()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/contas-acesso")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenOperador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private ContaAcesso criarConta(String nome, boolean administrador) {
        Pessoa pessoa = pessoaRepository.save(criarPessoa(nome));
        String login = (administrador ? "admin." : "operador.")
                + sufixo();
        ContaAcesso conta = new ContaAcesso(
                pessoa, login, passwordEncoder.encode(SENHA));

        if (administrador) {
            conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        }

        return contaAcessoRepository.saveAndFlush(conta);
    }

    private String tokenOperador() throws Exception {
        return autenticar(operadorLogin);
    }

    private String tokenAdmin() throws Exception {
        return autenticar(adminLogin);
    }

    private String autenticar(String login) throws Exception {
        String resposta = mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(login, SENHA)))
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

        return new Pessoa(
                nome, email, cpfValido(sequencia), LocalDate.now());
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
