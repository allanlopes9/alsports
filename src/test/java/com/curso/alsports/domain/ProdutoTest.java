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
                new BigDecimal("10.000"),
                new BigDecimal("49.90"),
                LocalDate.of(2026, 9, 7));

        categoria.adicionarProduto(produto);

        // Assert
        assertEquals("Bola de Futebol", produto.getNome());
        assertEquals(new BigDecimal("10.000"), produto.getQuantidade());
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
                new BigDecimal("3.000"),
                new BigDecimal("199.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        BigDecimal valor = produto.calcularValorEstoque();

        // Assert
        assertEquals(new BigDecimal("599.70"), valor);
    }

    @Test
    void deveCalcularValorDoEstoqueComQuantidadeFracionada() {
        // Arrange
        Produto produto = new Produto(
                "Tênis Esportivo",
                new BigDecimal("1.500"),
                new BigDecimal("199.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        BigDecimal valor = produto.calcularValorEstoque();

        // Assert
        assertEquals(new BigDecimal("299.85"), valor);
    }

    @Test
    void deveReceberERetirarEstoque() {
        // Arrange
        Produto produto = new Produto(
                "Camisa Esportiva",
                new BigDecimal("10.000"),
                new BigDecimal("89.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        produto.receberEstoque(new BigDecimal("5.000"));
        produto.retirarEstoque(new BigDecimal("3.000"));

        // Assert
        assertEquals(new BigDecimal("12.000"), produto.getQuantidade());
    }

    @Test
    void deveReceberERetirarEstoqueFracionado() {
        // Arrange
        Produto produto = new Produto(
                "Camisa Esportiva",
                new BigDecimal("10.000"),
                new BigDecimal("89.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        produto.receberEstoque(new BigDecimal("0.500"));
        produto.retirarEstoque(new BigDecimal("1.250"));

        // Assert
        assertEquals(new BigDecimal("9.250"), produto.getQuantidade());
    }

    @Test
    void naoDeveRetirarQuantidadeMaiorQueOSaldo() {
        // Arrange
        Produto produto = new Produto(
                "Short Esportivo",
                new BigDecimal("5.000"),
                new BigDecimal("59.90"),
                LocalDate.of(2026, 9, 7));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> produto.retirarEstoque(new BigDecimal("6.000")));
    }

    @Test
    void naoDeveCriarProdutoComNomeEmBranco() {
        // Arrange & Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new Produto(
                        "   ",
                        new BigDecimal("10.000"),
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
                        new BigDecimal("-1.000"),
                        new BigDecimal("79.90"),
                        LocalDate.of(2026, 9, 7)));
    }

    @Test
    void naoDeveCriarProdutoComQuantidadeDeMaisDeTresCasas() {
        // Arrange & Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new Produto(
                        "Bola de Vôlei",
                        new BigDecimal("1.0001"),
                        new BigDecimal("79.90"),
                        LocalDate.of(2026, 9, 7)));
    }

    @Test
    void deveAlterarOStatusPorComportamentoExplicito() {
        // Arrange
        Produto produto = new Produto(
                "Meia Esportiva",
                new BigDecimal("20.000"),
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
