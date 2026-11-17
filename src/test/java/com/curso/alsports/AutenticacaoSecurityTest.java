package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.JwtProperties;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AutenticacaoSecurityTest {

    private static final String SENHA = "uma-frase-senha-2026";
    private static final String NOVA_SENHA = "outra-frase-senha-2026";
    private static final String MENSAGEM_GENERICA = "Login ou senha inválidos";
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaAcessoRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtProperties jwtProperties;

    private Pessoa pessoa;
    private ContaAcesso conta;
    private String login;

    @BeforeEach
    void setUp() {
        pessoa = pessoaRepository.save(criarPessoa("Operadora JWT"));
        login = "jwt." + sufixo();
        conta = contaAcessoRepository.saveAndFlush(new ContaAcesso(
                pessoa, login, passwordEncoder.encode(SENHA)));
    }

    // ------------------------------------------------------------------
    // Emissão e contrato
    // ------------------------------------------------------------------

    @Test
    void loginComCredenciaisValidasDeveEmitirContratoCompleto()
            throws Exception {

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, SENHA)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(content().string(not(containsString(SENHA))))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andExpect(content().string(not(containsString("senhaHash"))))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void tokenEmitidoDeveConterClaimsObrigatorias() throws Exception {
        String token = autenticar(login, SENHA);
        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getIssuer().toString())
                .isEqualTo(jwtProperties.issuer());
        assertThat(jwt.getSubject()).isEqualTo(login);
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt())
                .isEqualTo(jwt.getIssuedAt().plusSeconds(1800));
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsStringList("perfis"))
                .containsExactly("OPERADOR");
    }

    @Test
    void tokenNaoDeveExporDadosSensiveis() throws Exception {
        String token = autenticar(login, SENHA);
        String payload = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8);

        assertThat(payload)
                .doesNotContain(SENHA)
                .doesNotContain("senha")
                .doesNotContain("senhaHash")
                .doesNotContain("$2a$")
                .doesNotContain("cpf")
                .doesNotContain(jwtProperties.secret());
    }

    @Test
    void loginDeveSerAvaliadoSemDiferenciarCaixa() throws Exception {
        String token = autenticar(login.toUpperCase(), SENHA);

        assertThat(jwtDecoder.decode(token).getSubject()).isEqualTo(login);
    }

    @Test
    void tokenEmitidoNaoDeveSerRenovadoPelaReutilizacao() throws Exception {
        String token = autenticar(login, SENHA);
        Jwt primeira = jwtDecoder.decode(token);
        Jwt segunda = jwtDecoder.decode(token);

        assertThat(segunda.getExpiresAt())
                .isEqualTo(primeira.getExpiresAt())
                .isEqualTo(primeira.getIssuedAt().plusSeconds(1800));
    }

    // ------------------------------------------------------------------
    // Falhas de credencial
    // ------------------------------------------------------------------

    @Test
    void senhaIncorretaDeveRetornar401Generico() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, "senha-incorreta-2026")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(MENSAGEM_GENERICA))
                .andExpect(jsonPath("$.path")
                        .value("/api/autenticacao/login"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(content().string(not(containsString(SENHA))))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void loginInexistenteDeveRetornarMesmaMensagemGenerica() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("nao.existe." + sufixo(), SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MENSAGEM_GENERICA));
    }

    @Test
    void contaBloqueadaDeveRetornar401Generico() throws Exception {
        conta.bloquear();
        contaAcessoRepository.saveAndFlush(conta);

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MENSAGEM_GENERICA));
    }

    @Test
    void pessoaInativaDeveRetornar401Generico() throws Exception {
        pessoa.inativar();
        pessoaRepository.saveAndFlush(pessoa);

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MENSAGEM_GENERICA));
    }

    @Test
    void loginEmBrancoDeveRetornar400Estruturado() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "", "senha": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.login").exists())
                .andExpect(jsonPath("$.fields.senha").exists());
    }

    @Test
    void loginAcimaDoLimiteDeveRetornar400() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("a".repeat(81), SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.login").exists());
    }

    @Test
    void jsonInvalidoDeveRetornar400() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ------------------------------------------------------------------
    // Resource Server
    // ------------------------------------------------------------------

    @Test
    void rotaProtegidaSemTokenDeveRetornar401Bearer() throws Exception {
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        containsString("Bearer")))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void httpBasicNaoDeveMaisAutenticar() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .with(httpBasic(login, SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        containsString("Bearer")));
    }

    @Test
    void tokenMalformadoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerVazioDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSemPrefixoBearerDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                autenticar(login, SENHA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAdulteradoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + adulterar(autenticar(login, SENHA))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDeveRetornar401() throws Exception {
        String token = emitir(
                jwtProperties.issuer(),
                Instant.now().minusSeconds(1920),
                Instant.now().minusSeconds(120));

        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenComIssuerIncorretoDeveRetornar401() throws Exception {
        String token = emitir(
                "https://outro.local",
                Instant.now(),
                Instant.now().plusSeconds(1800));

        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAssinadoComOutraChaveDeveRetornar401() throws Exception {
        byte[] outraChave = new byte[32];
        java.util.Arrays.fill(outraChave, (byte) 11);
        SecretKey key = new SecretKeySpec(outraChave, "HmacSHA256");
        JwtEncoder outroEncoder = NimbusJwtEncoder.withSecretKey(key)
                .algorithm(MacAlgorithm.HS256)
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(login)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .id(UUID.randomUUID().toString())
                .claim("perfis", List.of("OPERADOR"))
                .build();

        String token = outroEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenComAlgoritmoNoneDeveRetornar401() throws Exception {
        String header = base64Url(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"iss\":\"" + jwtProperties.issuer()
                + "\",\"sub\":\"" + login
                + "\",\"exp\":9999999999}");
        String token = header + "." + payload + ".";

        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Uso do token e limites de revogação
    // ------------------------------------------------------------------

    @Test
    void tokenValidoDevePermitirConsultarMinhaConta() throws Exception {
        String token = autenticar(login, SENHA);

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.pessoaId").value(pessoa.getId()))
                .andExpect(jsonPath("$.nomePessoa").value("Operadora JWT"))
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.perfis[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(content().string(not(containsString(SENHA))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void tokenValidoDevePermitirRotaDeProdutos() throws Exception {
        mockMvc.perform(get("/api/produtos")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + autenticar(login, SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void healthEOpenApiDevemContinuarPublicos() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AL Sports API"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.bearerAuth.scheme")
                        .value("bearer"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.basicAuth")
                        .doesNotExist());
    }

    @Test
    void tokenContinuaValidoAposBloqueioDaConta() throws Exception {
        String token = autenticar(login, SENHA);

        conta.bloquear();
        contaAcessoRepository.saveAndFlush(conta);

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, SENHA)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.status").value("BLOQUEADA"));
    }

    @Test
    void tokenContinuaValidoAposTrocaDeSenha() throws Exception {
        String token = autenticar(login, SENHA);

        mockMvc.perform(put("/api/contas-acesso/me/senha")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "%s",
                                  "novaSenha": "%s"
                                }
                                """.formatted(SENHA, NOVA_SENHA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/contas-acesso/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login));

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, SENHA)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, NOVA_SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private String autenticar(String login, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, senha)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(resposta, "$.accessToken");
    }

    private String emitir(String issuer, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(login)
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim("perfis", List.of("OPERADOR"))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    private String adulterar(String token) {
        String[] partes = token.split("\\.");
        String assinatura = partes[2];
        // O último caractere Base64URL carrega apenas bits de padding; trocá-lo
        // pode decodificar para a mesma assinatura. O primeiro caractere sempre
        // possui bits significativos, garantindo adulteração real.
        char primeiro = assinatura.charAt(0);
        char substituto = primeiro == 'A' ? 'B' : 'A';
        partes[2] = substituto + assinatura.substring(1);
        return String.join(".", partes);
    }

    private String credenciais(String login, String senha) {
        return """
                {
                  "login": "%s",
                  "senha": "%s"
                }
                """.formatted(login, senha);
    }

    private Pessoa criarPessoa(String nome) {
        long sequencia = SEQUENCIA.incrementAndGet();
        String email = "jwt." + sequencia + "." + System.nanoTime()
                + "@example.com";

        return new Pessoa(
                nome, email, cpfValido(sequencia), LocalDate.now());
    }

    private String sufixo() {
        return Long.toString(System.nanoTime());
    }

    private static String base64Url(String valor) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(valor.getBytes(StandardCharsets.UTF_8));
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
