package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Cliente;
import com.curso.alsports.model.Colaborador;
import com.curso.alsports.model.FuncaoColaborador;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Produto;
import com.curso.alsports.model.StatusVenda;
import com.curso.alsports.model.Venda;
import com.curso.alsports.repository.ClienteRepository;
import com.curso.alsports.repository.ColaboradorRepository;
import com.curso.alsports.repository.ProdutoRepository;
import com.curso.alsports.repository.VendaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @Mock
    private VendaRepository repository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ColaboradorRepository colaboradorRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private EntityManager entityManager;

    private VendaService service;

    @BeforeEach
    void setUp() {
        service = new VendaService(
                repository,
                clienteRepository,
                colaboradorRepository,
                produtoRepository,
                entityManager);
    }

    @Test
    void deveBloquearProdutosEmOrdemCrescenteDeIdentificador() {
        Produto produtoVinte = novoProduto(20L, "5.000");
        Produto produtoDez = novoProduto(10L, "5.000");

        Venda venda = novaVenda();
        venda.adicionarItem(produtoVinte, new BigDecimal("1.000"));
        venda.adicionarItem(produtoDez, new BigDecimal("1.000"));

        when(repository.findOneById(1L)).thenReturn(Optional.of(venda));
        when(repository.saveAndFlush(venda)).thenReturn(venda);

        service.finalizar(1L);

        InOrder ordem = inOrder(entityManager);
        ordem.verify(entityManager).refresh(
                produtoDez, LockModeType.PESSIMISTIC_WRITE);
        ordem.verify(entityManager).refresh(
                produtoVinte, LockModeType.PESSIMISTIC_WRITE);
        assertThat(venda.getStatus()).isEqualTo(StatusVenda.FINALIZADA);
    }

    @Test
    void deveAdquirirBloqueioAntesDeBaixarEstoque() {
        Produto produto = spy(novoProduto(10L, "5.000"));

        Venda venda = novaVenda();
        venda.adicionarItem(produto, new BigDecimal("2.000"));

        when(repository.findOneById(1L)).thenReturn(Optional.of(venda));
        when(repository.saveAndFlush(venda)).thenReturn(venda);

        service.finalizar(1L);

        InOrder ordem = inOrder(entityManager, produto);
        ordem.verify(entityManager).refresh(
                produto, LockModeType.PESSIMISTIC_WRITE);
        ordem.verify(produto).retirarEstoque(new BigDecimal("2.000"));
        assertThat(produto.getQuantidade()).isEqualByComparingTo("3.000");
    }

    @Test
    void deveManterVendaAbertaQuandoSaldoForInsuficiente() {
        Produto produto = novoProduto(10L, "1.000");

        Venda venda = novaVenda();
        venda.adicionarItem(produto, new BigDecimal("2.000"));

        when(repository.findOneById(1L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> service.finalizar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Quantidade para retirada maior que o estoque disponível");

        verify(entityManager).refresh(
                produto, LockModeType.PESSIMISTIC_WRITE);
        assertThat(venda.getStatus()).isEqualTo(StatusVenda.ABERTA);
        assertThat(produto.getQuantidade()).isEqualByComparingTo("1.000");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarProdutoInativoDepoisDeAdquirirBloqueio() {
        Produto produto = novoProduto(10L, "5.000");
        produto.setAtivo(false);

        Venda venda = novaVenda();
        venda.adicionarItem(produto, new BigDecimal("1.000"));

        when(repository.findOneById(1L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> service.finalizar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Todos os produtos da venda devem estar ativos");

        verify(entityManager).refresh(
                produto, LockModeType.PESSIMISTIC_WRITE);
        assertThat(venda.getStatus()).isEqualTo(StatusVenda.ABERTA);
        assertThat(produto.getQuantidade()).isEqualByComparingTo("5.000");
    }

    @Test
    void deveInterromperProcessamentoQuandoSegundoItemNaoPossuirSaldo() {
        Produto primeiro = novoProduto(10L, "5.000");
        Produto segundo = novoProduto(20L, "1.000");

        Venda venda = novaVenda();
        venda.adicionarItem(primeiro, new BigDecimal("2.000"));
        venda.adicionarItem(segundo, new BigDecimal("2.000"));

        when(repository.findOneById(1L)).thenReturn(Optional.of(venda));

        assertThatThrownBy(() -> service.finalizar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Quantidade para retirada maior que o estoque disponível");

        InOrder ordem = inOrder(entityManager);
        ordem.verify(entityManager).refresh(
                primeiro, LockModeType.PESSIMISTIC_WRITE);
        ordem.verify(entityManager).refresh(
                segundo, LockModeType.PESSIMISTIC_WRITE);
        assertThat(venda.getStatus()).isEqualTo(StatusVenda.ABERTA);
        verify(repository, never()).saveAndFlush(any());
    }

    private Venda novaVenda() {
        Pessoa pessoaCliente = new Pessoa(
                "Ana Souza",
                "ana.souza@example.com",
                "52998224725",
                LocalDate.now());
        Cliente cliente = new Cliente(pessoaCliente, null);

        Pessoa pessoaVendedor = new Pessoa(
                "Bruno Lima",
                "bruno.lima@example.com",
                "11144477735",
                LocalDate.now());
        Colaborador vendedor = new Colaborador(
                pessoaVendedor,
                "VEN-TESTE",
                FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29));

        return new Venda(cliente, vendedor, LocalDateTime.now());
    }

    private Produto novoProduto(Long id, String saldo) {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria teste");
        categoria.setAtivo(true);

        Produto produto = new Produto();
        produto.setId(id);
        produto.setCodigoBarras("TESTE-" + id);
        produto.setNome("Produto " + id);
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
