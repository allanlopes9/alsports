package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PessoaTest {

    private static final LocalDate DATA_CADASTRO = LocalDate.of(2026, 11, 16);

    @Test
    void deveCriarPessoaAtivaComDadosValidos() {
        Pessoa pessoa = new Pessoa(
                "Ana Souza", "ANA@Example.com", "52998224725", DATA_CADASTRO);

        assertThat(pessoa.getNome()).isEqualTo("Ana Souza");
        assertThat(pessoa.getEmail()).isEqualTo("ana@example.com");
        assertThat(pessoa.getCpf()).isEqualTo("52998224725");
        assertThat(pessoa.getDataCadastro()).isEqualTo(DATA_CADASTRO);
        assertThat(pessoa.getStatus()).isEqualTo(Status.ATIVO);
        assertThat(pessoa.isAtiva()).isTrue();
    }

    @Test
    void deveApararEspacosDoNomeEEmail() {
        Pessoa pessoa = new Pessoa(
                "  Bruno Lima  ", "  bruno@example.com  ",
                "11144477735", DATA_CADASTRO);

        assertThat(pessoa.getNome()).isEqualTo("Bruno Lima");
        assertThat(pessoa.getEmail()).isEqualTo("bruno@example.com");
    }

    @Test
    void naoDeveCriarPessoaComCpfInvalido() {
        assertThatThrownBy(() -> new Pessoa(
                "Ana Souza", "ana@example.com", "52998224724", DATA_CADASTRO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");
    }

    @Test
    void naoDeveCriarPessoaComCpfAusente() {
        assertThatThrownBy(() -> new Pessoa(
                "Ana Souza", "ana@example.com", "   ", DATA_CADASTRO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF é obrigatório");
    }

    @Test
    void naoDeveCriarPessoaComNomeEmBranco() {
        assertThatThrownBy(() -> new Pessoa(
                "  ", "ana@example.com", "52998224725", DATA_CADASTRO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome é obrigatório");
    }

    @Test
    void naoDeveCriarPessoaComEmailInvalido() {
        assertThatThrownBy(() -> new Pessoa(
                "Ana Souza", "ana.example.com", "52998224725", DATA_CADASTRO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("E-mail inválido");
    }

    @Test
    void naoDeveCriarPessoaSemDataDeCadastro() {
        assertThatThrownBy(() -> new Pessoa(
                "Ana Souza", "ana@example.com", "52998224725", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Data de cadastro é obrigatória");
    }

    @Test
    void deveAlterarDadosMantendoDataCadastro() {
        Pessoa pessoa = new Pessoa(
                "Ana Souza", "ana@example.com", "52998224725", DATA_CADASTRO);

        pessoa.alterarDados(
                "Ana Souza Silva", "ana.silva@example.com", "11144477735");

        assertThat(pessoa.getNome()).isEqualTo("Ana Souza Silva");
        assertThat(pessoa.getEmail()).isEqualTo("ana.silva@example.com");
        assertThat(pessoa.getCpf()).isEqualTo("11144477735");
        assertThat(pessoa.getDataCadastro()).isEqualTo(DATA_CADASTRO);
    }

    @Test
    void naoDeveAlterarDadosParaCpfInvalido() {
        Pessoa pessoa = new Pessoa(
                "Ana Souza", "ana@example.com", "52998224725", DATA_CADASTRO);

        assertThatThrownBy(() -> pessoa.alterarDados(
                "Ana Souza", "ana@example.com", "00000000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");

        assertThat(pessoa.getCpf()).isEqualTo("52998224725");
    }

    @Test
    void deveAtivarEInativarPorComportamentoExplicito() {
        Pessoa pessoa = new Pessoa(
                "Ana Souza", "ana@example.com", "52998224725", DATA_CADASTRO);

        pessoa.inativar();

        assertThat(pessoa.getStatus()).isEqualTo(Status.INATIVO);
        assertThat(pessoa.isAtiva()).isFalse();

        pessoa.ativar();

        assertThat(pessoa.getStatus()).isEqualTo(Status.ATIVO);
        assertThat(pessoa.isAtiva()).isTrue();
    }
}
