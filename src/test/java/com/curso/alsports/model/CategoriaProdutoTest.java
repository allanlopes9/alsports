package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CategoriaProdutoTest {

    @Test
    void deveAlterarNome() {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Bolas");

        categoria.alterarNome("  Futebol  ");

        assertThat(categoria.getNome()).isEqualTo("Futebol");
    }

    @Test
    void naoDeveAlterarNomeParaValorEmBranco() {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Bolas");

        assertThatThrownBy(() -> categoria.alterarNome("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome da categoria é obrigatório");
    }

    @Test
    void deveAtivarEInativar() {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setAtivo(true);

        categoria.inativar();

        assertThat(categoria.getAtivo()).isFalse();

        categoria.ativar();

        assertThat(categoria.getAtivo()).isTrue();
    }
}
