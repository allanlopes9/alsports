package com.curso.alsports.config;

import java.time.Clock;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Infraestrutura criptográfica do JWT.
 *
 * <p>HS256 é fixado explicitamente no encoder e no decoder. O decoder valida
 * assinatura, emissor e claims temporais padrão ({@code exp}, {@code nbf}).
 * Nenhum algoritmo é escolhido livremente pelo cabeçalho do token.</p>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final int TAMANHO_MINIMO_CHAVE_BYTES = 32;
    private static final String ALGORITMO_HMAC = "HmacSHA256";

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] chave;

        try {
            chave = Base64.getDecoder().decode(properties.secret().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET deve estar codificada em Base64", ex);
        }

        if (chave.length < TAMANHO_MINIMO_CHAVE_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET deve possuir pelo menos 32 bytes após a "
                            + "decodificação Base64");
        }

        return new SecretKeySpec(chave, ALGORITMO_HMAC);
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            JwtProperties properties) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(properties.issuer()));

        return decoder;
    }
}
