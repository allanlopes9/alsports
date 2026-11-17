package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FornecedorTest {

    @Test
    void deveCriarFornecedorAtivoComCnpjValido() {
        Fornecedor fornecedor = new Fornecedor(
                "Fornecedor Exemplo Ltda", "11222333000181");

        assertThat(fornecedor.getRazaoSocial())
                .isEqualTo("Fornecedor Exemplo Ltda");
        assertThat(fornecedor.getCnpj()).isEqualTo("11222333000181");
        assertThat(fornecedor.getStatus()).isEqualTo(Status.ATIVO);
    }

    @Test
    void naoDeveCriarFornecedorComCnpjComDigitoVerificadorIncorreto() {
        assertThatThrownBy(() -> new Fornecedor(
                "Fornecedor Exemplo Ltda", "11222333000182"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }

    @Test
    void naoDeveCriarFornecedorComCnpjAusenteOuComFormatoInvalido() {
        assertThatThrownBy(() -> new Fornecedor(
                "Fornecedor Exemplo Ltda", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");

        assertThatThrownBy(() -> new Fornecedor(
                "Fornecedor Exemplo Ltda", "11.222.333/0001-81"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }

    @Test
    void naoDeveAceitarCnpjInvalidoNoSetter() {
        Fornecedor fornecedor = new Fornecedor(
                "Fornecedor Exemplo Ltda", "11222333000181");

        assertThatThrownBy(() -> fornecedor.setCnpj("00000000000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");

        assertThat(fornecedor.getCnpj()).isEqualTo("11222333000181");
    }

    @Test
    void naoDeveAceitarCnpjInvalidoAoAlterarDados() {
        Fornecedor fornecedor = new Fornecedor(
                "Fornecedor Exemplo Ltda", "11222333000181");

        assertThatThrownBy(() -> fornecedor.alterarDados(
                "Fornecedor Renomeado Ltda", "1122233300018"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");

        assertThat(fornecedor.getRazaoSocial())
                .isEqualTo("Fornecedor Exemplo Ltda");
        assertThat(fornecedor.getCnpj()).isEqualTo("11222333000181");
    }

    @Test
    void deveAlterarDadosComCnpjValido() {
        Fornecedor fornecedor = new Fornecedor(
                "Fornecedor Exemplo Ltda", "11222333000181");

        fornecedor.alterarDados(
                "Fornecedor Renomeado Ltda", "11444777000161");

        assertThat(fornecedor.getRazaoSocial())
                .isEqualTo("Fornecedor Renomeado Ltda");
        assertThat(fornecedor.getCnpj()).isEqualTo("11444777000161");
    }
}
