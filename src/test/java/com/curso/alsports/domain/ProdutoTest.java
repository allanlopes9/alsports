package com.curso.alsports.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ProdutoTest {

    @Test
    void deveCriarProdutoAtivoComDadosValidos() {
        // Arrange
        CategoriaProduto categoria = new CategoriaProduto("Bolas");

        // Act
        Produto produto = new Produto(
                "Bola de Futebol",
                10,
                new BigDecimal("49.90"),
                LocalDate.of(2026, 9, 7));

        categoria.adicionarProduto(produto);

        // Assert
        assertEquals("Bola de Futebol", produto.getNome());
        assertEquals(10, produto.getQuantidade());
        assertEquals(new BigDecimal("49.90"), produto.getPreco());
        assertEquals(LocalDate.of(2026, 9, 7), produto.getDataCadastro());
        assertEquals(Status.ATIVO, produto.getStatus());
        assertEquals(categoria, produto.getCategoria());
    }

    @Test
    void deveCalcularValorDoEstoque() {
        // Arrange
        Produto produto = new Produto(
                "Tênis Esportivo",
                3,
                new BigDecimal("199.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        BigDecimal valor = produto.calcularValorEstoque();

        // Assert
        assertEquals(new BigDecimal("599.70"), valor);
    }

    @Test
    void deveReceberERetirarEstoque() {
        // Arrange
        Produto produto = new Produto(
                "Camisa Esportiva",
                10,
                new BigDecimal("89.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        produto.receberEstoque(5);
        produto.retirarEstoque(3);

        // Assert
        assertEquals(12, produto.getQuantidade());
    }

    @Test
    void naoDeveRetirarQuantidadeMaiorQueOSaldo() {
        // Arrange
        Produto produto = new Produto(
                "Short Esportivo",
                5,
                new BigDecimal("59.90"),
                LocalDate.of(2026, 9, 7));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> produto.retirarEstoque(6));
    }

    @Test
    void naoDeveCriarProdutoComNomeEmBranco() {
        // Arrange & Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new Produto(
                        "   ",
                        10,
                        new BigDecimal("49.90"),
                        LocalDate.of(2026, 9, 7)));
    }

    @Test
    void naoDeveCriarProdutoComQuantidadeNegativa() {
        // Arrange & Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new Produto(
                        "Bola de Vôlei",
                        -1,
                        new BigDecimal("79.90"),
                        LocalDate.of(2026, 9, 7)));
    }

    @Test
    void deveAlterarOStatusPorComportamentoExplicito() {
        // Arrange
        Produto produto = new Produto(
                "Meia Esportiva",
                20,
                new BigDecimal("19.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        produto.inativar();

        // Assert
        assertEquals(Status.INATIVO, produto.getStatus());

        // Act
        produto.ativar();

        // Assert
        assertEquals(Status.ATIVO, produto.getStatus());
    }
}
