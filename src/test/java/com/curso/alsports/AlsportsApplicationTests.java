package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Produto;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.ProdutoRepository;
import com.curso.alsports.service.ProdutoService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class AlsportsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaProdutoRepository categoriaProdutoRepository;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAlterarStatusProdutoPelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Status " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();
        produto.setCodigoBarras("STATUS-TESTE-" + System.currentTimeMillis());
        produto.setNome("Produto Teste Status");
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("99.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        Produto salvo = produtoService.salvar(produto);

        mockMvc.perform(put("/api/produtos/" + salvo.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INATIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salvo.getId()))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRejeitarStatusInvalidoPelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Status Invalido " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();
        produto.setCodigoBarras("STATUS-INVALIDO-" + System.currentTimeMillis());
        produto.setNome("Produto Status Invalido");
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("99.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        Produto salvo = produtoService.salvar(produto);

        mockMvc.perform(put("/api/produtos/" + salvo.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INVALIDO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void deveRetornar404AoAlterarStatusDeProdutoInexistente() throws Exception {

        mockMvc.perform(put("/api/produtos/999999999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INATIVO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/produtos/999999999/status"));
    }

    @Test
    void deveSalvarProduto() {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Teste");
        categoria.setAtivo(true);

        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();

        produto.setCodigoBarras("TESTE-SALVAR-" + System.currentTimeMillis());
        produto.setNome("Produto Teste");
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("99.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        Produto salvo = produtoService.salvar(produto);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getNome()).isEqualTo("Produto Teste");
        assertThat(salvo.getQuantidade()).isEqualByComparingTo("10.000");
        assertThat(salvo.getCodigoBarras()).isEqualTo(produto.getCodigoBarras());
    }

    @Test
    void deveBuscarProdutoPorNome() {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Busca");
        categoria.setAtivo(true);

        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();

        produto.setCodigoBarras("TESTE-BUSCA-" + System.currentTimeMillis());
        produto.setNome("Produto Busca");
        produto.setQuantidade(new BigDecimal("5.000"));
        produto.setEstoqueMinimo(new BigDecimal("1.000"));
        produto.setPreco(new BigDecimal("49.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        produtoService.salvar(produto);

        Produto encontrado = produtoService.buscarPorNome("Produto Busca");

        assertThat(encontrado).isNotNull();
        assertThat(encontrado.getNome()).isEqualTo("Produto Busca");
    }

    @Test
    void deveAtualizarProdutoComDirtyChecking() {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Dirty Checking");
        categoria.setAtivo(true);

        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();

        produto.setCodigoBarras("TESTE-UPDATE-" + System.currentTimeMillis());
        produto.setNome("Produto Original");
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("100.00"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        Produto salvo = produtoService.salvar(produto);

        salvo.setNome("Produto Atualizado");
        salvo.setQuantidade(new BigDecimal("20.000"));

        Produto encontrado = produtoService.buscarPorId(salvo.getId());

        assertThat(encontrado.getNome()).isEqualTo("Produto Atualizado");
        assertThat(encontrado.getQuantidade()).isEqualByComparingTo("20.000");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveFazerRollbackQuandoOcorrerErro() {

        String nomeProduto = "Produto Rollback";

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Rollback " + System.currentTimeMillis());
        categoria.setAtivo(true);

        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();

        produto.setCodigoBarras("TESTE-ROLLBACK-" + System.currentTimeMillis());
        produto.setNome(nomeProduto);
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("100.00"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        assertThatThrownBy(
                () -> produtoService.salvarEFalhar(produto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro proposital para testar rollback");

        entityManager.clear();

        assertThat(produtoRepository.findByNomeIgnoreCase(nomeProduto))
                .isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deveFazerRollbackQuandoCategoriaNaoExistir() {

        String nomeProduto = "Produto Categoria Inexistente";

        Produto produto = new Produto();

        produto.setCodigoBarras("TESTE-CATEGORIA-" + System.currentTimeMillis());
        produto.setNome(nomeProduto);
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("100.00"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");

        assertThatThrownBy(
                () -> produtoService.cadastrar(produto, 999999999L, null))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Categoria não encontrada: 999999999");

        entityManager.clear();

        assertThat(produtoRepository.findByNomeIgnoreCase(nomeProduto))
                .isEmpty();
    }

    @Test
    void deveCriarProdutoPelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Futebol " + System.currentTimeMillis());
        categoria.setAtivo(true);

        categoria = categoriaProdutoRepository.save(categoria);

        String codigoBarras = "API-CRIAR-" + System.currentTimeMillis();

        String json = """
                {
                    "codigoBarras": "%s",
                    "nome": "Bola API Teste %d",
                    "quantidade": 10,
                    "estoqueMinimo": 0.000,
                    "preco": 99.90,
                    "dataCadastro": "2026-09-04",
                    "ativo": true,
                    "unidadeMedida": "UN",
                    "categoriaId": %d
                }
                """.formatted(
                codigoBarras,
                System.currentTimeMillis(),
                categoria.getId());

        mockMvc.perform(post("/api/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        containsString("/api/produtos/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigoBarras").value(codigoBarras))
                .andExpect(jsonPath("$.nome")
                        .value(containsString("Bola API Teste")))
                .andExpect(jsonPath("$.quantidade").value(closeTo(10.0, 0.0001)))
                .andExpect(jsonPath("$.preco").value(99.90))
                .andExpect(jsonPath("$.unidadeMedida").value("UN"))
                .andExpect(jsonPath("$.categoria.id")
                        .value(categoria.getId()));
    }

    @Test
    void deveRetornar400QuandoProdutoForInvalido() throws Exception {

        String json = """
                {
                    "codigoBarras": "",
                    "nome": "",
                    "quantidade": -5,
                    "preco": -10.00,
                    "dataCadastro": "2026-09-04",
                    "ativo": true,
                    "unidadeMedida": "",
                    "categoriaId": null
                }
                """;

        mockMvc.perform(post("/api/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.fields.codigoBarras").exists())
                .andExpect(jsonPath("$.fields.nome").exists())
                .andExpect(jsonPath("$.fields.quantidade").exists())
                .andExpect(jsonPath("$.fields.estoqueMinimo").exists())
                .andExpect(jsonPath("$.fields.preco").exists())
                .andExpect(jsonPath("$.fields.unidadeMedida").exists())
                .andExpect(jsonPath("$.fields.categoriaId").exists());
    }

    @Test
    void deveRetornar404QuandoProdutoNaoExistir() throws Exception {

        mockMvc.perform(get("/api/produtos/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/produtos/999999999"));
    }

    @Test
    void deveBuscarProdutosPorCategoria() {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Relacionamento");
        categoria.setAtivo(true);

        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();

        produto.setCodigoBarras("TESTE-CATEGORIA-BUSCA-" + System.currentTimeMillis());
        produto.setNome("Produto Relacionamento");
        produto.setQuantidade(new BigDecimal("8.000"));
        produto.setEstoqueMinimo(new BigDecimal("1.000"));
        produto.setPreco(new BigDecimal("79.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        produtoService.salvar(produto);

        List<Produto> encontrados =
                produtoRepository.findByCategoriaId(categoria.getId());

        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.get(0).getNome())
                .isEqualTo("Produto Relacionamento");
    }

    @Test
    void deveBuscarProdutoExistentePelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria GET " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();
        produto.setCodigoBarras("API-GET-" + System.currentTimeMillis());
        produto.setNome("Produto GET API " + System.currentTimeMillis());
        produto.setQuantidade(new BigDecimal("15.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("129.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        produto = produtoService.salvar(produto);

        mockMvc.perform(get("/api/produtos/" + produto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.codigoBarras")
                        .value(produto.getCodigoBarras()))
                .andExpect(jsonPath("$.nome").value(produto.getNome()))
                .andExpect(jsonPath("$.quantidade").value(closeTo(15.0, 0.0001)))
                .andExpect(jsonPath("$.unidadeMedida").value("UN"));
    }

    @Test
    void deveListarProdutosPelaApi() throws Exception {

        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalElementos").isNumber())
                .andExpect(jsonPath("$.totalPaginas").isNumber());
    }

    @Test
    void deveRetornar409QuandoCodigoBarrasForDuplicado() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Duplicado " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        String codigoBarras = "DUPLICADO-" + System.currentTimeMillis();

        String json = """
                {
                    "codigoBarras": "%s",
                    "nome": "Produto Duplicado API %d",
                    "quantidade": 10,
                    "estoqueMinimo": 0.000,
                    "preco": 99.90,
                    "dataCadastro": "2026-09-07",
                    "ativo": true,
                    "unidadeMedida": "UN",
                    "categoriaId": %d
                }
                """.formatted(
                codigoBarras,
                System.currentTimeMillis(),
                categoria.getId());

        mockMvc.perform(post("/api/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"));
    }

    @Test
    void deveExcluirCategoriaSemProdutosPelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Exclusao " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        mockMvc.perform(delete("/api/categorias/" + categoria.getId()))
                .andExpect(status().isNoContent());

        assertThat(categoriaProdutoRepository.existsById(categoria.getId()))
                .isFalse();
    }

    @Test
    void deveRetornar409AoExcluirCategoriaComProdutosPelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Com Produto " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();
        produto.setCodigoBarras("CAT-EXCLUSAO-" + System.currentTimeMillis());
        produto.setNome("Produto Vinculado Categoria");
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("99.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);
        produtoRepository.save(produto);

        mockMvc.perform(delete("/api/categorias/" + categoria.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));

        assertThat(categoriaProdutoRepository.existsById(categoria.getId()))
                .isTrue();
    }

    @Test
    void deveRetornar400QuandoJsonForMalformado() throws Exception {

        String jsonInvalido = """
                {
                    "codigoBarras": "JSON-INVALIDO-001",
                    "nome": "Produto JSON Inválido",
                    "quantidade": 10,
                    "preco": 99.90,
                    "dataCadastro": "2026-09-07",
                    "ativo": true,
                    "unidadeMedida": "UN",
                    "categoriaId": 1
                """;

        mockMvc.perform(post("/api/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"));
    }

    private Produto criarProdutoParaMovimentacaoEstoque() {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Estoque " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();
        produto.setCodigoBarras("ESTOQUE-" + System.currentTimeMillis());
        produto.setNome("Produto Estoque " + System.currentTimeMillis());
        produto.setQuantidade(new BigDecimal("10.000"));
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("99.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        return produtoService.salvar(produto);
    }

    @Test
    void deveReceberEstoquePelaApi() throws Exception {
        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.quantidade").value(closeTo(15.0, 0.0001)));
    }

    @Test
    void deveReceberEstoqueFracionadoPelaApi() throws Exception {
        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":0.500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.quantidade").value(closeTo(10.5, 0.0001)));
    }

    @Test
    void deveRetirarEstoqueFracionadoPelaApi() throws Exception {
        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/saidas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":1.250}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.quantidade").value(closeTo(8.75, 0.0001)));
    }

    @Test
    void deveRejeitarMovimentacaoComMaisDeTresCasasDecimais() throws Exception {
        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":0.0001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.quantidade").exists());
    }

    @Test
    void deveRejeitarProdutoComQuantidadeAcimaDaPrecisao() throws Exception {
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Precisao " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        String json = """
                {
                    "codigoBarras": "PRECISAO-%d",
                    "nome": "Produto Precisao %d",
                    "quantidade": 1000000000000000.000,
                    "estoqueMinimo": 0.000,
                    "preco": 99.90,
                    "dataCadastro": "2026-09-04",
                    "ativo": true,
                    "unidadeMedida": "UN",
                    "categoriaId": %d
                }
                """.formatted(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                categoria.getId());

        mockMvc.perform(post("/api/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.quantidade").exists());
    }

    @Test
    void deveRetirarEstoquePelaApi() throws Exception {
        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/saidas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.quantidade").value(closeTo(6.0, 0.0001)));
    }

    @Test
    void deveRejeitarQuantidadeInvalidaNaMovimentacao() throws Exception {
        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/saidas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":-2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deveRejeitarSaidaMaiorQueEstoqueDisponivel() throws Exception {
        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(post("/api/produtos/" + produto.getId() + "/estoque/saidas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        entityManager.clear();

        assertThat(produtoRepository.findById(produto.getId()))
                .isPresent()
                .get()
                .extracting(Produto::getQuantidade)
                .isEqualTo(new BigDecimal("10.000"));
    }

    @Test
    void deveRetornar404AoMovimentarEstoqueDeProdutoInexistente() throws Exception {
        mockMvc.perform(post("/api/produtos/999999999/estoque/entradas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
    @Test
    void deveNormalizarPaginaNegativaParaPrimeiraPagina() throws Exception {

        mockMvc.perform(get("/api/produtos").param("page", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0));
    }

    @Test
    void deveLimitarTamanhoExcessivoAoMaximoPermitido() throws Exception {

        mockMvc.perform(get("/api/produtos").param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(100));
    }

    @Test
    void deveRejeitarCampoDeOrdenacaoNaoPermitido() throws Exception {

        mockMvc.perform(get("/api/produtos").param("sort", "campoInexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deveRespeitarTamanhoPersonalizadoNaPaginacao() throws Exception {

        mockMvc.perform(get("/api/produtos")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.conteudo.length()").value(
                        org.hamcrest.Matchers.lessThanOrEqualTo(1)))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(1));
    }

    @Test
    void deveCombinarFiltrosNaListagemDeProdutos() throws Exception {
        String identificador = String.valueOf(System.currentTimeMillis());

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Filtros " + identificador);
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        Produto produtoEncontrado = new Produto();
        produtoEncontrado.setCodigoBarras("FILTRO-OK-" + identificador);
        produtoEncontrado.setNome("Filtro Bola " + identificador);
        produtoEncontrado.setQuantidade(new BigDecimal("5.000"));
        produtoEncontrado.setEstoqueMinimo(new BigDecimal("1.000"));
        produtoEncontrado.setPreco(new BigDecimal("49.90"));
        produtoEncontrado.setDataCadastro(LocalDate.now());
        produtoEncontrado.setAtivo(true);
        produtoEncontrado.setUnidadeMedida("UN");
        produtoEncontrado.setCategoria(categoria);
        produtoEncontrado = produtoService.salvar(produtoEncontrado);

        Produto produtoForaDaFaixa = new Produto();
        produtoForaDaFaixa.setCodigoBarras("FILTRO-FORA-" + identificador);
        produtoForaDaFaixa.setNome("Filtro Chuteira " + identificador);
        produtoForaDaFaixa.setQuantidade(new BigDecimal("20.000"));
        produtoForaDaFaixa.setEstoqueMinimo(new BigDecimal("1.000"));
        produtoForaDaFaixa.setPreco(new BigDecimal("99.90"));
        produtoForaDaFaixa.setDataCadastro(LocalDate.now());
        produtoForaDaFaixa.setAtivo(true);
        produtoForaDaFaixa.setUnidadeMedida("UN");
        produtoForaDaFaixa.setCategoria(categoria);
        produtoService.salvar(produtoForaDaFaixa);

        mockMvc.perform(get("/api/produtos")
                .param("nome", "filtro bola " + identificador)
                .param("ativo", "true")
                .param("quantidadeMinima", "1")
                .param("quantidadeMaxima", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(
                        produtoEncontrado.getId()))
                .andExpect(jsonPath("$.conteudo[0].ativo").value(true))
                .andExpect(jsonPath("$.conteudo[0].quantidade").value(closeTo(5.0, 0.0001)));
    }

    @Test
    void deveAlterarProdutoPelaApi() throws Exception {

        Produto produto = criarProdutoParaMovimentacaoEstoque();

        String json = """
                {
                    "nome": "Produto Alterado %d",
                    "estoqueMinimo": 4.000,
                    "preco": 59.90,
                    "categoriaId": %d,
                    "fornecedorId": null
                }
                """.formatted(
                System.currentTimeMillis(),
                produto.getCategoria().getId());

        mockMvc.perform(put("/api/produtos/" + produto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.nome")
                        .value(containsString("Produto Alterado")))
                .andExpect(jsonPath("$.preco").value(59.90))
                .andExpect(jsonPath("$.quantidade").value(closeTo(10.0, 0.0001)));
    }

    @Test
    void deveRetornar404AoAlterarProdutoInexistente() throws Exception {

        String json = """
                {
                    "nome": "Produto Inexistente",
                    "estoqueMinimo": 4.000,
                    "preco": 59.90,
                    "categoriaId": 1,
                    "fornecedorId": null
                }
                """;

        mockMvc.perform(put("/api/produtos/999999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveRetornar409AoAlterarProdutoComNomeDuplicado() throws Exception {

        Produto primeiro = criarProdutoParaMovimentacaoEstoque();
        Produto segundo = criarProdutoParaMovimentacaoEstoque();

        String json = """
                {
                    "nome": "%s",
                    "estoqueMinimo": 2.000,
                    "preco": 99.90,
                    "categoriaId": %d,
                    "fornecedorId": null
                }
                """.formatted(
                segundo.getNome(),
                primeiro.getCategoria().getId());

        mockMvc.perform(put("/api/produtos/" + primeiro.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deveExcluirProdutoPelaApi() throws Exception {

        Produto produto = criarProdutoParaMovimentacaoEstoque();

        mockMvc.perform(delete("/api/produtos/" + produto.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/produtos/" + produto.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAlterarCategoriaPelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Alterar " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        String json = """
                {
                    "nome": "Categoria Alterada %d"
                }
                """.formatted(System.currentTimeMillis());

        mockMvc.perform(put("/api/categorias/" + categoria.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoria.getId()))
                .andExpect(jsonPath("$.nome")
                        .value(containsString("Categoria Alterada")));
    }

    @Test
    void deveAlterarStatusCategoriaPelaApi() throws Exception {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Status " + System.currentTimeMillis());
        categoria.setAtivo(true);
        categoria = categoriaProdutoRepository.save(categoria);

        mockMvc.perform(put("/api/categorias/" + categoria.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INATIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoria.getId()))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRetornar409AoAlterarCategoriaComNomeDuplicado() throws Exception {

        String identificador = String.valueOf(System.currentTimeMillis());

        CategoriaProduto primeira = new CategoriaProduto();
        primeira.setNome("Categoria Primeira " + identificador);
        primeira.setAtivo(true);
        primeira = categoriaProdutoRepository.save(primeira);

        CategoriaProduto segunda = new CategoriaProduto();
        segunda.setNome("Categoria Segunda " + identificador);
        segunda.setAtivo(true);
        categoriaProdutoRepository.save(segunda);

        String json = """
                {
                    "nome": "%s"
                }
                """.formatted(segunda.getNome());

        mockMvc.perform(put("/api/categorias/" + primeira.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void devePesquisarCategoriasComPaginacaoEFiltros() throws Exception {

        String identificador = String.valueOf(System.currentTimeMillis());

        CategoriaProduto encontrada = new CategoriaProduto();
        encontrada.setNome("PesquisaFiltro" + identificador);
        encontrada.setAtivo(true);
        encontrada = categoriaProdutoRepository.save(encontrada);

        CategoriaProduto foraDoFiltro = new CategoriaProduto();
        foraDoFiltro.setNome("OutroFiltro" + identificador);
        foraDoFiltro.setAtivo(true);
        categoriaProdutoRepository.save(foraDoFiltro);

        mockMvc.perform(get("/api/categorias")
                .param("nome", "pesquisafiltro" + identificador)
                .param("ativo", "true")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id")
                        .value(encontrada.getId()))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primeira").value(true))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void deveRejeitarOrdenacaoInvalidaNaListagemDeCategorias() throws Exception {

        mockMvc.perform(get("/api/categorias")
                .param("sort", "campoInexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void bancoDeveImpedirNomeDeCategoriaDuplicadoIgnorandoMaiusculas() {

        String identificador = String.valueOf(System.currentTimeMillis());

        jdbcTemplate.update(
                "INSERT INTO categoria_produto (nome, ativo) VALUES (?, ?)",
                "Papelaria " + identificador,
                true);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO categoria_produto (nome, ativo) VALUES (?, ?)",
                "PAPELARIA " + identificador,
                true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
