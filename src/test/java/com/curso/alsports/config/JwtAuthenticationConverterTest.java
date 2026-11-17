package com.curso.alsports.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter =
            new SecurityConfig().jwtAuthenticationConverter();

    @Test
    void deveConverterClaimPerfisEmAuthoritiesComPrefixoRole() {
        Jwt jwt = jwtComPerfis(List.of("OPERADOR", "ADMINISTRADOR"));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("ana.souza");
        assertThat(authoritiesDePerfil(authentication))
                .containsExactlyInAnyOrder(
                        "ROLE_OPERADOR",
                        "ROLE_ADMINISTRADOR");
    }

    @Test
    void deveConverterUmUnicoPerfilSemDuplicarPrefixo() {
        Jwt jwt = jwtComPerfis(List.of("OPERADOR"));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authoritiesDePerfil(authentication))
                .containsExactly("ROLE_OPERADOR");
    }

    @Test
    void naoDeveCriarAuthoritiesDeNegocioSemClaimPerfis() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ana.souza")
                .claim("scope", "read")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authoritiesDePerfil(authentication))
                .doesNotContain("ROLE_OPERADOR", "ROLE_ADMINISTRADOR")
                .isEmpty();
    }

    @Test
    void naoDeveInterpretarScopeComoPerfilDeNegocio() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ana.souza")
                .claim("scope", "ADMINISTRADOR")
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authoritiesDePerfil(authentication))
                .doesNotContain("ROLE_ADMINISTRADOR");
    }

    /**
     * O Resource Server acrescenta a authority de fator de autenticação
     * ({@code FACTOR_BEARER}). Ela não é um perfil de negócio e não deve
     * contaminar as asserções sobre o claim {@code perfis}.
     */
    private static List<String> authoritiesDePerfil(
            AbstractAuthenticationToken authentication) {

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }

    private static Jwt jwtComPerfis(List<String> perfis) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("ana.souza")
                .claim("perfis", perfis)
                .build();
    }
}
