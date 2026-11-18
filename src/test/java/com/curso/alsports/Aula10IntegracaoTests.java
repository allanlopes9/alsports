package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Cliente;
import com.curso.alsports.model.Colaborador;
import com.curso.alsports.model.FuncaoColaborador;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Produto;
import com.curso.alsports.model.StatusVenda;
import com.curso.alsports.model.Venda;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.ClienteRepository;
import com.curso.alsports.repository.ColaboradorRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.curso.alsports.repository.ProdutoRepository;
import com.curso.alsports.repository.VendaRepository;
import com.curso.alsports.service.VendaService;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;
import liquibase.change.Change;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class Aula10IntegracaoTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaProdutoRepository categoriaProdutoRepository;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    // ------------------------------------------------------------------
    // Liquibase / modelo físico
    // ------------------------------------------------------------------

    @Test
    void liquibaseDeveCriarTabelasEContraintsDaAula10() {
        List<String> tabelas = jdbcTemplate.queryForList(
                """
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_name IN
                       ('pessoa', 'cliente', 'colaborador', 'venda', 'item_venda')
                """,
                String.class);

        assertThat(tabelas).containsExactlyInAnyOrder(
                "pessoa", "cliente", "colaborador", "venda", "item_venda");

        List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT conname
                  FROM pg_constraint
                 WHERE conname IN (
                    'uk_pessoa_cpf',
                    'ck_pessoa_cpf_formato',
                    'ck_pessoa_status',
                    'uk_cliente_pessoa',
                    'fk_cliente_pessoa',
                    'uk_colaborador_pessoa',
                    'uk_colaborador_matricula',
                    'fk_colaborador_pessoa',
                    'ck_colaborador_funcao',
                    'fk_venda_cliente',
                    'fk_venda_vendedor',
                    'ck_venda_status',
                    'uk_item_venda_produto',
                    'fk_item_venda_venda',
                    'fk_item_venda_produto',
                    'ck_item_venda_quantidade',
                    'ck_item_venda_valor')
                """,
                String.class);

        assertThat(constraints).hasSize(17);

        Integer indiceEmail = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM pg_indexes
                 WHERE schemaname = 'public'
                   AND indexname = 'uk_pessoa_email_ci'
                """,
                Integer.class);

        assertThat(indiceEmail).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // Persistência e filtros
    // ------------------------------------------------------------------

    @Test
    void devePermitirQueUmaMesmaPessoaSejaClienteEColaborador() {
        Pessoa pessoa = pessoaRepository.save(novaPessoa("Ana Souza"));

        Cliente cliente = clienteRepository.save(new Cliente(pessoa, null));
        Colaborador colaborador = colaboradorRepository.save(new Colaborador(
                pessoa, "VEN-" + sufixo(), FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29)));

        assertThat(cliente.getPessoa().getId()).isEqualTo(pessoa.getId());
        assertThat(colaborador.getPessoa().getId()).isEqualTo(pessoa.getId());
        assertThat(clienteRepository.existsByPessoaId(pessoa.getId())).isTrue();
        assertThat(colaboradorRepository.existsByPessoaId(pessoa.getId()))
                .isTrue();
        assertThat(pessoaRepository.findById(pessoa.getId())).isPresent();
    }

    @Test
    void devePersistirPessoaClienteColaboradorEVenda() {
        Pessoa pessoaCliente = pessoaRepository.save(novaPessoa("Ana Souza"));
        Cliente cliente = clienteRepository.save(
                new Cliente(pessoaCliente, "(11) 99999-0000"));

        Pessoa pessoaVendedor = pessoaRepository.save(novaPessoa("Bruno Lima"));
        Colaborador vendedor = colaboradorRepository.save(new Colaborador(
                pessoaVendedor, "VEN-" + sufixo(), FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29)));

        Produto produto = produtoRepository.save(novoProduto("89.90", 10));

        Venda venda = new Venda(
                cliente, vendedor, LocalDateTime.of(2026, 11, 16, 10, 30));
        venda.adicionarItem(produto, new BigDecimal("2"));

        Venda vendaSalva = vendaRepository.saveAndFlush(venda);
        Long vendaId = vendaSalva.getId();

        entityManager.clear();

        Venda recuperada = vendaRepository.findOneById(vendaId).orElseThrow();

        assertThat(recuperada.getCliente().getPessoa().getNome())
                .isEqualTo("Ana Souza");
        assertThat(recuperada.getVendedor().getPessoa().getNome())
                .isEqualTo("Bruno Lima");
        assertThat(recuperada.getItens()).hasSize(1);
        assertThat(recuperada.getItens().get(0).getValorUnitario())
                .isEqualByComparingTo("89.90");
        assertThat(recuperada.calcularTotal()).isEqualByComparingTo("179.80");
    }

    @Test
    void deveFiltrarEPaginarVendas() {
        Pessoa pessoaCliente = pessoaRepository.save(novaPessoa("Ana Souza"));
        Cliente cliente = clienteRepository.save(
                new Cliente(pessoaCliente, null));

        Pessoa pessoaVendedor = pessoaRepository.save(novaPessoa("Bruno Lima"));
        Colaborador vendedor = colaboradorRepository.save(new Colaborador(
                pessoaVendedor, "VEN-" + sufixo(), FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29)));

        Produto produto = produtoRepository.save(novoProduto("89.90", 100));

        LocalDateTime hoje = LocalDateTime.now();

        Venda aberta = vendaRepository.save(new Venda(
                cliente, vendedor, hoje.minusDays(2)));

        Venda finalizada = new Venda(cliente, vendedor, hoje.minusDays(1));
        finalizada.adicionarItem(produto, new BigDecimal("1"));
        finalizada.finalizar();
        finalizada = vendaRepository.save(finalizada);

        Venda cancelada = new Venda(cliente, vendedor, hoje);
        cancelada.cancelar();
        cancelada = vendaRepository.save(cancelada);

        Page<Venda> porStatus = vendaService.pesquisar(
                StatusVenda.FINALIZADA,
                cliente.getId(),
                null,
                null,
                PageRequest.of(0, 20, Sort.by("id")));

        assertThat(porStatus.getContent())
                .extracting(Venda::getId)
                .containsExactly(finalizada.getId());

        Page<Venda> porPeriodo = vendaService.pesquisar(
                null,
                cliente.getId(),
                hoje.toLocalDate().minusDays(2),
                hoje.toLocalDate().minusDays(2),
                PageRequest.of(0, 20, Sort.by("id")));

        assertThat(porPeriodo.getContent())
                .extracting(Venda::getId)
                .containsExactly(aberta.getId());

        Page<Venda> primeiraPagina = vendaService.pesquisar(
                null,
                cliente.getId(),
                null,
                null,
                PageRequest.of(0, 2, Sort.by("id")));

        assertThat(primeiraPagina.getTotalElements()).isEqualTo(3);
        assertThat(primeiraPagina.getTotalPages()).isEqualTo(2);
        assertThat(primeiraPagina.getContent()).hasSize(2);
        assertThat(primeiraPagina.isFirst()).isTrue();
        assertThat(primeiraPagina.hasNext()).isTrue();

        assertThat(cancelada.getStatus()).isEqualTo(StatusVenda.CANCELADA);
    }

    @Test
    void naoDeveAceitarPeriodoInvertido() {
        assertThatThrownBy(() -> vendaService.pesquisar(
                null,
                null,
                LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 11, 1),
                PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data inicial não pode ser posterior à data final");
    }

    @Test
    void naoDeveAceitarCampoDeOrdenacaoDeVendaForaDaLista() {
        assertThatThrownBy(() -> vendaService.pesquisar(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20, Sort.by("cliente.nome"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campo de ordenação inválido");
    }

    // ------------------------------------------------------------------
    // HTTP: pessoas
    // ------------------------------------------------------------------

    @Test
    void deveCriarPessoaPelaApiComCpfValido() throws Exception {
        String cpf = cpfValido();
        String email = "ana." + sufixo() + "@example.com";

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Ana Souza","email":"%s","cpf":"%s"}
                            """.formatted(email.toUpperCase(), cpf)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", containsString("/api/pessoas/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.dataCadastro").exists());
    }

    @Test
    void deveRetornar400ComCpfInvalido() throws Exception {
        String cpfInvalido = "11111111111";

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Ana Souza","email":"ana.%s@example.com","cpf":"%s"}
                            """.formatted(sufixo(), cpfInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.cpf").exists());
    }

    @Test
    void deveRetornar409QuandoCpfForDuplicado() throws Exception {
        String cpf = cpfValido();

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Ana Souza","email":"ana.%s@example.com","cpf":"%s"}
                            """.formatted(sufixo(), cpf)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Outra Pessoa","email":"outra.%s@example.com","cpf":"%s"}
                            """.formatted(sufixo(), cpf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado"));
    }

    @Test
    void deveRetornar404AoBuscarPessoaInexistente() throws Exception {
        mockMvc.perform(get("/api/pessoas/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveInativarPessoaPelaApi() throws Exception {
        Pessoa pessoa = pessoaRepository.save(novaPessoa("Ana Souza"));

        mockMvc.perform(put("/api/pessoas/{id}/status", pessoa.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INATIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"));
    }

    // ------------------------------------------------------------------
    // HTTP: fornecedores (CNPJ no DTO)
    // ------------------------------------------------------------------

    @Test
    void deveRetornar400ComCnpjInvalidoNoFornecedor() throws Exception {
        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"razaoSocial":"Fornecedor Inválido Ltda","cnpj":"11222333000182"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.cnpj").exists());
    }

    @Test
    void deveCriarFornecedorComCnpjValido() throws Exception {
        String cnpj = cnpjValido();

        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"razaoSocial":"Fornecedor Aula 10 Ltda","cnpj":"%s"}
                            """.formatted(cnpj)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", containsString("/api/fornecedores/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cnpj").value(cnpj))
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void deveAssociarFornecedorAoProduto() throws Exception {
        String cnpj = cnpjValido();

        MvcResult fornecedor = mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"razaoSocial":"Fornecedor Aula 10 Ltda","cnpj":"%s"}
                            """.formatted(cnpj)))
                .andExpect(status().isCreated())
                .andReturn();

        Long fornecedorId = lerId(fornecedor);

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria " + sufixo());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "codigoBarras":"FORN-%s",
                              "nome":"Produto com fornecedor %s",
                              "quantidade":1,
                              "estoqueMinimo":1.000,
                              "preco":49.90,
                              "dataCadastro":"2026-11-16",
                              "ativo":true,
                              "unidadeMedida":"UN",
                              "categoriaId":%d,
                              "fornecedorId":%d
                            }
                            """.formatted(sufixo(), sufixo(),
                                    categoria.getId(), fornecedorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fornecedor.id").value(fornecedorId))
                .andExpect(jsonPath("$.fornecedor.cnpj").value(cnpj));
    }

    // ------------------------------------------------------------------
    // HTTP: fluxo completo de venda
    // ------------------------------------------------------------------

    @Test
    void deveExecutarFluxoCompletoDeVendaPelaApi() throws Exception {
        Long clienteId = criarClientePelaApi("Ana Souza");
        Long vendedorId = criarColaboradorPelaApi(
                "Bruno Lima", FuncaoColaborador.VENDEDOR);
        Long produtoId = produtoRepository.save(novoProduto("89.90", 10)).getId();

        MvcResult abertura = mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"clienteId":%d,"vendedorId":%d}
                            """.formatted(clienteId, vendedorId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", containsString("/api/vendas/")))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.clienteNome").value("Ana Souza"))
                .andExpect(jsonPath("$.vendedorNome").value("Bruno Lima"))
                .andExpect(jsonPath("$.itens").isEmpty())
                .andReturn();

        Long vendaId = lerId(abertura);

        MvcResult item = mockMvc.perform(post(
                        "/api/vendas/{id}/itens", vendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"produtoId":%d,"quantidade":2}
                            """.formatted(produtoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].produtoId").value(produtoId))
                .andExpect(jsonPath("$.itens[0].quantidade")
                        .value(closeTo(2.0, 0.0001)))
                .andExpect(jsonPath("$.itens[0].valorUnitario").value(89.90))
                .andExpect(jsonPath("$.itens[0].subtotal").value(179.80))
                .andExpect(jsonPath("$.total").value(179.80))
                .andReturn();

        Number itemIdLido = JsonPath.read(
                item.getResponse().getContentAsString(), "$.itens[0].id");
        Long itemId = itemIdLido.longValue();

        mockMvc.perform(post("/api/vendas/{id}/finalizacao", vendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.total").value(179.80));

        mockMvc.perform(get("/api/produtos/{id}", produtoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(closeTo(8.0, 0.0001)));

        mockMvc.perform(get("/api/vendas/{id}", vendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value(itemId))
                .andExpect(jsonPath("$.total").value(179.80));

        mockMvc.perform(post("/api/vendas/{id}/itens", vendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"produtoId":%d,"quantidade":1}
                            """.formatted(produtoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Somente vendas abertas podem ser alteradas"));
    }

    @Test
    void deveExecutarVendaFracionadaPelaApi() throws Exception {
        Long clienteId = criarClientePelaApi("Ana Souza");
        Long vendedorId = criarColaboradorPelaApi(
                "Bruno Lima", FuncaoColaborador.VENDEDOR);
        Long produtoId = produtoRepository.save(
                novoProduto("89.90", new BigDecimal("10.000"))).getId();

        MvcResult abertura = mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"clienteId":%d,"vendedorId":%d}
                            """.formatted(clienteId, vendedorId)))
                .andExpect(status().isCreated())
                .andReturn();

        Long vendaId = lerId(abertura);

        mockMvc.perform(post("/api/vendas/{id}/itens", vendaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"produtoId":%d,"quantidade":1.500}
                            """.formatted(produtoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].quantidade")
                        .value(closeTo(1.5, 0.0001)))
                .andExpect(jsonPath("$.itens[0].valorUnitario").value(89.90))
                .andExpect(jsonPath("$.itens[0].subtotal").value(134.85))
                .andExpect(jsonPath("$.total").value(134.85));

        mockMvc.perform(post("/api/vendas/{id}/finalizacao", vendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.total").value(134.85));

        mockMvc.perform(get("/api/produtos/{id}", produtoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade")
                        .value(closeTo(8.5, 0.0001)));

        BigDecimal quantidadeItem = jdbcTemplate.queryForObject(
                "SELECT quantidade FROM item_venda WHERE venda_id = ?",
                BigDecimal.class,
                vendaId);

        assertThat(quantidadeItem).isEqualByComparingTo("1.500");
    }

    @Test
    void deveRetornar400ParaQuantidadesInvalidasNaVenda() throws Exception {
        Long clienteId = criarClientePelaApi("Ana Souza");
        Long vendedorId = criarColaboradorPelaApi(
                "Bruno Lima", FuncaoColaborador.VENDEDOR);
        Long produtoId = produtoRepository.save(novoProduto("89.90", 10)).getId();

        MvcResult abertura = mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"clienteId":%d,"vendedorId":%d}
                            """.formatted(clienteId, vendedorId)))
                .andExpect(status().isCreated())
                .andReturn();

        Long vendaId = lerId(abertura);

        for (String quantidade : List.of(
                "0",
                "-1.000",
                "1.0001",
                "1000000000000000.000")) {

            mockMvc.perform(post("/api/vendas/{id}/itens", vendaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"produtoId":%d,"quantidade":%s}
                                """.formatted(produtoId, quantidade)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fields.quantidade").exists());
        }

        mockMvc.perform(get("/api/vendas/{id}", vendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveDesfazerBaixaFracionadaQuandoSaldoForInsuficiente() {
        CenarioVenda cenario = criarCenarioVenda(1);

        try {
            vendaService.adicionarItem(
                    cenario.vendaId(), cenario.produtoId(),
                    new BigDecimal("1.500"));

            assertThatThrownBy(() -> vendaService.finalizar(cenario.vendaId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Quantidade para retirada maior que o estoque disponível");

            BigDecimal saldo = jdbcTemplate.queryForObject(
                    "SELECT quantidade FROM produto WHERE id = ?",
                    BigDecimal.class,
                    cenario.produtoId());

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM venda WHERE id = ?",
                    String.class,
                    cenario.vendaId());

            assertThat(saldo).isEqualByComparingTo("1.000");
            assertThat(status).isEqualTo("ABERTA");
        } finally {
            limparCenario(cenario);
        }
    }

    @Test
    void devePreservarPrecoHistoricoComQuantidadeFracionada() {
        Pessoa pessoaCliente = pessoaRepository.save(novaPessoa("Ana Souza"));
        Cliente cliente = clienteRepository.save(
                new Cliente(pessoaCliente, null));

        Pessoa pessoaVendedor = pessoaRepository.save(novaPessoa("Bruno Lima"));
        Colaborador vendedor = colaboradorRepository.save(new Colaborador(
                pessoaVendedor, "VEN-" + sufixo(), FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29)));

        Produto produto = produtoRepository.save(
                novoProduto("89.90", new BigDecimal("10.000")));

        Venda venda = new Venda(
                cliente, vendedor, LocalDateTime.of(2026, 11, 16, 10, 30));
        venda.adicionarItem(produto, new BigDecimal("1.500"));
        Venda vendaSalva = vendaRepository.saveAndFlush(venda);

        produto.alterarPreco(new BigDecimal("129.90"));
        produtoRepository.saveAndFlush(produto);

        Long vendaId = vendaSalva.getId();
        entityManager.clear();

        Venda recuperada = vendaRepository.findOneById(vendaId).orElseThrow();

        assertThat(recuperada.getItens().get(0).getValorUnitario())
                .isEqualByComparingTo("89.90");
        assertThat(recuperada.getItens().get(0).getQuantidade())
                .isEqualByComparingTo("1.500");
        assertThat(recuperada.calcularTotal()).isEqualByComparingTo("134.85");
    }

    @Test
    void devePersistirQuantidadesComoNumeric18Escala3() {
        for (String tabela : List.of("produto", "item_venda")) {
            List<Map<String, Object>> colunas = jdbcTemplate
                    .queryForList(
                            """
                            SELECT data_type, numeric_precision, numeric_scale
                              FROM information_schema.columns
                             WHERE table_schema = 'public'
                               AND table_name = ?
                               AND column_name = 'quantidade'
                            """,
                            tabela);

            assertThat(colunas).hasSize(1);
            assertThat(colunas.get(0).get("data_type"))
                    .isEqualTo("numeric");
            assertThat(((Number) colunas.get(0).get("numeric_precision"))
                    .intValue()).isEqualTo(18);
            assertThat(((Number) colunas.get(0).get("numeric_scale"))
                    .intValue()).isEqualTo(3);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveExecutarRollbackDosChangesetsDeQuantidade() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(
                            new JdbcConnection(connection));

            ChangeSet itemVenda = buscarChangeSet(
                    database, "007-02-quantidade-decimal-item-venda");
            ChangeSet produto = buscarChangeSet(
                    database, "007-01-quantidade-decimal-produto");

            Map<Long, BigDecimal> produtoAntes = quantidades(
                    connection, "produto");
            Map<Long, BigDecimal> itemVendaAntes = quantidades(
                    connection, "item_venda");

            assertThat(contarFracionadas(connection, "produto"))
                    .as("O rollback normal exige produto sem frações")
                    .isZero();
            assertThat(contarFracionadas(connection, "item_venda"))
                    .as("O rollback normal exige item_venda sem frações")
                    .isZero();

            List<String> sqlItemVenda = sqlDeRollback(database, itemVenda);
            List<String> sqlProduto = sqlDeRollback(database, produto);

            try {
                executarSql(connection, sqlItemVenda);
                executarSql(connection, sqlProduto);

                assertThat(tipoColuna(connection, "produto"))
                        .isEqualTo("integer");
                assertThat(tipoColuna(connection, "item_venda"))
                        .isEqualTo("integer");

                assertThat(definicaoConstraint(
                        connection, "ck_produto_quantidade"))
                        .contains("quantidade >= 0");
                assertThat(definicaoConstraint(
                        connection, "ck_item_venda_quantidade"))
                        .contains("quantidade > 0");
            } finally {
                connection.rollback();
            }

            assertThat(colunaQuantidade(connection, "produto"))
                    .containsEntry("data_type", "numeric")
                    .containsEntry("numeric_precision", 18)
                    .containsEntry("numeric_scale", 3);
            assertThat(colunaQuantidade(connection, "item_venda"))
                    .containsEntry("data_type", "numeric")
                    .containsEntry("numeric_precision", 18)
                    .containsEntry("numeric_scale", 3);

            assertThat(definicaoConstraint(
                    connection, "ck_produto_quantidade"))
                    .contains("quantidade >= (0)::numeric");
            assertThat(definicaoConstraint(
                    connection, "ck_item_venda_quantidade"))
                    .contains("quantidade > (0)::numeric");

            assertThat(quantidades(connection, "produto"))
                    .isEqualTo(produtoAntes);
            assertThat(quantidades(connection, "item_venda"))
                    .isEqualTo(itemVendaAntes);

            assertThat(contarChangesets007(connection)).isEqualTo(2);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveBloquearRollbackQuandoHouverQuantidadeFracionada() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(
                            new JdbcConnection(connection));

            String nomeCategoria = "Categoria rollback " + sufixo();
            String nomeProduto = "Produto rollback " + sufixo();
            String codigoBarras = "RB-" + sufixo();

            try {
                long categoriaId = inserirCategoria(connection, nomeCategoria);
                inserirProduto(connection, nomeProduto,
                        new BigDecimal("1.500"), categoriaId, codigoBarras);

                List<String> sqlProduto = sqlDeRollback(
                        database, buscarChangeSet(database,
                                "007-01-quantidade-decimal-produto"));

                assertThatThrownBy(() -> executarSql(connection, sqlProduto))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining(
                                "Rollback 007-01 bloqueado: existem"
                                        + " quantidades fracionadas em"
                                        + " produto.quantidade");
            } finally {
                connection.rollback();
            }

            assertThat(colunaQuantidade(connection, "produto"))
                    .containsEntry("data_type", "numeric")
                    .containsEntry("numeric_precision", 18)
                    .containsEntry("numeric_scale", 3);
            assertThat(contarPorNome(connection, "produto", nomeProduto))
                    .isZero();
            assertThat(contarPorNome(
                    connection, "categoria_produto", nomeCategoria)).isZero();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveBloquearRollbackDeItemVendaQuandoHouverQuantidadeFracionada()
            throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(
                            new JdbcConnection(connection));

            String identificador = sufixo();
            String nomeCategoria = "Categoria rollback item " + identificador;
            String nomeProduto = "Produto rollback item " + identificador;
            String codigoBarras = "RB-ITEM-" + identificador;
            String emailCliente = "cliente.rollback." + identificador
                    + "@example.com";
            String emailVendedor = "vendedor.rollback." + identificador
                    + "@example.com";
            String matricula = "RB-" + identificador;

            try {
                long categoriaId = inserirCategoria(connection, nomeCategoria);
                long produtoId = inserirProduto(connection, nomeProduto,
                        BigDecimal.ZERO, categoriaId, codigoBarras);
                long clienteId = inserirCliente(connection, emailCliente);
                long vendedorId = inserirVendedor(
                        connection, emailVendedor, matricula);
                long vendaId = inserirVenda(
                        connection, clienteId, vendedorId);

                inserirItemVenda(connection, vendaId, produtoId,
                        new BigDecimal("1.500"), new BigDecimal("10.00"));

                List<String> sqlItemVenda = sqlDeRollback(
                        database, buscarChangeSet(database,
                                "007-02-quantidade-decimal-item-venda"));

                assertThatThrownBy(
                        () -> executarSql(connection, sqlItemVenda))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining(
                                "Rollback 007-02 bloqueado: existem"
                                        + " quantidades fracionadas em"
                                        + " item_venda.quantidade");
            } finally {
                connection.rollback();
            }

            assertThat(colunaQuantidade(connection, "item_venda"))
                    .containsEntry("data_type", "numeric")
                    .containsEntry("numeric_precision", 18)
                    .containsEntry("numeric_scale", 3);
            assertThat(contarPorNome(connection, "produto", nomeProduto))
                    .isZero();
            assertThat(contarPorNome(
                    connection, "categoria_produto", nomeCategoria)).isZero();
            assertThat(contarPorEmail(connection, emailCliente)).isZero();
            assertThat(contarPorEmail(connection, emailVendedor)).isZero();
            assertThat(contarPorMatricula(connection, matricula)).isZero();
        }
    }

    private List<String> sqlDeRollback(
            Database database, ChangeSet changeSet) throws Exception {

        List<String> sqls = new ArrayList<>();

        for (Change change : changeSet.getRollback().getChanges()) {
            for (SqlStatement statement
                    : change.generateStatements(database)) {
                for (Sql sql : SqlGeneratorFactory.getInstance()
                        .generateSql(statement, database)) {
                    sqls.add(sql.toSql());
                }
            }
        }

        return sqls;
    }

    private void executarSql(Connection connection, List<String> sqls)
            throws Exception {

        try (Statement statement = connection.createStatement()) {
            for (String sql : sqls) {
                statement.execute(sql);
            }
        }
    }

    private Map<String, Object> colunaQuantidade(
            Connection connection, String tabela) throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT data_type, numeric_precision, numeric_scale"
                        + " FROM information_schema.columns"
                        + " WHERE table_schema = 'public'"
                        + " AND table_name = ?"
                        + " AND column_name = 'quantidade'")) {

            statement.setString(1, tabela);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "Coluna quantidade não encontrada: " + tabela);
                }

                Map<String, Object> coluna = new LinkedHashMap<>();
                coluna.put("data_type", resultSet.getString("data_type"));
                coluna.put("numeric_precision",
                        resultSet.getInt("numeric_precision"));
                coluna.put("numeric_scale",
                        resultSet.getInt("numeric_scale"));
                return coluna;
            }
        }
    }

    private String definicaoConstraint(
            Connection connection, String nome) throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint"
                        + " WHERE conname = ?")) {

            statement.setString(1, nome);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "Constraint não encontrada: " + nome);
                }
                return resultSet.getString(1);
            }
        }
    }

    private Map<Long, BigDecimal> quantidades(
            Connection connection, String tabela) throws Exception {

        Map<Long, BigDecimal> valores = new LinkedHashMap<>();

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT id, quantidade FROM " + tabela
                                + " ORDER BY id")) {

            while (resultSet.next()) {
                valores.put(resultSet.getLong("id"),
                        resultSet.getBigDecimal("quantidade"));
            }
        }

        return valores;
    }

    private int contarFracionadas(Connection connection, String tabela)
            throws Exception {

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT count(*) FROM " + tabela
                                + " WHERE quantidade <> TRUNC(quantidade)")) {

            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int contarChangesets007(Connection connection) throws Exception {

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT count(*) FROM databasechangelog"
                                + " WHERE id LIKE '007-%'"
                                + " AND exectype = 'EXECUTED'")) {

            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private long inserirCategoria(Connection connection, String nome)
            throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO categoria_produto (nome, ativo)"
                        + " VALUES (?, true) RETURNING id")) {

            statement.setString(1, nome);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long inserirProduto(
            Connection connection,
            String nome,
            BigDecimal quantidade,
            long categoriaId,
            String codigoBarras) throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO produto (nome, quantidade, preco,"
                        + " data_cadastro, ativo, categoria_id,"
                        + " unidade_medida, estoque_minimo, codigo_barras)"
                        + " VALUES (?, ?, 10.00, CURRENT_DATE, true, ?,"
                        + " 'UN', 0, ?) RETURNING id")) {

            statement.setString(1, nome);
            statement.setBigDecimal(2, quantidade);
            statement.setLong(3, categoriaId);
            statement.setString(4, codigoBarras);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long inserirCliente(Connection connection, String email)
            throws Exception {

        long pessoaId = inserirPessoa(
                connection, "Cliente rollback", email);

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO cliente (pessoa_id, telefone)"
                        + " VALUES (?, NULL) RETURNING id")) {

            statement.setLong(1, pessoaId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long inserirVendedor(
            Connection connection, String email, String matricula)
            throws Exception {

        long pessoaId = inserirPessoa(
                connection, "Vendedor rollback", email);

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO colaborador (pessoa_id, matricula, funcao,"
                        + " data_admissao)"
                        + " VALUES (?, ?, 'VENDEDOR', CURRENT_DATE)"
                        + " RETURNING id")) {

            statement.setLong(1, pessoaId);
            statement.setString(2, matricula);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long inserirPessoa(
            Connection connection, String nome, String email)
            throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO pessoa (nome, email, cpf, data_cadastro,"
                        + " status)"
                        + " VALUES (?, ?, ?, CURRENT_DATE, 'ATIVO')"
                        + " RETURNING id")) {

            statement.setString(1, nome);
            statement.setString(2, email);
            statement.setString(3, cpfValido());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long inserirVenda(
            Connection connection, long clienteId, long vendedorId)
            throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO venda (cliente_id, vendedor_id, data_venda,"
                        + " status)"
                        + " VALUES (?, ?, CURRENT_TIMESTAMP, 'ABERTA')"
                        + " RETURNING id")) {

            statement.setLong(1, clienteId);
            statement.setLong(2, vendedorId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void inserirItemVenda(
            Connection connection,
            long vendaId,
            long produtoId,
            BigDecimal quantidade,
            BigDecimal valorUnitario) throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO item_venda (venda_id, produto_id, quantidade,"
                        + " valor_unitario) VALUES (?, ?, ?, ?)")) {

            statement.setLong(1, vendaId);
            statement.setLong(2, produtoId);
            statement.setBigDecimal(3, quantidade);
            statement.setBigDecimal(4, valorUnitario);
            statement.executeUpdate();
        }
    }

    private int contarPorNome(
            Connection connection, String tabela, String nome)
            throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM " + tabela + " WHERE nome = ?")) {

            statement.setString(1, nome);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int contarPorEmail(Connection connection, String email)
            throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM pessoa WHERE email = ?")) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int contarPorMatricula(Connection connection, String matricula)
            throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM colaborador WHERE matricula = ?")) {

            statement.setString(1, matricula);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private ChangeSet buscarChangeSet(Database database, String id)
            throws Exception {

        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
        String caminho = "db/changelog/db.changelog-master.yaml";

        ChangeLogParser parser = ChangeLogParserFactory.getInstance()
                .getParser(caminho, resourceAccessor);

        DatabaseChangeLog changeLog = parser.parse(
                caminho, new ChangeLogParameters(database), resourceAccessor);

        return changeLog.getChangeSets().stream()
                .filter(changeSet -> id.equals(changeSet.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "ChangeSet não encontrado: " + id));
    }

    private String tipoColuna(Connection connection, String tabela)
            throws Exception {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT data_type FROM information_schema.columns"
                        + " WHERE table_schema = 'public'"
                        + " AND table_name = ?"
                        + " AND column_name = 'quantidade'")) {

            statement.setString(1, tabela);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "Coluna quantidade não encontrada: " + tabela);
                }

                return resultSet.getString(1);
            }
        }
    }

    @Test
    void deveCancelarVendaPelaApi() throws Exception {
        Long clienteId = criarClientePelaApi("Ana Souza");
        Long vendedorId = criarColaboradorPelaApi(
                "Bruno Lima", FuncaoColaborador.VENDEDOR);

        MvcResult abertura = mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"clienteId":%d,"vendedorId":%d}
                            """.formatted(clienteId, vendedorId)))
                .andExpect(status().isCreated())
                .andReturn();

        Long vendaId = lerId(abertura);

        mockMvc.perform(post("/api/vendas/{id}/cancelamento", vendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void deveRetornar400AoAbrirVendaComColaboradorSemFuncaoDeVenda()
            throws Exception {

        Long clienteId = criarClientePelaApi("Ana Souza");
        Long estoquistaId = criarColaboradorPelaApi(
                "Carlos Dias", FuncaoColaborador.ESTOQUISTA);

        mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"clienteId":%d,"vendedorId":%d}
                            """.formatted(clienteId, estoquistaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Somente colaboradores VENDEDOR ou GERENTE podem realizar vendas"));
    }

    @Test
    void deveListarVendasComFiltroDeStatusPelaApi() throws Exception {
        Long clienteId = criarClientePelaApi("Ana Souza");
        Long vendedorId = criarColaboradorPelaApi(
                "Bruno Lima", FuncaoColaborador.VENDEDOR);

        mockMvc.perform(post("/api/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"clienteId":%d,"vendedorId":%d}
                            """.formatted(clienteId, vendedorId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/vendas")
                        .param("clienteId", clienteId.toString())
                        .param("status", "ABERTA")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[0].clienteNome")
                        .value("Ana Souza"))
                .andExpect(jsonPath("$.conteudo[0].status").value("ABERTA"))
                .andExpect(jsonPath("$.conteudo[0].itens").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Integridade: produto vendido e rollback multi-item
    // ------------------------------------------------------------------

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveRetornar409AoExcluirProdutoVendido() throws Exception {
        CenarioVenda cenario = criarCenarioVenda(10);

        try {
            vendaService.adicionarItem(
                    cenario.vendaId(), cenario.produtoId(), new BigDecimal("1"));

            mockMvc.perform(delete("/api/produtos/{id}", cenario.produtoId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message")
                            .value(containsString("vinculado a outros dados")));
        } finally {
            limparCenario(cenario);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveDesfazerBaixaDeEstoqueQuandoAlgumItemNaoPossuirSaldo() {
        CenarioVenda cenario = criarCenarioVenda(2);
        Long segundoProdutoId = produtoRepository
                .save(novoProduto("49.90", 1)).getId();

        try {
            vendaService.adicionarItem(
                    cenario.vendaId(), cenario.produtoId(), new BigDecimal("2"));
            vendaService.adicionarItem(
                    cenario.vendaId(), segundoProdutoId, new BigDecimal("5"));

            assertThatThrownBy(() -> vendaService.finalizar(cenario.vendaId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Quantidade para retirada maior que o estoque disponível");

            BigDecimal saldoPrimeiro = jdbcTemplate.queryForObject(
                    "SELECT quantidade FROM produto WHERE id = ?",
                    BigDecimal.class,
                    cenario.produtoId());

            BigDecimal saldoSegundo = jdbcTemplate.queryForObject(
                    "SELECT quantidade FROM produto WHERE id = ?",
                    BigDecimal.class,
                    segundoProdutoId);

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM venda WHERE id = ?",
                    String.class,
                    cenario.vendaId());

            assertThat(saldoPrimeiro).isEqualByComparingTo("2.000");
            assertThat(saldoSegundo).isEqualByComparingTo("1.000");            assertThat(status).isEqualTo("ABERTA");
        } finally {
            jdbcTemplate.update(
                    "DELETE FROM item_venda WHERE venda_id = ?",
                    cenario.vendaId());
            limparCenario(cenario);
            jdbcTemplate.update(
                    "DELETE FROM produto WHERE id = ?", segundoProdutoId);
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private record CenarioVenda(
            Long pessoaClienteId,
            Long clienteId,
            Long pessoaVendedorId,
            Long colaboradorId,
            Long categoriaId,
            Long produtoId,
            Long vendaId) {
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

    private Produto novoProduto(String preco, int quantidade) {
        return novoProduto(preco, BigDecimal.valueOf(quantidade));
    }

    private Produto novoProduto(String preco, BigDecimal quantidade) {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria " + sufixo());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();
        produto.setCodigoBarras("AULA10-" + sufixo());
        produto.setNome("Produto Aula 10 " + sufixo());
        produto.setQuantidade(quantidade);
        produto.setEstoqueMinimo(new BigDecimal("1.000"));
        produto.setPreco(new BigDecimal(preco));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        return produto;
    }

    private Long criarClientePelaApi(String nome) throws Exception {
        MvcResult pessoa = mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"%s","email":"%s.%s@example.com","cpf":"%s"}
                            """.formatted(
                                    nome,
                                    nome.toLowerCase().replace(" ", "."),
                                    sufixo(),
                                    cpfValido())))
                .andExpect(status().isCreated())
                .andReturn();

        Long pessoaId = lerId(pessoa);

        MvcResult cliente = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"pessoaId":%d,"telefone":"(11) 99999-0000"}
                            """.formatted(pessoaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pessoa.id").value(pessoaId))
                .andReturn();

        return lerId(cliente);
    }

    private Long criarColaboradorPelaApi(
            String nome, FuncaoColaborador funcao) throws Exception {

        MvcResult pessoa = mockMvc.perform(post("/api/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"%s","email":"%s.%s@example.com","cpf":"%s"}
                            """.formatted(
                                    nome,
                                    nome.toLowerCase().replace(" ", "."),
                                    sufixo(),
                                    cpfValido())))
                .andExpect(status().isCreated())
                .andReturn();

        Long pessoaId = lerId(pessoa);

        MvcResult colaborador = mockMvc.perform(post("/api/colaboradores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"pessoaId":%d,"matricula":"%s","funcao":"%s","dataAdmissao":"2026-09-29"}
                            """.formatted(pessoaId, sufixo(), funcao.name())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pessoa.id").value(pessoaId))
                .andExpect(jsonPath("$.funcao").value(funcao.name()))
                .andReturn();

        return lerId(colaborador);
    }

    private Long lerId(MvcResult resultado) throws Exception {
        Number id = JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private CenarioVenda criarCenarioVenda(int estoqueProduto) {
        Pessoa pessoaCliente = pessoaRepository.save(novaPessoa("Ana Souza"));
        Cliente cliente = clienteRepository.save(
                new Cliente(pessoaCliente, null));

        Pessoa pessoaVendedor = pessoaRepository.save(novaPessoa("Bruno Lima"));
        Colaborador vendedor = colaboradorRepository.save(new Colaborador(
                pessoaVendedor, "VEN-" + sufixo(), FuncaoColaborador.VENDEDOR,
                LocalDate.of(2026, 9, 29)));

        Produto produto = produtoRepository.save(
                novoProduto("89.90", estoqueProduto));

        Venda venda = vendaRepository.save(
                new Venda(cliente, vendedor, LocalDateTime.now()));

        return new CenarioVenda(
                pessoaCliente.getId(),
                cliente.getId(),
                pessoaVendedor.getId(),
                vendedor.getId(),
                produto.getCategoria().getId(),
                produto.getId(),
                venda.getId());
    }

    private void limparCenario(CenarioVenda cenario) {
        jdbcTemplate.update(
                "DELETE FROM item_venda WHERE venda_id = ?", cenario.vendaId());
        jdbcTemplate.update(
                "DELETE FROM venda WHERE id = ?", cenario.vendaId());
        jdbcTemplate.update(
                "DELETE FROM colaborador WHERE id = ?", cenario.colaboradorId());
        jdbcTemplate.update(
                "DELETE FROM cliente WHERE id = ?", cenario.clienteId());
        jdbcTemplate.update(
                "DELETE FROM pessoa WHERE id = ?", cenario.pessoaClienteId());
        jdbcTemplate.update(
                "DELETE FROM pessoa WHERE id = ?", cenario.pessoaVendedorId());
        jdbcTemplate.update(
                "DELETE FROM produto WHERE id = ?", cenario.produtoId());
        jdbcTemplate.update(
                "DELETE FROM categoria_produto WHERE id = ?",
                cenario.categoriaId());
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

    private String cnpjValido() {
        long semente = Math.abs(System.nanoTime() % 1_000_000L);
        String base = String.format("11%06d0001", semente);
        char primeiro = base.charAt(0);

        if (base.chars().allMatch(digito -> digito == primeiro)) {
            base = "1" + base.substring(1);
        }

        String treze = base + digitoCnpj(base, new int[] {
                5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 });

        return treze + digitoCnpj(treze, new int[] {
                6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 });
    }

    private int digitoCnpj(String base, int[] pesos) {
        int soma = 0;

        for (int indice = 0; indice < pesos.length; indice++) {
            soma += Character.getNumericValue(base.charAt(indice))
                    * pesos[indice];
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
