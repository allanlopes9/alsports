package com.curso.alsports.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VendaTest {

    private Cliente cliente;
    private Colaborador vendedor;
    private Produto bola;
    private Produto tenis;

    @BeforeEach
    void prepararCenario() {
        Pessoa pessoaCliente = new Pessoa(
                "Ana Souza", "ana@example.com", "52998224725",
                LocalDate.of(2026, 11, 16));

        Pessoa pessoaVendedor = new Pessoa(
                "Bruno Lima", "bruno@example.com", "11144477735",
                LocalDate.of(2026, 11, 16));

        cliente = new Cliente(pessoaCliente, "(11) 99999-0000");

        vendedor = new Colaborador(
                pessoaVendedor, "VEN-001", FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29));

        bola = novoProduto(1L, "Bola de futebol", "89.90", 10);
        tenis = novoProduto(2L, "Tênis esportivo", "199.90", 5);
    }

    private Produto novoProduto(
            Long id, String nome, String preco, int quantidade) {

        Produto produto = new Produto();
        produto.setCodigoBarras("PRODUTO-" + id);
        produto.setNome(nome);
        produto.setPreco(new BigDecimal(preco));
        produto.setQuantidade(BigDecimal.valueOf(quantidade));
        produto.setEstoqueMinimo(new BigDecimal("1.000"));
        produto.setDataCadastro(LocalDate.of(2026, 11, 16));
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        ReflectionTestUtils.setField(produto, "id", id);

        return produto;
    }

    private Venda novaVenda() {
        return new Venda(cliente, vendedor, LocalDateTime.of(2026, 11, 16, 10, 30));
    }

    @Test
    void deveAbrirVendaSemItens() {
        Venda venda = novaVenda();

        assertThat(venda.getCliente()).isSameAs(cliente);
        assertThat(venda.getVendedor()).isSameAs(vendedor);
        assertThat(venda.getStatus()).isEqualTo(StatusVenda.ABERTA);
        assertThat(venda.getItens()).isEmpty();
        assertThat(venda.calcularTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void naoDeveAbrirVendaSemClienteOuVendedor() {
        LocalDateTime data = LocalDateTime.of(2026, 11, 16, 10, 30);

        assertThatThrownBy(() -> new Venda(null, vendedor, data))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Cliente é obrigatório");

        assertThatThrownBy(() -> new Venda(cliente, null, data))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Vendedor é obrigatório");
    }

    @Test
    void deveAdicionarItemCapturandoPrecoAtualDoProduto() {
        Venda venda = novaVenda();

        ItemVenda item = venda.adicionarItem(bola, new BigDecimal("2"));

        assertThat(venda.getItens()).hasSize(1);
        assertThat(item.getProduto()).isSameAs(bola);
        assertThat(item.getQuantidade()).isEqualByComparingTo("2.000");
        assertThat(item.getValorUnitario()).isEqualByComparingTo("89.90");
        assertThat(item.calcularSubtotal()).isEqualByComparingTo("179.80");
        assertThat(venda.calcularTotal()).isEqualByComparingTo("179.80");
    }

    @Test
    void precoDoItemNaoDeveMudarQuandoPrecoDoProdutoMudar() {
        Venda venda = novaVenda();
        venda.adicionarItem(bola, new BigDecimal("1"));

        bola.alterarPreco(new BigDecimal("129.90"));

        assertThat(venda.calcularTotal()).isEqualByComparingTo("89.90");
    }

    @Test
    void deveCalcularSubtotalETotalComQuantidadeFracionada() {
        Venda venda = novaVenda();

        ItemVenda item = venda.adicionarItem(bola, new BigDecimal("1.500"));

        assertThat(item.getQuantidade()).isEqualByComparingTo("1.500");
        assertThat(item.calcularSubtotal()).isEqualByComparingTo("134.85");
        assertThat(venda.calcularTotal()).isEqualByComparingTo("134.85");
    }

    @Test
    void deveCalcularTotalComItensInteirosEFracionados() {
        Venda venda = novaVenda();

        venda.adicionarItem(bola, new BigDecimal("1.500"));
        venda.adicionarItem(tenis, new BigDecimal("2.250"));

        assertThat(venda.calcularTotal()).isEqualByComparingTo("584.63");
    }

    @Test
    void deveSomarItensNoTotal() {
        Venda venda = novaVenda();

        venda.adicionarItem(bola, new BigDecimal("2"));
        venda.adicionarItem(tenis, new BigDecimal("1"));

        assertThat(venda.getItens()).hasSize(2);
        assertThat(venda.calcularTotal()).isEqualByComparingTo("379.70");
    }

    @Test
    void naoDeveAdicionarMesmoProdutoDuasVezes() {
        Venda venda = novaVenda();
        venda.adicionarItem(bola, new BigDecimal("1"));

        assertThatThrownBy(() -> venda.adicionarItem(bola, new BigDecimal("1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Produto já incluído na venda");

        assertThat(venda.getItens()).hasSize(1);
    }

    @Test
    void naoDeveAdicionarItemComQuantidadeInvalida() {
        Venda venda = novaVenda();

        assertThatThrownBy(() -> venda.adicionarItem(bola, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade do item deve ser positiva");

        assertThatThrownBy(() -> venda.adicionarItem(bola, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade do item deve ser positiva");

        assertThatThrownBy(
                () -> venda.adicionarItem(bola, new BigDecimal("-1.000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade do item deve ser positiva");

        assertThatThrownBy(
                () -> venda.adicionarItem(bola, new BigDecimal("1.0001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A quantidade deve ter no máximo 3 casas decimais");

        assertThat(venda.getItens()).isEmpty();
    }

    @Test
    void deveRemoverItemPeloIdentificador() {
        Venda venda = novaVenda();
        ItemVenda item = venda.adicionarItem(bola, new BigDecimal("1"));
        ReflectionTestUtils.setField(item, "id", 10L);

        venda.removerItem(10L);

        assertThat(venda.getItens()).isEmpty();
    }

    @Test
    void naoDeveRemoverItemDeOutraVenda() {
        Venda venda = novaVenda();
        venda.adicionarItem(bola, new BigDecimal("1"));

        assertThatThrownBy(() -> venda.removerItem(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item não pertence à venda");
    }

    @Test
    void naoDeveFinalizarVendaSemItens() {
        Venda venda = novaVenda();

        assertThatThrownBy(venda::finalizar)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Venda deve possuir ao menos um item");

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.ABERTA);
    }

    @Test
    void deveFinalizarVendaComItens() {
        Venda venda = novaVenda();
        venda.adicionarItem(bola, new BigDecimal("1"));

        venda.finalizar();

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.FINALIZADA);
    }

    @Test
    void deveCancelarVendaAberta() {
        Venda venda = novaVenda();

        venda.cancelar();

        assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
    }

    @Test
    void naoDeveAlterarVendaFinalizada() {
        Venda venda = novaVenda();
        venda.adicionarItem(bola, new BigDecimal("1"));
        venda.finalizar();

        assertThatThrownBy(() -> venda.adicionarItem(tenis, new BigDecimal("1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Somente vendas abertas podem ser alteradas");

        assertThatThrownBy(venda::cancelar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Somente vendas abertas podem ser alteradas");
    }

    @Test
    void naoDeveAlterarVendaCancelada() {
        Venda venda = novaVenda();
        venda.cancelar();

        assertThatThrownBy(() -> venda.adicionarItem(bola, new BigDecimal("1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Somente vendas abertas podem ser alteradas");

        assertThatThrownBy(venda::finalizar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Somente vendas abertas podem ser alteradas");
    }
}
