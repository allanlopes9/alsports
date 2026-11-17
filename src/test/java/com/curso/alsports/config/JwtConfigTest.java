package com.curso.alsports.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtConfigTest {

    private static final String ISSUER = "https://alsports.local";
    private static final String SECRET =
            "YWxzcG9ydHMtYXVsYTEzLXRlc3RlLWNoYXZlLTMyYiEh";
    private static final String OUTRO_ISSUER = "https://outro.local";

    private JwtConfig config;
    private JwtProperties properties;
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        config = new JwtConfig();
        properties = new JwtProperties(
                ISSUER, Duration.ofMinutes(30), SECRET);
        SecretKey key = config.jwtSecretKey(properties);
        encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key, properties);
    }

    @Test
    void deveCriarChaveHmacSha256ComPeloMenos32Bytes() {
        SecretKey key = config.jwtSecretKey(properties);

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void deveRejeitarSecretQueNaoSejaBase64() {
        String invalido = "isto-nao-e-base64!";

        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(
                ISSUER, Duration.ofMinutes(30), invalido)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64")
                .hasMessageNotContaining(invalido);
    }

    @Test
    void deveRejeitarChaveComMenosDe32Bytes() {
        String curta = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(
                ISSUER, Duration.ofMinutes(30), curta)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void deveRejeitarSecretAusente() {
        assertThatThrownBy(() -> new JwtProperties(
                ISSUER, Duration.ofMinutes(30), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");

        assertThatThrownBy(() -> new JwtProperties(
                ISSUER, Duration.ofMinutes(30), "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void deveRejeitarIssuerAusente() {
        assertThatThrownBy(() -> new JwtProperties(
                " ", Duration.ofMinutes(30), SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void deveRejeitarExpiracaoNaoPositiva() {
        assertThatThrownBy(() -> new JwtProperties(
                ISSUER, Duration.ZERO, SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expiration");

        assertThatThrownBy(() -> new JwtProperties(
                ISSUER, Duration.ofSeconds(-1), SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expiration");
    }

    @Test
    void deveAceitarTokenComAssinaturaEmissorEExpiracaoValidos() {
        String token = emitir(ISSUER, Instant.now(), Instant.now().plusSeconds(600));

        assertThat(decoder.decode(token).getSubject()).isEqualTo("ana.souza");
    }

    @Test
    void deveRejeitarTokenComEmissorIncorreto() {
        String token = emitir(
                OUTRO_ISSUER, Instant.now(), Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        String token = emitir(
                ISSUER,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenAdulterado() {
        String token = emitir(
                ISSUER, Instant.now(), Instant.now().plusSeconds(600));
        String[] partes = token.split("\\.");
        String payload = partes[1];
        char ultimo = payload.charAt(payload.length() - 1);
        char substituto = ultimo == 'A' ? 'B' : 'A';
        partes[1] = payload.substring(0, payload.length() - 1) + substituto;
        String adulterado = String.join(".", partes);

        assertThatThrownBy(() -> decoder.decode(adulterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenMalformado() {
        assertThatThrownBy(() -> decoder.decode("nao.e.um.jwt"))
                .isInstanceOf(JwtException.class);

        assertThatThrownBy(() -> decoder.decode("sem-segmentos"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenComAlgoritmoNaoPermitido() {
        String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"iss\":\"" + ISSUER
                + "\",\"sub\":\"ana.souza\",\"exp\":9999999999}");
        String token = header + "." + payload + ".";

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenAssinadoComOutraChave() {
        byte[] outraChave = new byte[32];
        java.util.Arrays.fill(outraChave, (byte) 7);
        SecretKey outra = new SecretKeySpec(outraChave, "HmacSHA256");
        JwtEncoder outroEncoder = NimbusJwtEncoder.withSecretKey(outra)
                .algorithm(MacAlgorithm.HS256)
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("ana.souza")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .id(UUID.randomUUID().toString())
                .claim("perfis", List.of("OPERADOR"))
                .build();

        String token = outroEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    private String emitir(String issuer, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("ana.souza")
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim("perfis", List.of("OPERADOR"))
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    private static String base64Url(String valor) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }
}
