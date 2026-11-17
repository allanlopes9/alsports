package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Produto;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.FornecedorRepository;
import com.curso.alsports.repository.ProdutoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository repository;

    @Mock
    private CategoriaProdutoRepository categoriaProdutoRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private EntityManager entityManager;

    private ProdutoService service;

    @BeforeEach
    void setUp() {
        service = new ProdutoService(
                repository,
                categoriaProdutoRepository,
                fornecedorRepository,
                entityManager);
    }

    @Test
    void deveAdquirirBloqueioPessimistaAntesDeReceberEstoque() {
        Produto produto = novoProduto("5.000");
        when(repository.findById(10L)).thenReturn(Optional.of(produto));

        Produto atualizado = service.receberEstoque(
                10L, new BigDecimal("2.500"));

        verify(entityManager).refresh(
                produto, LockModeType.PESSIMISTIC_WRITE);
        assertThat(atualizado.getQuantidade())
                .isEqualByComparingTo("7.500");
    }

    @Test
    void deveAdquirirBloqueioPessimistaAntesDeRetirarEstoque() {
        Produto produto = novoProduto("5.000");
        when(repository.findById(10L)).thenReturn(Optional.of(produto));

        Produto atualizado = service.retirarEstoque(
                10L, new BigDecimal("2.500"));

        verify(entityManager).refresh(
                produto, LockModeType.PESSIMISTIC_WRITE);
        assertThat(atualizado.getQuantidade())
                .isEqualByComparingTo("2.500");
    }

    @Test
    void naoDeveBloquearConsultaSimplesDeProduto() {
        Produto produto = novoProduto("5.000");
        when(repository.findById(10L)).thenReturn(Optional.of(produto));

        Produto encontrado = service.buscarPorId(10L);

        assertThat(encontrado).isSameAs(produto);
        verifyNoInteractions(entityManager);
    }

    @Test
    void devePreservarSaldoQuandoRetiradaExcederDisponivel() {
        Produto produto = novoProduto("1.000");
        when(repository.findById(10L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.retirarEstoque(
                10L, new BigDecimal("2.000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Quantidade para retirada maior que o estoque disponível");

        verify(entityManager).refresh(
                produto, LockModeType.PESSIMISTIC_WRITE);
        assertThat(produto.getQuantidade()).isEqualByComparingTo("1.000");
    }

    private Produto novoProduto(String saldo) {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria teste");
        categoria.setAtivo(true);

        Produto produto = new Produto();
        produto.setId(10L);
        produto.setCodigoBarras("TESTE-SERVICO");
        produto.setNome("Produto teste");
        produto.setQuantidade(new BigDecimal(saldo));
        produto.setEstoqueMinimo(new BigDecimal("1.000"));
        produto.setPreco(new BigDecimal("10.00"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        return produto;
    }
}
