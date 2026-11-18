package com.curso.alsports.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.curso.alsports.service.ContaAcessoUserDetailsService;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AdministradorInicialProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            ContaAcessoUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Manager explícito reutilizado pelo endpoint de login. Não há um segundo
     * fluxo de autenticação: o mesmo provider, UserDetailsService e BCrypt da
     * Aula 12 continuam responsáveis pela validação.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationProvider authenticationProvider) {

        return new ProviderManager(authenticationProvider);
    }

    /**
     * Converte o claim {@code perfis} do JWT em authorities {@code ROLE_*}.
     *
     * <p>O token continua sendo a única fonte: o conversor não consulta o
     * banco, não aceita perfis do corpo da requisição e não altera o token.
     * O prefixo {@code ROLE_} é convenção do Spring Security e não contamina
     * o enum de domínio nem o JSON emitido.</p>
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("perfis");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter);

        return authenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {

        http
            // A API é stateless e não usa cookie de autenticação; por isso o
            // CSRF é desabilitado. Se cookies forem introduzidos, reavaliar.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // As regras são avaliadas na ordem. As administrativas vêm antes
            // de "/api/**" para não serem absorvidas pela regra geral.
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/api/health",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-ui.html",
                    "/swagger-ui/**")
                .permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/autenticacao/login",
                    "/api/autenticacao/refresh",
                    "/api/autenticacao/logout")
                .permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/contas-acesso")
                .hasRole("ADMINISTRADOR")
                .requestMatchers(
                    "/api/contas-acesso/*/perfis",
                    "/api/contas-acesso/*/status")
                .hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/**")
                .hasRole("ADMINISTRADOR")
                .requestMatchers("/api/**")
                .hasAnyRole("OPERADOR", "ADMINISTRADOR")
                .anyRequest()
                .denyAll())
            // HTTP Basic foi removido. Apenas Bearer Token válido autentica
            // as rotas protegidas; falhas mantêm o comportamento padrão do
            // Resource Server (401).
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                    jwtAuthenticationConverter)))
            .authenticationProvider(authenticationProvider);

        return http.build();
    }
}
