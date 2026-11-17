package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ProdutoTest {

    private Produto novoProduto() {
        Produto produto = new Produto();
        produto.setCodigoBarras("PRODUTO-001");
        produto.setNome("Produto original");
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("99.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        return produto;
    }

    @Test
    void deveAlterarDadosEditaveis() {
        Produto produto = novoProduto();

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Futebol");

        produto.alterarNome("  Produto alterado  ");
        produto.alterarPreco(new BigDecimal("59.90"));
        produto.alterarEstoqueMinimo(new BigDecimal("4.000"));
        produto.alterarCategoria(categoria);
        produto.alterarFornecedor(null);

        assertThat(produto.getNome()).isEqualTo("Produto alterado");
        assertThat(produto.getPreco()).isEqualByComparingTo("59.90");
        assertThat(produto.getEstoqueMinimo()).isEqualByComparingTo("4.000");
        assertThat(produto.getCategoria()).isSameAs(categoria);
        assertThat(produto.getFornecedor()).isNull();
    }

    @Test
    void naoDeveAlterarNomeParaValorEmBranco() {
        Produto produto = novoProduto();

        assertThatThrownBy(() -> produto.alterarNome("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nome do produto é obrigatório");
    }

    @Test
    void naoDeveAlterarPrecoParaValorNegativo() {
        Produto produto = novoProduto();

        assertThatThrownBy(
                () -> produto.alterarPreco(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Preço não pode ser negativo");
    }

    @Test
    void naoDeveAlterarEstoqueMinimoParaValorNegativo() {
        Produto produto = novoProduto();

        assertThatThrownBy(
                () -> produto.alterarEstoqueMinimo(new BigDecimal("-1.000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Estoque mínimo não pode ser negativo");
    }

    @Test
    void naoDeveAlterarCategoriaParaNula() {
        Produto produto = novoProduto();

        assertThatThrownBy(() -> produto.alterarCategoria(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoria de produto é obrigatória");
    }

    @Test
    void deveAtivarEInativar() {
        Produto produto = novoProduto();

        produto.inativar();

        assertThat(produto.getAtivo()).isFalse();

        produto.ativar();

        assertThat(produto.getAtivo()).isTrue();
    }

    @Test
    void deveReceberERetirarEstoque() {
        Produto produto = novoProduto();

        produto.receberEstoque(new BigDecimal("5.000"));

        assertThat(produto.getQuantidade()).isEqualByComparingTo("15.000");

        produto.retirarEstoque(new BigDecimal("3.000"));

        assertThat(produto.getQuantidade()).isEqualByComparingTo("12.000");
    }

    @Test
    void deveReceberERetirarEstoqueFracionado() {
        Produto produto = novoProduto();

        produto.receberEstoque(new BigDecimal("0.500"));

        assertThat(produto.getQuantidade()).isEqualByComparingTo("10.500");

        produto.retirarEstoque(new BigDecimal("1.250"));

        assertThat(produto.getQuantidade()).isEqualByComparingTo("9.250");
    }

    @Test
    void deveRejeitarQuantidadeComMaisDeTresCasasDecimais() {
        Produto produto = novoProduto();

        assertThatThrownBy(
                () -> produto.receberEstoque(new BigDecimal("0.0001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A quantidade deve ter no máximo 3 casas decimais");

        assertThat(produto.getQuantidade()).isEqualByComparingTo("10.000");
    }

    @Test
    void naoDeveRetirarQuantidadeMaiorQueOSaldo() {
        Produto produto = novoProduto();

        assertThatThrownBy(
                () -> produto.retirarEstoque(new BigDecimal("11.000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Quantidade para retirada maior que o estoque disponível");

        assertThat(produto.getQuantidade()).isEqualByComparingTo("10.000");
    }
}
