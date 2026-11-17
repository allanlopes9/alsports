package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ClienteColaboradorTest {

    private Pessoa novaPessoa(String nome, String cpf) {
        return new Pessoa(
                nome, nome.toLowerCase().replace(" ", ".") + "@example.com",
                cpf, LocalDate.of(2026, 11, 16));
    }

    @Test
    void deveCriarClienteAssociadoAPessoa() {
        Pessoa pessoa = novaPessoa("Ana Souza", "52998224725");

        Cliente cliente = new Cliente(pessoa, "  (11) 99999-0000  ");

        assertThat(cliente.getPessoa()).isSameAs(pessoa);
        assertThat(cliente.getTelefone()).isEqualTo("(11) 99999-0000");
    }

    @Test
    void deveTratarTelefoneOpcionalEmBrancoComoNulo() {
        Pessoa pessoa = novaPessoa("Ana Souza", "52998224725");

        Cliente cliente = new Cliente(pessoa, "   ");

        assertThat(cliente.getTelefone()).isNull();
    }

    @Test
    void naoDeveCriarClienteSemPessoa() {
        assertThatThrownBy(() -> new Cliente(null, "123"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Pessoa é obrigatória");
    }

    @Test
    void deveAlterarTelefoneDoCliente() {
        Pessoa pessoa = novaPessoa("Ana Souza", "52998224725");
        Cliente cliente = new Cliente(pessoa, "1111");

        cliente.alterarDados("2222");

        assertThat(cliente.getTelefone()).isEqualTo("2222");
    }

    @Test
    void deveCriarColaboradorVendedor() {
        Pessoa pessoa = novaPessoa("Bruno Lima", "11144477735");

        Colaborador colaborador = new Colaborador(
                pessoa, "  VEN-001  ", FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29));

        assertThat(colaborador.getPessoa()).isSameAs(pessoa);
        assertThat(colaborador.getMatricula()).isEqualTo("VEN-001");
        assertThat(colaborador.getFuncao()).isEqualTo(FuncaoColaborador.VENDEDOR);
        assertThat(colaborador.getDataAdmissao())
                .isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(colaborador.podeRealizarVenda()).isTrue();
    }

    @Test
    void deveIndicarQuaisFuncoesPodemRealizarVenda() {
        assertThat(FuncaoColaborador.VENDEDOR.podeRealizarVenda()).isTrue();
        assertThat(FuncaoColaborador.GERENTE.podeRealizarVenda()).isTrue();
        assertThat(FuncaoColaborador.ESTOQUISTA.podeRealizarVenda()).isFalse();
        assertThat(
                FuncaoColaborador.ADMINISTRATIVO.podeRealizarVenda()).isFalse();
    }

    @Test
    void naoDeveCriarColaboradorSemMatricula() {
        Pessoa pessoa = novaPessoa("Bruno Lima", "11144477735");

        assertThatThrownBy(() -> new Colaborador(
                pessoa, "  ", FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Matrícula é obrigatória");
    }

    @Test
    void naoDeveCriarColaboradorSemFuncao() {
        Pessoa pessoa = novaPessoa("Bruno Lima", "11144477735");

        assertThatThrownBy(() -> new Colaborador(
                pessoa, "VEN-001", null, LocalDate.of(2026, 9, 29)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Função é obrigatória");
    }

    @Test
    void naoDeveCriarColaboradorSemDataDeAdmissao() {
        Pessoa pessoa = novaPessoa("Bruno Lima", "11144477735");

        assertThatThrownBy(() -> new Colaborador(
                pessoa, "VEN-001", FuncaoColaborador.VENDEDOR, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Data de admissão é obrigatória");
    }

    @Test
    void deveAlterarMatriculaEFuncaoDoColaborador() {
        Pessoa pessoa = novaPessoa("Bruno Lima", "11144477735");
        Colaborador colaborador = new Colaborador(
                pessoa, "VEN-001", FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29));

        colaborador.alterarDados("GER-001", FuncaoColaborador.GERENTE);

        assertThat(colaborador.getMatricula()).isEqualTo("GER-001");
        assertThat(colaborador.getFuncao()).isEqualTo(FuncaoColaborador.GERENTE);
        assertThat(colaborador.podeRealizarVenda()).isTrue();
    }
}
