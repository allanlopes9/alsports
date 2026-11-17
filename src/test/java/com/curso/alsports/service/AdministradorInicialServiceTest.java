package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.curso.alsports.config.AdministradorInicialProperties;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;

@ExtendWith(MockitoExtension.class)
class AdministradorInicialServiceTest {

    private static final String SENHA = "uma-frase-senha-2026";
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ContaAcessoRepository contaAcessoRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    private PasswordEncoder passwordEncoder;
    private AdministradorInicialService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
        service = new AdministradorInicialService(
                contaAcessoRepository,
                pessoaRepository,
                passwordEncoder,
                CLOCK);
    }

    @Test
    void deveCriarAdministradorComOperadorEAdministrador() {
        prepararCriacao();

        Optional<ContaAcesso> criada = service.criarSeNecessario(
                propriedadesValidas());

        assertThat(criada).isPresent();
        ContaAcesso conta = criada.orElseThrow();
        assertThat(conta.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
        assertThat(conta.getPerfis()).containsExactlyInAnyOrder(
                PerfilAcesso.OPERADOR,
                PerfilAcesso.ADMINISTRADOR);
        assertThat(conta.getPessoa().getNome())
                .isEqualTo("Administrador do Curso");
        assertThat(conta.getPessoa().getEmail())
                .isEqualTo("administrador@example.com");
        assertThat(conta.getPessoa().getCpf()).isEqualTo("52998224725");
        assertThat(conta.getPessoa().getDataCadastro()).isEqualTo(HOJE);
        assertThat(conta.getSenhaHash())
                .isNotEqualTo(SENHA)
                .startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(SENHA, conta.getSenhaHash()))
                .isTrue();
        verify(pessoaRepository).save(any(Pessoa.class));
        verify(contaAcessoRepository).save(conta);
    }

    @Test
    void naoDeveDuplicarQuandoJaExisteAdministradorAtivo() {
        when(contaAcessoRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA)).thenReturn(1L);

        Optional<ContaAcesso> criada = service.criarSeNecessario(
                propriedadesValidas());

        assertThat(criada).isEmpty();
        verifyNoInteractions(pessoaRepository);
        verify(contaAcessoRepository, never()).save(any(ContaAcesso.class));
    }

    @Test
    void naoDeveCriarComLoginJaUtilizado() {
        when(contaAcessoRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA)).thenReturn(0L);
        when(contaAcessoRepository.existsByLoginIgnoreCase("administrador"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criarSeNecessario(
                propriedadesValidas()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Login do administrador inicial já está em uso");

        verify(contaAcessoRepository, never()).save(any(ContaAcesso.class));
    }

    @Test
    void naoDeveCriarComCpfJaCadastrado() {
        when(contaAcessoRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA)).thenReturn(0L);
        when(pessoaRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.criarSeNecessario(
                propriedadesValidas()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CPF do administrador inicial já está cadastrado");

        verify(pessoaRepository, never()).save(any(Pessoa.class));
    }

    @Test
    void naoDeveCriarComEmailJaCadastrado() {
        when(contaAcessoRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA)).thenReturn(0L);
        when(pessoaRepository.existsByEmailIgnoreCase(
                "administrador@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criarSeNecessario(
                propriedadesValidas()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "E-mail do administrador inicial já está cadastrado");

        verify(pessoaRepository, never()).save(any(Pessoa.class));
    }

    @ParameterizedTest
    @MethodSource("propriedadesInvalidas")
    void naoDeveCriarComPropriedadesInvalidas(
            AdministradorInicialProperties propriedades,
            String mensagem) {

        when(contaAcessoRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA)).thenReturn(0L);

        assertThatThrownBy(() -> service.criarSeNecessario(propriedades))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(mensagem);

        verifyNoInteractions(pessoaRepository);
        verify(contaAcessoRepository, never()).save(any(ContaAcesso.class));
    }

    @Test
    void naoDeveCriarComConfiguracaoAusente() {
        assertThatThrownBy(() -> service.criarSeNecessario(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Configuração do administrador inicial é obrigatória");

        verifyNoInteractions(pessoaRepository, contaAcessoRepository);
    }

    private void prepararCriacao() {
        when(contaAcessoRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA)).thenReturn(0L);
        when(pessoaRepository.save(any(Pessoa.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(contaAcessoRepository.save(any(ContaAcesso.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private static AdministradorInicialProperties propriedadesValidas() {
        return new AdministradorInicialProperties(
                true,
                "Administrador do Curso",
                "administrador@example.com",
                "52998224725",
                "administrador",
                SENHA);
    }

    private static Stream<Arguments> propriedadesInvalidas() {
        return Stream.of(
                Arguments.of(propriedades(null, "administrador@example.com",
                        "52998224725", "administrador", SENHA),
                        "Nome do administrador inicial é obrigatório"),
                Arguments.of(propriedades("Admin", "  ",
                        "52998224725", "administrador", SENHA),
                        "E-mail do administrador inicial é obrigatório"),
                Arguments.of(propriedades("Admin", "administrador@example.com",
                        null, "administrador", SENHA),
                        "CPF do administrador inicial é obrigatório"),
                Arguments.of(propriedades("Admin", "administrador@example.com",
                        "123", "administrador", SENHA),
                        "CPF do administrador inicial é inválido"),
                Arguments.of(propriedades("Admin", "administrador@example.com",
                        "52998224725", "ab", SENHA),
                        "Login deve possuir entre 3 e 80 caracteres"),
                Arguments.of(propriedades("Admin", "administrador@example.com",
                        "52998224725", "administrador", "curta"),
                        "Senha deve possuir entre 12 e 72 caracteres"));
    }

    private static AdministradorInicialProperties propriedades(
            String nome,
            String email,
            String cpf,
            String login,
            String senha) {

        return new AdministradorInicialProperties(
                true, nome, email, cpf, login, senha);
    }
}
