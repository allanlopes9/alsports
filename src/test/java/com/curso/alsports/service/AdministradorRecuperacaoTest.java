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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.curso.alsports.config.AdministradorRecuperacaoProperties;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;

@ExtendWith(MockitoExtension.class)
class AdministradorRecuperacaoTest {

    private static final Instant AGORA =
            Instant.parse("2026-11-17T12:00:00Z");
    private static final String ORIGEM = "RECUPERACAO_ADMINISTRATIVA";
    private static final String SENHA_HASH = "{bcrypt}hash-preservado";

    @Mock
    private ContaAcessoRepository contaRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private RegistradorEventosSeguranca auditoria;

    private AdministradorRecuperacaoService service;

    @BeforeEach
    void setUp() {
        service = new AdministradorRecuperacaoService(
                contaRepository,
                pessoaRepository,
                auditoria,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveReativarPessoaSemAlterarContaSenhaOuPerfis() {
        Pessoa pessoa = pessoaInativa();
        ContaAcesso conta = contaAdministrativa(pessoa, true);
        prepararLeituras(conta, pessoa);

        boolean reativada = service.recuperar(
                propriedades(true, "admin.teste", "admin.teste"));

        assertThat(reativada).isTrue();
        assertThat(pessoa.isAtiva()).isTrue();
        assertThat(conta.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
        assertThat(conta.getSenhaHash()).isEqualTo(SENHA_HASH);
        assertThat(conta.getPerfis()).containsExactlyInAnyOrder(
                PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR);

        verify(auditoria).registrar(
                TipoEventoSeguranca.STATUS_ALTERADO,
                AGORA,
                null,
                10L,
                null,
                ORIGEM);
        verify(contaRepository, never()).save(any(ContaAcesso.class));
        verify(pessoaRepository, never()).save(any(Pessoa.class));
    }

    @Test
    void deveSerIdempotenteQuandoPessoaJaEstaAtiva() {
        Pessoa pessoa = pessoaInativa();
        pessoa.ativar();
        ContaAcesso conta = contaAdministrativa(pessoa, true);
        prepararLeituras(conta, pessoa);

        boolean reativada = service.recuperar(
                propriedades(true, "admin.teste", "admin.teste"));

        assertThat(reativada).isFalse();
        assertThat(pessoa.isAtiva()).isTrue();
        verifyNoInteractions(auditoria);
    }

    @Test
    void deveRejeitarConfirmacaoDivergente() {
        assertThatThrownBy(() -> service.recuperar(
                propriedades(true, "admin.teste", "outro.login")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login e confirmação de recuperação divergem");

        verifyNoInteractions(
                contaRepository, pessoaRepository, auditoria);
    }

    @Test
    void deveExigirLoginEConfirmacao() {
        assertThatThrownBy(() -> service.recuperar(
                propriedades(true, " ", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login de recuperação é obrigatório");

        assertThatThrownBy(() -> service.recuperar(
                propriedades(true, "admin.teste", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Confirmação de recuperação é obrigatória");

        verifyNoInteractions(
                contaRepository, pessoaRepository, auditoria);
    }

    @Test
    void deveRejeitarConfiguracaoDesabilitada() {
        assertThatThrownBy(() -> service.recuperar(
                propriedades(false, "admin.teste", "admin.teste")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recuperação administrativa está desabilitada");

        verifyNoInteractions(
                contaRepository, pessoaRepository, auditoria);
    }

    @Test
    void deveFalharQuandoContaNaoExiste() {
        when(contaRepository.buscarIdPorLogin("admin.teste"))
                .thenReturn(null);

        assertThatThrownBy(() -> service.recuperar(
                propriedades(true, "admin.teste", "admin.teste")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Conta administrativa não encontrada para recuperação");

        verifyNoInteractions(auditoria);
    }

    @Test
    void deveFalharQuandoContaNaoEAdministrativa() {
        Pessoa pessoa = pessoaInativa();
        ContaAcesso conta = contaAdministrativa(pessoa, false);
        prepararLeituras(conta, pessoa);

        assertThatThrownBy(() -> service.recuperar(
                propriedades(true, "admin.teste", "admin.teste")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A recuperação exige uma conta administrativa ativa");

        assertThat(pessoa.isAtiva()).isFalse();
        verifyNoInteractions(auditoria);
    }

    @Test
    void deveFalharQuandoContaAdministrativaEstaBloqueada() {
        Pessoa pessoa = pessoaInativa();
        ContaAcesso conta = contaAdministrativa(pessoa, true);
        conta.bloquear();
        prepararLeituras(conta, pessoa);

        assertThatThrownBy(() -> service.recuperar(
                propriedades(true, "admin.teste", "admin.teste")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A recuperação exige uma conta administrativa ativa");

        assertThat(pessoa.isAtiva()).isFalse();
        verifyNoInteractions(auditoria);
    }

    private void prepararLeituras(ContaAcesso conta, Pessoa pessoa) {
        when(contaRepository.buscarIdPorLogin("admin.teste"))
                .thenReturn(10L);
        when(contaRepository.buscarPorIdParaAtualizacao(10L))
                .thenReturn(Optional.of(conta));
        when(pessoaRepository.buscarPorIdParaAtualizacao(20L))
                .thenReturn(Optional.of(pessoa));
        when(contaRepository.findById(10L))
                .thenReturn(Optional.of(conta));
        when(pessoaRepository.findById(20L))
                .thenReturn(Optional.of(pessoa));
    }

    private static Pessoa pessoaInativa() {
        Pessoa pessoa = new Pessoa(
                "Administrador Recuperado",
                "recuperacao@example.com",
                "52998224725",
                LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(pessoa, "id", 20L);
        pessoa.inativar();
        return pessoa;
    }

    private static ContaAcesso contaAdministrativa(
            Pessoa pessoa, boolean administrador) {

        ContaAcesso conta = new ContaAcesso(
                pessoa, "admin.teste", SENHA_HASH);
        ReflectionTestUtils.setField(conta, "id", 10L);
        if (administrador) {
            conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        }
        return conta;
    }

    private static AdministradorRecuperacaoProperties propriedades(
            boolean enabled, String login, String confirm) {

        return new AdministradorRecuperacaoProperties(
                enabled, login, confirm);
    }
}
