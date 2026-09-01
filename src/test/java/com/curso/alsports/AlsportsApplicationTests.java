package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Produto;
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.ProdutoRepository;
import com.curso.alsports.service.ProdutoService;

@SpringBootTest
@ActiveProfiles("test")
class AlsportsApplicationTests {

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private CategoriaProdutoRepository categoriaProdutoRepository;

	@Autowired
	private ProdutoService produtoService;

	@Test
	void deveSalvarProduto() {
		CategoriaProduto categoria = new CategoriaProduto();
		categoria.setNome("Bolas");
		categoria.setAtivo(true);

		categoria = categoriaProdutoRepository.save(categoria);

		Produto produto = new Produto();
		produto.setNome("Bola de Teste Automatizado " + System.currentTimeMillis());
		produto.setQuantidade(10);
		produto.setPreco(new BigDecimal("99.90"));
		produto.setDataCadastro(LocalDate.now());
		produto.setAtivo(true);
		produto.setUnidadeMedida("UN");
		produto.setCategoria(categoria);

		Produto produtoSalvo = produtoRepository.save(produto);

		assertThat(produtoSalvo.getId()).isNotNull();
		assertThat(produtoSalvo.getNome()).startsWith("Bola de Teste Automatizado");
		assertThat(produtoSalvo.getCategoria().getId()).isEqualTo(categoria.getId());

		assertThat(produtoSalvo.getQuantidade()).isEqualTo(10);
		assertThat(produtoSalvo.getPreco()).isEqualByComparingTo("99.90");
		assertThat(produtoSalvo.getUnidadeMedida()).isEqualTo("UN");
		assertThat(produtoSalvo.getAtivo()).isTrue();
	}

	@Test
	void deveBuscarProdutoPorNome() {
		CategoriaProduto categoria = new CategoriaProduto();
		categoria.setNome("Tênis");
		categoria.setAtivo(true);

		categoria = categoriaProdutoRepository.save(categoria);

		Produto produto = new Produto();
		produto.setNome("Tênis de Teste " + System.currentTimeMillis());
		produto.setQuantidade(5);
		produto.setPreco(new BigDecimal("199.90"));
		produto.setDataCadastro(LocalDate.now());
		produto.setAtivo(true);
		produto.setUnidadeMedida("UN");
		produto.setCategoria(categoria);

		produtoService.salvar(produto);

		String nomeBusca = produto.getNome();

		Produto encontrado = produtoService.buscarPorNome(nomeBusca);

		assertThat(encontrado).isNotNull();
		assertThat(encontrado.getNome()).isEqualTo(nomeBusca);
	}

	@Test
	void deveAtualizarProdutoComDirtyChecking() {
		CategoriaProduto categoria = new CategoriaProduto();
		categoria.setNome("Acessórios " + System.currentTimeMillis());
		categoria.setAtivo(true);

		categoria = categoriaProdutoRepository.save(categoria);

		Produto produto = new Produto();
		produto.setNome("Produto Dirty Checking " + System.currentTimeMillis());
		produto.setQuantidade(10);
		produto.setPreco(new BigDecimal("50.00"));
		produto.setDataCadastro(LocalDate.now());
		produto.setAtivo(true);
		produto.setUnidadeMedida("UN");
		produto.setCategoria(categoria);

		produto = produtoService.salvar(produto);

		Long id = produto.getId();

		Produto produtoAtualizado = new Produto();
		produtoAtualizado.setNome("Produto Atualizado " + System.currentTimeMillis());
		produtoAtualizado.setQuantidade(20);
		produtoAtualizado.setPreco(new BigDecimal("75.00"));
		produtoAtualizado.setDataCadastro(LocalDate.now());
		produtoAtualizado.setAtivo(true);
		produtoAtualizado.setUnidadeMedida("UN");
		produtoAtualizado.setCategoria(categoria);

		produtoService.atualizar(id, produtoAtualizado);

		Produto produtoConsultado = produtoRepository.findById(id).orElseThrow();

		assertThat(produtoConsultado.getNome()).startsWith("Produto Atualizado");
		assertThat(produtoConsultado.getQuantidade()).isEqualTo(20);
		assertThat(produtoConsultado.getPreco()).isEqualByComparingTo("75.00");
	}

	@Test
	void deveFazerRollbackQuandoOcorrerErro() {
		CategoriaProduto categoria = new CategoriaProduto();
		categoria.setNome("Rollback " + System.currentTimeMillis());
		categoria.setAtivo(true);

		categoria = categoriaProdutoRepository.save(categoria);

		String nomeProduto = "Produto Rollback " + System.currentTimeMillis();

		Produto produto = new Produto();
		produto.setNome(nomeProduto);
		produto.setQuantidade(10);
		produto.setPreco(new BigDecimal("100.00"));
		produto.setDataCadastro(LocalDate.now());
		produto.setAtivo(true);
		produto.setUnidadeMedida("UN");
		produto.setCategoria(categoria);

		org.assertj.core.api.Assertions.assertThatThrownBy(
						() -> produtoService.salvarEFalhar(produto))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Erro proposital para testar rollback");

		assertThat(produtoRepository.findByNomeIgnoreCase(nomeProduto))
				.isEmpty();
	}
}