package com.curso.alsports.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.curso.alsports.config.JwtProperties;

/**
 * Emite access tokens JWT assinados com HS256.
 *
 * <p>As claims de perfil são derivadas exclusivamente do objeto
 * {@link Authentication} validado pelo servidor. Nenhum dado enviado pelo
 * cliente participa da emissão além das credenciais já autenticadas.</p>
 *
 * <p>A validade é absoluta: {@code exp = iat + app.security.jwt.expiration}.
 * O uso do token não renova o prazo.</p>
 */
@Service
public class TokenService {

    private static final String PREFIXO_PERFIL = "ROLE_";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(
            JwtEncoder encoder,
            JwtProperties properties,
            Clock clock) {

        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Emite um token sem identificador de sessão. Mantido para compatibilidade
     * dos fluxos que não possuem refresh token; o fluxo da Aula 16 utiliza a
     * sobrecarga que informa {@code sid}.
     */
    public TokenAcesso emitir(Authentication authentication) {
        return emitir(authentication, null);
    }

    /**
     * Emite o access token associado a uma família de sessão.
     *
     * @param authentication autenticação já validada pelo servidor
     * @param sessaoId       identificador da família, gravado na claim {@code sid}
     */
    public TokenAcesso emitir(Authentication authentication, String sessaoId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException(
                    "Autenticação válida é obrigatória para emitir o token");
        }

        Instant emitidoEm = clock.instant();
        Instant expiraEm = emitidoEm.plus(properties.expiration());

        List<String> perfis = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridade -> autoridade != null
                        && autoridade.startsWith(PREFIXO_PERFIL))
                .map(autoridade -> autoridade.substring(PREFIXO_PERFIL.length()))
                .filter(perfil -> !perfil.isBlank())
                .distinct()
                .sorted()
                .toList();

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(authentication.getName())
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim("perfis", perfis);

        if (sessaoId != null && !sessaoId.isBlank()) {
            builder.claim("sid", sessaoId);
        }

        JwtClaimsSet claims = builder.build();

        String valor = encoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        long duracaoSegundos = Duration.between(emitidoEm, expiraEm).toSeconds();

        return new TokenAcesso(
                Objects.requireNonNull(valor, "Token emitido é obrigatório"),
                expiraEm,
                duracaoSegundos);
    }
}
