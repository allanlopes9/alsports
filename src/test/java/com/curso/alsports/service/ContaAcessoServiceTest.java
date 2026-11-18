package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.curso.alsports.dto.ContaAcessoRequest;
import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Status;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;

@ExtendWith(MockitoExtension.class)
class ContaAcessoServiceTest {

    private static final String SENHA = "uma-frase-senha-2026";
    private static final String NOVA_SENHA = "outra-frase-senha-2026";

    @Mock
    private ContaAcessoRepository repository;

    @Mock
    private PessoaRepository pessoaRepository;

    private PasswordEncoder passwordEncoder;
    private ContaAcessoService service;
    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        passwordEncoder = PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
        service = new ContaAcessoService(
                repository, pessoaRepository, passwordEncoder);
        pessoa = new Pessoa(
                "Ana Souza",
                "ana@example.com",
                "52998224725",
                LocalDate.of(2026, 11, 17));
    }

    @Test
    void deveCadastrarContaPersistindoSomenteHashBcrypt() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.save(any(ContaAcesso.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ContaAcesso conta = service.cadastrar(
                new ContaAcessoRequest(1L, "ana.souza", SENHA));

        assertThat(conta.getPessoa()).isSameAs(pessoa);
        assertThat(conta.getLogin()).isEqualTo("ana.souza");
        assertThat(conta.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
        assertThat(conta.getPerfis())
                .containsExactly(PerfilAcesso.OPERADOR);
        assertThat(conta.getSenhaHash())
                .isNotEqualTo(SENHA)
                .startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(SENHA, conta.getSenhaHash()))
                .isTrue();

        verify(repository).save(conta);
    }

    @Test
    void deveNormalizarLoginComEspacosExternosAntesDeValidarUnicidade() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.save(any(ContaAcesso.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ContaAcesso conta = service.cadastrar(
                new ContaAcessoRequest(1L, "  ana.souza  ", SENHA));

        assertThat(conta.getLogin()).isEqualTo("ana.souza");
        verify(repository).existsByLoginIgnoreCase("ana.souza");
    }

    @Test
    void naoDeveCadastrarLoginDuplicado() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.existsByLoginIgnoreCase("ana.souza"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(1L, "ana.souza", SENHA)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Login já cadastrado");

        verify(repository, never()).save(any(ContaAcesso.class));
    }

    @Test
    void naoDeveCadastrarSegundaContaParaMesmaPessoa() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.existsByPessoaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(1L, "outra.pessoa", SENHA)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Pessoa já possui conta de acesso");

        verify(repository, never()).save(any(ContaAcesso.class));
    }

    @Test
    void naoDeveCadastrarContaParaPessoaInexistente() {
        when(pessoaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(99L, "ana.souza", SENHA)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pessoa não encontrada: 99");

        verify(repository, never()).save(any(ContaAcesso.class));
    }

    @Test
    void naoDeveCadastrarContaParaPessoaInativa() {
        pessoa.inativar();
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));

        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(1L, "ana.souza", SENHA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pessoa inativa não pode possuir conta de acesso");

        verify(repository, never()).save(any(ContaAcesso.class));
    }

    @Test
    void naoDeveAceitarLoginComCaracteresInvalidos() {
        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(1L, "ana souza", SENHA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login deve conter apenas");
    }

    @Test
    void naoDeveAceitarLoginMenorQueTresCaracteres() {
        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(1L, "ab", SENHA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login deve possuir entre 3 e 80 caracteres");
    }

    @Test
    void naoDeveAceitarSenhaMenorQueDozeCaracteres() {
        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(1L, "ana.souza", "curta")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha deve possuir entre 12 e 72 caracteres");
    }

    @Test
    void naoDeveAceitarSenhaComMaisDeSetentaEDoisBytesUtf8() {
        String senha = "á".repeat(37);

        assertThat(senha.length()).isBetween(12, 72);

        assertThatThrownBy(() -> service.cadastrar(
                new ContaAcessoRequest(1L, "ana.souza", senha)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha deve possuir no máximo 72 bytes em UTF-8");
    }

    @Test
    void deveAlterarSenhaQuandoSenhaAtualEstiverCorreta() {
        ContaAcesso conta = contaComSenha(SENHA);
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        service.alterarSenha("ana.souza", SENHA, NOVA_SENHA);

        assertThat(passwordEncoder.matches(NOVA_SENHA, conta.getSenhaHash()))
                .isTrue();
        assertThat(passwordEncoder.matches(SENHA, conta.getSenhaHash()))
                .isFalse();
    }

    @Test
    void naoDeveAlterarSenhaQuandoSenhaAtualEstiverIncorreta() {
        ContaAcesso conta = contaComSenha(SENHA);
        String hashOriginal = conta.getSenhaHash();
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service.alterarSenha(
                "ana.souza", "senha-incorreta-2026", NOVA_SENHA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha atual inválida");

        assertThat(conta.getSenhaHash()).isEqualTo(hashOriginal);
    }

    @Test
    void naoDeveAlterarSenhaParaOMesmoValor() {
        ContaAcesso conta = contaComSenha(SENHA);
        String hashOriginal = conta.getSenhaHash();
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service.alterarSenha(
                "ana.souza", SENHA, SENHA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nova senha deve ser diferente da senha atual");

        assertThat(conta.getSenhaHash()).isEqualTo(hashOriginal);
    }

    @Test
    void naoDeveAlterarSenhaParaValorComMaisDeSetentaEDoisBytes() {
        ContaAcesso conta = contaComSenha(SENHA);

        assertThatThrownBy(() -> service.alterarSenha(
                "ana.souza", SENHA, "á".repeat(37)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha deve possuir no máximo 72 bytes em UTF-8");

        assertThat(passwordEncoder.matches(SENHA, conta.getSenhaHash()))
                .isTrue();
    }

    @Test
    void deveBuscarContaPorLoginSemDiferenciarCaixa() {
        ContaAcesso conta = contaComSenha(SENHA);
        when(repository.findByLoginIgnoreCase("ana.souza"))
                .thenReturn(Optional.of(conta));

        ContaAcesso encontrada = service.buscarPorLogin("  ana.souza  ");

        assertThat(encontrada).isSameAs(conta);
    }

    @Test
    void naoDeveEncontrarContaInexistente() {
        when(repository.findByLoginIgnoreCase("nao.existe"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorLogin("nao.existe"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Conta de acesso não encontrada");
    }

    @Test
    void deveSubstituirPerfisDeOutraConta() {
        ContaAcesso conta = new ContaAcesso(
                pessoa, "operador.teste", passwordEncoder.encode(SENHA));
        when(repository.findById(10L)).thenReturn(Optional.of(conta));

        ContaAcesso alterada = service.alterarPerfis(
                10L,
                Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR),
                "admin.teste");

        assertThat(alterada.getPerfis()).containsExactlyInAnyOrder(
                PerfilAcesso.OPERADOR,
                PerfilAcesso.ADMINISTRADOR);
    }

    @Test
    void naoDeveSubstituirPerfisPorListaVazia() {
        assertThatThrownBy(() -> service.alterarPerfis(
                10L, Set.of(), "admin.teste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A conta deve possuir ao menos um perfil");

        verify(repository, never()).findById(any());
    }

    @Test
    void naoDeveRemoverProprioPerfilAdministrador() {
        ContaAcesso admin = contaAdministrativa("admin.teste");
        when(repository.findById(10L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.alterarPerfis(
                10L, Set.of(PerfilAcesso.OPERADOR), "ADMIN.TESTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O administrador não pode remover o próprio perfil "
                        + "ADMINISTRADOR");

        assertThat(admin.getPerfis()).contains(
                PerfilAcesso.ADMINISTRADOR);
    }

    @Test
    void naoDeveRemoverAdministradorDoUltimoAdminAtivo() {
        ContaAcesso outroAdmin = contaAdministrativa("outro.admin");
        when(repository.findById(10L)).thenReturn(Optional.of(outroAdmin));
        when(repository.contarAdministradoresEfetivos(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA,
                Status.ATIVO)).thenReturn(1L);

        assertThatThrownBy(() -> service.alterarPerfis(
                10L, Set.of(PerfilAcesso.OPERADOR), "admin.teste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O sistema deve manter ao menos um administrador ativo");

        assertThat(outroAdmin.getPerfis()).contains(
                PerfilAcesso.ADMINISTRADOR);
    }

    @Test
    void deveRemoverAdministradorQuandoHaOutroAdminAtivo() {
        ContaAcesso outroAdmin = contaAdministrativa("outro.admin");
        when(repository.findById(10L)).thenReturn(Optional.of(outroAdmin));
        when(repository.contarAdministradoresEfetivos(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA,
                Status.ATIVO)).thenReturn(2L);

        ContaAcesso alterada = service.alterarPerfis(
                10L, Set.of(PerfilAcesso.OPERADOR), "admin.teste");

        assertThat(alterada.getPerfis())
                .containsExactly(PerfilAcesso.OPERADOR);
    }

    @Test
    void naoDeveAlterarPerfisDeContaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterarPerfis(
                99L, Set.of(PerfilAcesso.OPERADOR), "admin.teste"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Conta de acesso não encontrada: 99");
    }

    @Test
    void naoDeveBloquearPropriaConta() {
        ContaAcesso admin = contaAdministrativa("admin.teste");
        when(repository.findById(10L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.alterarStatus(
                10L, StatusContaAcesso.BLOQUEADA, "admin.teste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O administrador não pode bloquear a própria conta");

        assertThat(admin.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
    }

    @Test
    void naoDeveBloquearUltimoAdministradorAtivo() {
        ContaAcesso outroAdmin = contaAdministrativa("outro.admin");
        when(repository.findById(10L)).thenReturn(Optional.of(outroAdmin));
        when(repository.contarAdministradoresEfetivos(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA,
                Status.ATIVO)).thenReturn(1L);

        assertThatThrownBy(() -> service.alterarStatus(
                10L, StatusContaAcesso.BLOQUEADA, "admin.teste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O sistema deve manter ao menos um administrador ativo");

        assertThat(outroAdmin.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
    }

    @Test
    void deveBloquearOutraConta() {
        ContaAcesso conta = new ContaAcesso(
                pessoa, "operador.teste", passwordEncoder.encode(SENHA));
        when(repository.findById(10L)).thenReturn(Optional.of(conta));

        ContaAcesso alterada = service.alterarStatus(
                10L, StatusContaAcesso.BLOQUEADA, "admin.teste");

        assertThat(alterada.getStatus())
                .isEqualTo(StatusContaAcesso.BLOQUEADA);
    }

    @Test
    void deveAtivarContaBloqueada() {
        ContaAcesso conta = new ContaAcesso(
                pessoa, "operador.teste", passwordEncoder.encode(SENHA));
        conta.bloquear();
        when(repository.findById(10L)).thenReturn(Optional.of(conta));

        ContaAcesso alterada = service.alterarStatus(
                10L, StatusContaAcesso.ATIVA, "admin.teste");

        assertThat(alterada.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
    }

    @Test
    void naoDeveAlterarStatusSemValor() {
        assertThatThrownBy(() -> service.alterarStatus(
                10L, null, "admin.teste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status é obrigatório");

        verify(repository, never()).findById(any());
    }

    private ContaAcesso contaAdministrativa(String login) {
        ContaAcesso conta = new ContaAcesso(
                pessoa, login, passwordEncoder.encode(SENHA));
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        return conta;
    }

    private ContaAcesso contaComSenha(String senha) {
        return new ContaAcesso(
                pessoa, "ana.souza", passwordEncoder.encode(senha));
    }
}
