package com.curso.alsports.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CategoriaProdutoTest {

    @Test
    void deveAdicionarProdutoEManejarOsDoisLadosDaAssociacao() {
        // Arrange
        CategoriaProduto categoria = new CategoriaProduto("Bolas");
        Produto produto = new Produto(
                "Bola de Futebol",
                10,
                new BigDecimal("49.90"),
                LocalDate.of(2026, 9, 7));

        // Act
        categoria.adicionarProduto(produto);

        // Assert
        assertEquals(1, categoria.getProdutos().size());
        assertEquals(produto, categoria.getProdutos().get(0));
        assertEquals(categoria, produto.getCategoria());
    }

    @Test
    void naoDeveAdicionarProdutoNulo() {
        // Arrange
        CategoriaProduto categoria = new CategoriaProduto("Calçados");

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> categoria.adicionarProduto(null));
    }

    @Test
    void naoDeveAdicionarDoisProdutosComOMesmoNome() {
        // Arrange
        CategoriaProduto categoria = new CategoriaProduto("Bolas");
        Produto produto1 = new Produto(
                "Bola de Futebol",
                10,
                new BigDecimal("99.90"),
                LocalDate.of(2026, 9, 7));
        Produto produto2 = new Produto(
                "Bola de Futebol",
                5,
                new BigDecimal("89.90"),
                LocalDate.of(2026, 9, 7));

        categoria.adicionarProduto(produto1);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> categoria.adicionarProduto(produto2));
    }

    @Test
    void naoDevePermitirQueProdutoPertençaADuasCategorias() {
        // Arrange
        CategoriaProduto categoria1 = new CategoriaProduto("Bolas");
        CategoriaProduto categoria2 = new CategoriaProduto("Acessórios");

        Produto produto = new Produto(
                "Bola de Vôlei",
                8,
                new BigDecimal("79.90"),
                LocalDate.of(2026, 9, 7));

        categoria1.adicionarProduto(produto);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> categoria2.adicionarProduto(produto));
    }

    @Test
    void naoDeveExporUmaListaInternaModificavel() {
        // Arrange
        CategoriaProduto categoria = new CategoriaProduto("Meias");
        Produto produto = new Produto(
                "Meia Esportiva",
                20,
                new BigDecimal("19.90"),
                LocalDate.of(2026, 9, 7));

        categoria.adicionarProduto(produto);

        // Act & Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> categoria.getProdutos().clear());

        assertEquals(1, categoria.getProdutos().size());
    }
}
