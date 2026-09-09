package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
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
@AutoConfigureMockMvc
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

    @Test
    void deveSalvarProduto() {

        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome("Categoria Teste");
        categoria.setAtivo(true);

        categoria = categoriaProdutoRepository.save(categoria);

        Produto produto = new Produto();

        produto.setCodigoBarras("TESTE-SALVAR-" + System.currentTimeMillis());
        produto.setNome("Produto Teste");
        produto.setQuantidade(10);
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("99.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        Produto salvo = produtoService.salvar(produto);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getNome()).isEqualTo("Produto Teste");
        assertThat(salvo.getQuantidade()).isEqualTo(10);
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
        produto.setQuantidade(5);
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
        produto.setQuantidade(10);
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("100.00"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        Produto salvo = produtoService.salvar(produto);

        salvo.setNome("Produto Atualizado");
        salvo.setQuantidade(20);

        Produto encontrado = produtoService.buscarPorId(salvo.getId());

        assertThat(encontrado.getNome()).isEqualTo("Produto Atualizado");
        assertThat(encontrado.getQuantidade()).isEqualTo(20);
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
        produto.setQuantidade(10);
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
        produto.setQuantidade(10);
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

        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        containsString("/produtos/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigoBarras").value(codigoBarras))
                .andExpect(jsonPath("$.nome")
                        .value(containsString("Bola API Teste")))
                .andExpect(jsonPath("$.quantidade").value(10))
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

        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro")
                        .value("Erro de validação"))
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

        mockMvc.perform(get("/produtos/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro")
                        .value("Recurso não encontrado"))
                .andExpect(jsonPath("$.path")
                        .value("/produtos/999999999"));
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
        produto.setQuantidade(8);
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
        produto.setQuantidade(15);
        produto.setEstoqueMinimo(new BigDecimal("2.000"));
        produto.setPreco(new BigDecimal("129.90"));
        produto.setDataCadastro(LocalDate.now());
        produto.setAtivo(true);
        produto.setUnidadeMedida("UN");
        produto.setCategoria(categoria);

        produto = produtoService.salvar(produto);

        mockMvc.perform(get("/produtos/" + produto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.codigoBarras")
                        .value(produto.getCodigoBarras()))
                .andExpect(jsonPath("$.nome").value(produto.getNome()))
                .andExpect(jsonPath("$.quantidade").value(15))
                .andExpect(jsonPath("$.unidadeMedida").value("UN"));
    }

    @Test
    void deveListarProdutosPelaApi() throws Exception {

        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
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

        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro")
                        .value("Recurso duplicado"));
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

        mockMvc.perform(post("/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro")
                        .value("JSON inválido"));
    }
}
