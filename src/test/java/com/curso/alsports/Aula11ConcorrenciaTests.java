package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Cliente;
import com.curso.alsports.model.Colaborador;
import com.curso.alsports.model.FuncaoColaborador;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Produto;
import com.curso.alsports.model.Venda;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.ClienteRepository;
import com.curso.alsports.repository.ColaboradorRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.curso.alsports.repository.ProdutoRepository;
import com.curso.alsports.service.ProdutoService;
import com.curso.alsports.service.VendaService;

/**
 * Testes de concorrência da Aula 11 com PostgreSQL real.
 *
 * <p>A classe não abre transação de teste. Cada chamada aos beans
 * {@link VendaService} e {@link ProdutoService} parte de uma thread própria e
 * atravessa o proxy do Spring, produzindo transações independentes.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class Aula11ConcorrenciaTests {

    private static final String SALDO_INSUFICIENTE =
            "Quantidade para retirada maior que o estoque disponível";

    @Autowired
    private VendaService vendaService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaProdutoRepository categoriaProdutoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void devePermitirApenasUmaFinalizacaoQuandoDuasVendasDisputamOMesmoSaldo()
            throws Exception {

        Participantes participantes = criarParticipantes();
        ProdutoCriado produto = criarProduto("5.000");
        List<Long> vendaIds = new ArrayList<>();

        try {
            Venda primeira = criarVendaComItem(
                    participantes, produto.produto(), "4.000");
            Venda segunda = criarVendaComItem(
                    participantes, produto.produto(), "4.000");
            vendaIds.add(primeira.getId());
            vendaIds.add(segunda.getId());

            List<Resultado> resultados = executarConcorrentemente(List.of(
                    () -> finalizarVenda(primeira.getId()),
                    () -> finalizarVenda(segunda.getId())));

            assertThat(resultados)
                    .filteredOn(Resultado::sucesso)
                    .hasSize(1);
            assertThat(resultados)
                    .filteredOn(resultado -> !resultado.sucesso())
                    .hasSize(1)
                    .allMatch(resultado -> SALDO_INSUFICIENTE
                            .equals(resultado.mensagem()));

            BigDecimal saldoFinal = saldoPersistido(produto.produto().getId());
            assertThat(saldoFinal).isEqualByComparingTo("1.000");
            assertThat(saldoFinal).isGreaterThanOrEqualTo(BigDecimal.ZERO);

            assertThat(quantidadeFinalizadas(vendaIds)).isEqualTo(1);
            assertThat(quantidadeAbertas(vendaIds)).isEqualTo(1);

            BigDecimal baixaTotal = new BigDecimal("5.000").subtract(saldoFinal);
            assertThat(baixaTotal).isEqualByComparingTo("4.000");
        } finally {
            limpar(participantes, List.of(produto), vendaIds);
        }
    }

    @Test
    void deveCoordenarVendaERetiraManualConcorrentes() throws Exception {
        Participantes participantes = criarParticipantes();
        ProdutoCriado produto = criarProduto("5.000");
        List<Long> vendaIds = new ArrayList<>();

        try {
            Venda venda = criarVendaComItem(
                    participantes, produto.produto(), "4.000");
            vendaIds.add(venda.getId());

            List<Resultado> resultados = executarConcorrentemente(List.of(
                    () -> finalizarVenda(venda.getId()),
                    () -> retirarEstoque(produto.produto().getId(), "4.000")));

            assertThat(resultados)
                    .filteredOn(Resultado::sucesso)
                    .hasSize(1);
            assertThat(resultados)
                    .filteredOn(resultado -> !resultado.sucesso())
                    .hasSize(1)
                    .allMatch(resultado -> SALDO_INSUFICIENTE
                            .equals(resultado.mensagem()));

            BigDecimal saldoFinal = saldoPersistido(produto.produto().getId());
            assertThat(saldoFinal).isEqualByComparingTo("1.000");
            assertThat(saldoFinal).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(statusPersistido(venda.getId()))
                    .isEqualTo(resultados.get(0).sucesso()
                            ? "FINALIZADA"
                            : "ABERTA");
        } finally {
            limpar(participantes, List.of(produto), vendaIds);
        }
    }

    @Test
    void deveCoordenarVendaEEntradaManualConcorrentes() throws Exception {
        Participantes participantes = criarParticipantes();
        ProdutoCriado produto = criarProduto("5.000");
        List<Long> vendaIds = new ArrayList<>();

        try {
            Venda venda = criarVendaComItem(
                    participantes, produto.produto(), "4.000");
            vendaIds.add(venda.getId());

            List<Resultado> resultados = executarConcorrentemente(List.of(
                    () -> finalizarVenda(venda.getId()),
                    () -> receberEstoque(produto.produto().getId(), "4.000")));

            assertThat(resultados)
                    .allMatch(Resultado::sucesso);

            assertThat(saldoPersistido(produto.produto().getId()))
                    .isEqualByComparingTo("5.000");
            assertThat(statusPersistido(venda.getId()))
                    .isEqualTo("FINALIZADA");
        } finally {
            limpar(participantes, List.of(produto), vendaIds);
        }
    }

    @Test
    void deveDesfazerBaixaDeTodosOsItensQuandoUmProdutoNaoPossuirSaldo() {
        Participantes participantes = criarParticipantes();
        ProdutoCriado primeiro = criarProduto("5.000");
        ProdutoCriado segundo = criarProduto("1.000");
        List<Long> vendaIds = new ArrayList<>();

        try {
            Venda venda = vendaService.cadastrar(
                    participantes.clienteId(), participantes.colaboradorId());
            vendaService.adicionarItem(
                    venda.getId(), primeiro.produto().getId(),
                    new BigDecimal("2.000"));
            vendaService.adicionarItem(
                    venda.getId(), segundo.produto().getId(),
                    new BigDecimal("2.000"));
            vendaIds.add(venda.getId());

            assertThatThrownBy(() -> vendaService.finalizar(venda.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(SALDO_INSUFICIENTE);

            assertThat(saldoPersistido(primeiro.produto().getId()))
                    .isEqualByComparingTo("5.000");
            assertThat(saldoPersistido(segundo.produto().getId()))
                    .isEqualByComparingTo("1.000");
            assertThat(statusPersistido(venda.getId())).isEqualTo("ABERTA");
        } finally {
            limpar(participantes, List.of(primeiro, segundo), vendaIds);
        }
    }

    @Test
    void deveProcessarProdutosEmOrdemCrescenteSemDeadlock() throws Exception {
        Participantes participantes = criarParticipantes();
        ProdutoCriado primeiro = criarProduto("10.000");
        ProdutoCriado segundo = criarProduto("10.000");
        List<Long> vendaIds = new ArrayList<>();

        try {
            Venda vendaDireta = criarVendaComItens(
                    participantes,
                    List.of(primeiro, segundo),
                    "1.000");
            Venda vendaInversa = criarVendaComItens(
                    participantes,
                    List.of(segundo, primeiro),
                    "1.000");
            vendaIds.add(vendaDireta.getId());
            vendaIds.add(vendaInversa.getId());

            List<Resultado> resultados = executarConcorrentemente(List.of(
                    () -> finalizarVenda(vendaDireta.getId()),
                    () -> finalizarVenda(vendaInversa.getId())));

            assertThat(resultados).allMatch(Resultado::sucesso);
            assertThat(saldoPersistido(primeiro.produto().getId()))
                    .isEqualByComparingTo("8.000");
            assertThat(saldoPersistido(segundo.produto().getId()))
                    .isEqualByComparingTo("8.000");
            assertThat(quantidadeFinalizadas(vendaIds)).isEqualTo(2);
        } finally {
            limpar(participantes, List.of(primeiro, segundo), vendaIds);
        }
    }

    @Test
    void deveRespeitarSaldoFracionadoEmFinalizacoesConcorrentes()
            throws Exception {

        Participantes participantes = criarParticipantes();
        ProdutoCriado produto = criarProduto("2.000");
        List<Long> vendaIds = new ArrayList<>();

        try {
            Venda primeira = criarVendaComItem(
                    participantes, produto.produto(), "1.500");
            Venda segunda = criarVendaComItem(
                    participantes, produto.produto(), "1.500");
            vendaIds.add(primeira.getId());
            vendaIds.add(segunda.getId());

            List<Resultado> resultados = executarConcorrentemente(List.of(
                    () -> finalizarVenda(primeira.getId()),
                    () -> finalizarVenda(segunda.getId())));

            assertThat(resultados)
                    .filteredOn(Resultado::sucesso)
                    .hasSize(1);
            assertThat(resultados)
                    .filteredOn(resultado -> !resultado.sucesso())
                    .hasSize(1)
                    .allMatch(resultado -> SALDO_INSUFICIENTE
                            .equals(resultado.mensagem()));

            assertThat(saldoPersistido(produto.produto().getId()))
                    .isEqualByComparingTo("0.500");
            assertThat(quantidadeFinalizadas(vendaIds)).isEqualTo(1);
            assertThat(quantidadeAbertas(vendaIds)).isEqualTo(1);
        } finally {
            limpar(participantes, List.of(produto), vendaIds);
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private record Participantes(
            Long pessoaClienteId,
            Long clienteId,
            Long pessoaVendedorId,
            Long colaboradorId) {
    }

    private record ProdutoCriado(Produto produto, Long categoriaId) {
    }

    private record Resultado(boolean sucesso, String mensagem) {
    }

    private List<Resultado> executarConcorrentemente(
            List<Callable<Resultado>> tarefas) throws Exception {

        CountDownLatch inicioSimultaneo = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(tarefas.size());

        try {
            List<Future<Resultado>> futuros = new ArrayList<>();

            for (Callable<Resultado> tarefa : tarefas) {
                futuros.add(executor.submit(() -> {
                    inicioSimultaneo.await();
                    return tarefa.call();
                }));
            }

            inicioSimultaneo.countDown();

            List<Resultado> resultados = new ArrayList<>();

            for (Future<Resultado> futuro : futuros) {
                resultados.add(futuro.get(30, TimeUnit.SECONDS));
            }

            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }

    private Resultado finalizarVenda(Long vendaId) {
        try {
            vendaService.finalizar(vendaId);
            return new Resultado(true, null);
        } catch (IllegalArgumentException excecao) {
            return new Resultado(false, excecao.getMessage());
        } catch (RuntimeException excecao) {
            return new Resultado(
                    false,
                    excecao.getClass().getSimpleName()
                            + ": " + excecao.getMessage());
        }
    }

    private Resultado retirarEstoque(Long produtoId, String quantidade) {
        try {
            produtoService.retirarEstoque(
                    produtoId, new BigDecimal(quantidade));
            return new Resultado(true, null);
        } catch (IllegalArgumentException excecao) {
            return new Resultado(false, excecao.getMessage());
        } catch (RuntimeException excecao) {
            return new Resultado(
                    false,
                    excecao.getClass().getSimpleName()
                            + ": " + excecao.getMessage());
        }
    }

    private Resultado receberEstoque(Long produtoId, String quantidade) {
        try {
            produtoService.receberEstoque(
                    produtoId, new BigDecimal(quantidade));
            return new Resultado(true, null);
        } catch (RuntimeException excecao) {
            return new Resultado(
                    false,
                    excecao.getClass().getSimpleName()
                            + ": " + excecao.getMessage());
        }
    }

    private Participantes criarParticipantes() {
        Pessoa pessoaCliente = pessoaRepository.save(novaPessoa("Ana Souza"));
        Cliente cliente = clienteRepository.save(
                new Cliente(pessoaCliente, null));

        Pessoa pessoaVendedor = pessoaRepository.save(novaPessoa("Bruno Lima"));
        Colaborador vendedor = colaboradorRepository.save(new Colaborador(
                pessoaVendedor,
                "VEN-" + sufixo(),
                FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29)));

        return new Participantes(
                pessoaCliente.getId(),
                cliente.getId(),
                pessoaVendedor.getId(),
                vendedor.getId());
    }

    private ProdutoCriado criarProduto(String saldo) {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria " + sufixo());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();
        produto.setCodigoBarras("AULA11-" + sufixo());
        produto.setNome("Produto Aula 11 " + sufixo());
        produto.setQuantidade(new BigDecimal(saldo));
        produto.setEstoqueMinimo(new BigDecimal("1.000"));
        produto.setPreco(new BigDecimal("89.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        return new ProdutoCriado(
                produtoRepository.save(produto), categoria.getId());
    }

    private Venda criarVendaComItem(
            Participantes participantes,
            Produto produto,
            String quantidade) {

        Venda venda = vendaService.cadastrar(
                participantes.clienteId(), participantes.colaboradorId());
        vendaService.adicionarItem(
                venda.getId(), produto.getId(), new BigDecimal(quantidade));
        return venda;
    }

    private Venda criarVendaComItens(
            Participantes participantes,
            List<ProdutoCriado> produtos,
            String quantidade) {

        Venda venda = vendaService.cadastrar(
                participantes.clienteId(), participantes.colaboradorId());

        for (ProdutoCriado produto : produtos) {
            vendaService.adicionarItem(
                    venda.getId(),
                    produto.produto().getId(),
                    new BigDecimal(quantidade));
        }

        return venda;
    }

    private BigDecimal saldoPersistido(Long produtoId) {
        return jdbcTemplate.queryForObject(
                "SELECT quantidade FROM produto WHERE id = ?",
                BigDecimal.class,
                produtoId);
    }

    private String statusPersistido(Long vendaId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM venda WHERE id = ?",
                String.class,
                vendaId);
    }

    private long quantidadeFinalizadas(List<Long> vendaIds) {
        return vendaIds.stream()
                .map(this::statusPersistido)
                .filter("FINALIZADA"::equals)
                .count();
    }

    private long quantidadeAbertas(List<Long> vendaIds) {
        return vendaIds.stream()
                .map(this::statusPersistido)
                .filter("ABERTA"::equals)
                .count();
    }

    private void limpar(
            Participantes participantes,
            List<ProdutoCriado> produtos,
            List<Long> vendaIds) {

        for (Long vendaId : vendaIds) {
            jdbcTemplate.update(
                    "DELETE FROM item_venda WHERE venda_id = ?", vendaId);
            jdbcTemplate.update(
                    "DELETE FROM venda WHERE id = ?", vendaId);
        }

        jdbcTemplate.update(
                "DELETE FROM colaborador WHERE id = ?",
                participantes.colaboradorId());
        jdbcTemplate.update(
                "DELETE FROM cliente WHERE id = ?",
                participantes.clienteId());
        jdbcTemplate.update(
                "DELETE FROM pessoa WHERE id = ?",
                participantes.pessoaClienteId());
        jdbcTemplate.update(
                "DELETE FROM pessoa WHERE id = ?",
                participantes.pessoaVendedorId());

        for (ProdutoCriado produto : produtos) {
            jdbcTemplate.update(
                    "DELETE FROM produto WHERE id = ?",
                    produto.produto().getId());
            jdbcTemplate.update(
                    "DELETE FROM categoria_produto WHERE id = ?",
                    produto.categoriaId());
        }
    }

    private String sufixo() {
        return Long.toString(System.nanoTime(), 36);
    }

    private Pessoa novaPessoa(String nome) {
        return new Pessoa(
                nome,
                nome.toLowerCase().replace(" ", ".") + "." + sufixo()
                        + "@example.com",
                cpfValido(),
                LocalDate.now());
    }

    private String cpfValido() {
        long semente = Math.abs(System.nanoTime() % 1_000_000_000L);
        String base = String.format("%09d", semente);
        char primeiro = base.charAt(0);
        boolean repetidos = base.chars().allMatch(digito -> digito == primeiro);
        String nove = repetidos ? "1" + base.substring(1) : base;

        String dez = nove + digitoCpf(nove, 10);
        return dez + digitoCpf(dez, 11);
    }

    private int digitoCpf(String base, int pesoInicial) {
        int soma = 0;

        for (int indice = 0; indice < base.length(); indice++) {
            soma += Character.getNumericValue(base.charAt(indice))
                    * (pesoInicial - indice);
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
