package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.curso.alsports.config.JwtConfig;
import com.curso.alsports.config.JwtProperties;

class TokenServiceTest {

    private static final String ISSUER = "https://alsports.local";
    private static final String SECRET =
            "YWxzcG9ydHMtYXVsYTEzLXRlc3RlLWNoYXZlLTMyYiEh";
    private static final Instant AGORA =
            Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    private static final String SENHA_CONHECIDA = "uma-frase-senha-2026";

    private JwtProperties properties;
    private JwtEncoder encoder;
    private JwtDecoder decoder;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(
                ISSUER, Duration.ofMinutes(30), SECRET);

        JwtConfig config = new JwtConfig();
        SecretKey key = config.jwtSecretKey(properties);
        encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key, properties);

        Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
        tokenService = new TokenService(encoder, properties, clock);
    }

    @Test
    void deveEmitirTokenComTresSegmentosEAlgoritmoHs256() {
        TokenAcesso token = tokenService.emitir(autenticacaoOperador());

        assertThat(token.valor().split("\\.")).hasSize(3);

        Jwt jwt = decoder.decode(token.valor());

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void deveEmitirClaimsObrigatorias() {
        TokenAcesso token = tokenService.emitir(autenticacaoOperador());
        Jwt jwt = decoder.decode(token.valor());

        assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
        assertThat(jwt.getSubject()).isEqualTo("ana.souza");
        assertThat(jwt.getIssuedAt()).isEqualTo(AGORA);
        assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plusSeconds(1800));
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsStringList("perfis"))
                .containsExactly("OPERADOR");
    }

    @Test
    void deveCalcularDuracaoAbsolutaDe1800Segundos() {
        TokenAcesso token = tokenService.emitir(autenticacaoOperador());

        assertThat(token.duracaoSegundos()).isEqualTo(1800);
        assertThat(token.expiraEm()).isEqualTo(AGORA.plusSeconds(1800));

        Jwt jwt = decoder.decode(token.valor());

        assertThat(Duration.between(
                jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds())
                .isEqualTo(1800);
    }

    @Test
    void deveGerarJtiUnicoEntreEmissoes() {
        TokenAcesso primeiro = tokenService.emitir(autenticacaoOperador());
        TokenAcesso segundo = tokenService.emitir(autenticacaoOperador());

        String jtiPrimeiro = decoder.decode(primeiro.valor()).getId();
        String jtiSegundo = decoder.decode(segundo.valor()).getId();

        assertThat(jtiPrimeiro).isNotBlank();
        assertThat(jtiSegundo).isNotBlank();
        assertThat(jtiPrimeiro).isNotEqualTo(jtiSegundo);
    }

    @Test
    void deveDerivarPerfisDasAuthoritiesAutenticadas() {
        Authentication authentication = UsernamePasswordAuthenticationToken
                .authenticated(
                        "ana.souza",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_OPERADOR"),
                                new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));

        TokenAcesso token = tokenService.emitir(authentication);
        Jwt jwt = decoder.decode(token.valor());

        assertThat(jwt.getClaimAsStringList("perfis"))
                .containsExactly("ADMINISTRADOR", "OPERADOR");
    }

    @Test
    void naoDeveIncluirSenhaHashCpfOuSegredoNoPayload() {
        TokenAcesso token = tokenService.emitir(autenticacaoOperador());

        String payload = new String(
                Base64.getUrlDecoder().decode(token.valor().split("\\.")[1]),
                StandardCharsets.UTF_8);

        assertThat(payload)
                .doesNotContain(SENHA_CONHECIDA)
                .doesNotContain("senha")
                .doesNotContain("senhaHash")
                .doesNotContain("$2a$")
                .doesNotContain("cpf")
                .doesNotContain(SECRET);
    }

    @Test
    void naoDeveRenovarExpiracaoAoReutilizarOToken() {
        TokenAcesso token = tokenService.emitir(autenticacaoOperador());

        Jwt primeiraLeitura = decoder.decode(token.valor());
        Jwt segundaLeitura = decoder.decode(token.valor());

        assertThat(segundaLeitura.getExpiresAt())
                .isEqualTo(primeiraLeitura.getExpiresAt())
                .isEqualTo(AGORA.plusSeconds(1800));
    }

    @Test
    void naoDeveEmitirTokenSemAutenticacaoValidada() {
        Authentication naoAutenticada = UsernamePasswordAuthenticationToken
                .unauthenticated("ana.souza", SENHA_CONHECIDA);

        assertThatThrownBy(() -> tokenService.emitir(naoAutenticada))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Autenticação válida");
    }

    private Authentication autenticacaoOperador() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "ana.souza",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_OPERADOR")));
    }
}
