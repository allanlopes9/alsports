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

@SpringBootTest
@ActiveProfiles("test")
class AlsportsApplicationTests {

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private CategoriaProdutoRepository categoriaProdutoRepository;

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
}