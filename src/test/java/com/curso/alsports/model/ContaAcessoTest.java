package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContaAcessoTest {

    private static final String HASH =
            "{bcrypt}$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
    private static final String OUTRO_HASH =
            "{bcrypt}$2a$10$zyxwvutsrqponmlkjihgfedcba9876543210ZYXWVU";

    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        pessoa = new Pessoa(
                "Ana Souza",
                "ana@example.com",
                "52998224725",
                LocalDate.of(2026, 11, 17));
    }

    @Test
    void deveCriarContaAtivaComSomentePerfilOperador() {
        ContaAcesso conta = new ContaAcesso(pessoa, "ana.souza", HASH);

        assertThat(conta.getPessoa()).isSameAs(pessoa);
        assertThat(conta.getLogin()).isEqualTo("ana.souza");
        assertThat(conta.getSenhaHash()).isEqualTo(HASH);
        assertThat(conta.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
        assertThat(conta.getPerfis())
                .containsExactly(PerfilAcesso.OPERADOR);
        assertThat(conta.podeAutenticar()).isTrue();
    }

    @Test
    void deveApararEspacosExternosDoLogin() {
        ContaAcesso conta = new ContaAcesso(
                pessoa, "  ana.souza  ", HASH);

        assertThat(conta.getLogin()).isEqualTo("ana.souza");
    }

    @Test
    void naoDeveCriarContaSemPessoa() {
        assertThatThrownBy(() -> new ContaAcesso(null, "ana.souza", HASH))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Pessoa é obrigatória");
    }

    @Test
    void naoDeveCriarContaComLoginEmBranco() {
        assertThatThrownBy(() -> new ContaAcesso(pessoa, "   ", HASH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login é obrigatório");
    }

    @Test
    void naoDeveCriarContaComLoginMaiorQueOitentaCaracteres() {
        String login = "a".repeat(81);

        assertThatThrownBy(() -> new ContaAcesso(pessoa, login, HASH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login deve possuir no máximo 80 caracteres");
    }

    @Test
    void naoDeveCriarContaComHashEmBranco() {
        assertThatThrownBy(() -> new ContaAcesso(pessoa, "ana.souza", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hash da senha é obrigatório");
    }

    @Test
    void deveAlterarSenhaSemExporOHash() {
        ContaAcesso conta = new ContaAcesso(pessoa, "ana.souza", HASH);

        conta.alterarSenha(OUTRO_HASH);

        assertThat(conta.getSenhaHash()).isEqualTo(OUTRO_HASH);
        assertThat(conta.toString())
                .doesNotContain(HASH)
                .doesNotContain(OUTRO_HASH)
                .contains("ana.souza");
    }

    @Test
    void naoDeveAutenticarContaBloqueada() {
        ContaAcesso conta = new ContaAcesso(pessoa, "ana.souza", HASH);

        conta.bloquear();

        assertThat(conta.getStatus()).isEqualTo(StatusContaAcesso.BLOQUEADA);
        assertThat(conta.podeAutenticar()).isFalse();

        conta.ativar();

        assertThat(conta.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
        assertThat(conta.podeAutenticar()).isTrue();
    }

    @Test
    void naoDeveAutenticarQuandoAPessoaEstiverInativa() {
        ContaAcesso conta = new ContaAcesso(pessoa, "ana.souza", HASH);

        pessoa.inativar();

        assertThat(conta.podeAutenticar()).isFalse();
    }

    @Test
    void naoDevePermitirAlterarPerfisPelaColecaoExposta() {
        ContaAcesso conta = new ContaAcesso(pessoa, "ana.souza", HASH);

        assertThatThrownBy(() -> conta.getPerfis()
                .add(PerfilAcesso.ADMINISTRADOR))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deveConcederPerfilAdministrativoSemRemoverOperador() {
        ContaAcesso conta = new ContaAcesso(pessoa, "ana.souza", HASH);

        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);

        assertThat(conta.getPerfis()).containsExactlyInAnyOrder(
                PerfilAcesso.OPERADOR,
                PerfilAcesso.ADMINISTRADOR);
    }
}
