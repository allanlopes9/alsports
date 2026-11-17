package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.repository.ContaAcessoRepository;

@ExtendWith(MockitoExtension.class)
class ContaAcessoUserDetailsServiceTest {

    private static final String HASH =
            "{bcrypt}$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    @Mock
    private ContaAcessoRepository repository;

    private ContaAcessoUserDetailsService service;
    private Pessoa pessoa;
    private ContaAcesso conta;

    @BeforeEach
    void setUp() {
        service = new ContaAcessoUserDetailsService(repository);
        pessoa = new Pessoa(
                "Ana Souza",
                "ana@example.com",
                "52998224725",
                LocalDate.of(2026, 11, 17));
        conta = new ContaAcesso(pessoa, "ana.souza", HASH);
    }

    @Test
    void deveCarregarContaComHashEPerfilOperador() {
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        UserDetails userDetails = service.loadUserByUsername("ana.souza");

        assertThat(userDetails.getUsername()).isEqualTo("ana.souza");
        assertThat(userDetails.getPassword()).isEqualTo(HASH);
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_OPERADOR");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void deveConsultarLoginSemDiferenciarCaixaEAparandoEspacos() {
        when(repository.findByLoginIgnoreCase("ANA.SOUZA"))
                .thenReturn(Optional.of(conta));

        UserDetails userDetails = service.loadUserByUsername(
                "  ANA.SOUZA  ");

        assertThat(userDetails.getUsername()).isEqualTo("ana.souza");
    }

    @Test
    void deveDesabilitarContaBloqueada() {
        conta.bloquear();
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        UserDetails userDetails = service.loadUserByUsername("ana.souza");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void deveDesabilitarContaDePessoaInativa() {
        pessoa.inativar();
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        UserDetails userDetails = service.loadUserByUsername("ana.souza");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void deveMapearPerfisParaAutoridadesComPrefixoRole() {
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        UserDetails userDetails = service.loadUserByUsername("ana.souza");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder(
                        "ROLE_OPERADOR",
                        "ROLE_ADMINISTRADOR");
    }

    @Test
    void naoDeveRevelarSeOLoginExiste() {
        when(repository.findByLoginIgnoreCase("nao.existe"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.loadUserByUsername("nao.existe"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Login ou senha inválidos");
    }

    @Test
    void naoDeveConsultarRepositorioComLoginEmBranco() {
        assertThatThrownBy(() -> service.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Login ou senha inválidos");

        verifyNoInteractions(repository);
    }
}
