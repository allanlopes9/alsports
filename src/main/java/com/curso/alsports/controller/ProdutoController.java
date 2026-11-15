package com.curso.alsports.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.curso.alsports.dto.MovimentacaoEstoqueRequest;
import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.dto.ProdutoMapper;
import com.curso.alsports.dto.ProdutoRequest;
import com.curso.alsports.dto.ProdutoResponse;
import com.curso.alsports.dto.ProdutoUpdateRequest;
import com.curso.alsports.dto.StatusRequest;
import com.curso.alsports.model.Produto;
import com.curso.alsports.service.ProdutoService;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final ProdutoMapper produtoMapper;

    public ProdutoController(
        ProdutoService service,
        ProdutoMapper produtoMapper) {

        this.service = service;
        this.produtoMapper = produtoMapper;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> salvar(
        @Valid @RequestBody ProdutoRequest request) {

        Produto produto = produtoMapper.toEntity(request);

        Produto produtoSalvo = service.cadastrar(
            produto,
            request.getCategoriaId(),
            request.getFornecedorId());

        return ResponseEntity
            .created(URI.create("/api/produtos/" + produtoSalvo.getId()))
            .body(produtoMapper.toResponse(produtoSalvo));
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<ProdutoResponse>> listar(

        @RequestParam(required = false) String nome,

        @RequestParam(required = false) Boolean ativo,

        @RequestParam(required = false) Long categoriaId,

        @RequestParam(required = false) Long fornecedorId,

        @RequestParam(required = false) Integer quantidadeMinima,

        @RequestParam(required = false) Integer quantidadeMaxima,

        @RequestParam(required = false) Boolean abaixoEstoqueMinimo,

        @PageableDefault(
            size = 20,
            sort = "id",
            direction = Sort.Direction.ASC)
        Pageable pageable) {

        if (quantidadeMinima != null && quantidadeMinima < 0) {
            throw new IllegalArgumentException(
                "A quantidade mínima não pode ser negativa");
        }

        if (quantidadeMaxima != null && quantidadeMaxima < 0) {
            throw new IllegalArgumentException(
                "A quantidade máxima não pode ser negativa");
        }

        if (quantidadeMinima != null
            && quantidadeMaxima != null
            && quantidadeMinima > quantidadeMaxima) {

            throw new IllegalArgumentException(
                "A quantidade mínima não pode ser maior que a máxima");
        }

        return ResponseEntity.ok(
            PaginaResponse.de(
                service.pesquisar(
                    nome,
                    ativo,
                    categoriaId,
                    fornecedorId,
                    quantidadeMinima,
                    quantidadeMaxima,
                    abaixoEstoqueMinimo,
                    pageable),
                produtoMapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(
        @PathVariable Long id) {

        Produto produto = service.buscarPorId(id);

        return ResponseEntity.ok(
            produtoMapper.toResponse(produto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(
        @PathVariable Long id,
        @Valid @RequestBody ProdutoUpdateRequest request) {

        Produto produtoAtualizado = service.atualizar(
            id,
            request.getNome(),
            request.getEstoqueMinimo(),
            request.getPreco(),
            request.getCategoriaId(),
            request.getFornecedorId());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @PostMapping("/{id}/estoque/entradas")
    public ResponseEntity<ProdutoResponse> receberEstoque(
        @PathVariable Long id,
        @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        Produto produtoAtualizado = service.receberEstoque(
            id, request.quantidade());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @PostMapping("/{id}/estoque/saidas")
    public ResponseEntity<ProdutoResponse> retirarEstoque(
        @PathVariable Long id,
        @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        Produto produtoAtualizado = service.retirarEstoque(
            id, request.quantidade());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProdutoResponse> alterarStatus(
        @PathVariable Long id,
        @Valid @RequestBody StatusRequest request) {

        Produto produtoAtualizado = service.alterarStatus(
            id, request.status());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        service.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
