package com.curso.alsports.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.curso.alsports.dto.ProdutoMapper;
import com.curso.alsports.dto.ProdutoRequest;
import com.curso.alsports.dto.ProdutoResponse;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.model.Fornecedor;

import com.curso.alsports.model.Produto;
import com.curso.alsports.service.ProdutoService;

@RestController
@RequestMapping("/produtos")
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

        CategoriaProduto categoria = service.buscarCategoriaPorId(
                request.getCategoriaId());

        Fornecedor fornecedor = null;

        if (request.getFornecedorId() != null) {
            fornecedor = service.buscarFornecedorPorId(
                    request.getFornecedorId());
        }

        Produto produto = produtoMapper.toEntity(
                request,
                categoria,
                fornecedor);

        Produto produtoSalvo = service.salvar(produto);

        return ResponseEntity
                .created(java.net.URI.create("/produtos/" + produtoSalvo.getId()))
                .body(produtoMapper.toResponse(produtoSalvo));
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listar() {
        return ResponseEntity.ok(
                service.listar()
                        .stream()
                        .map(produtoMapper::toResponse)
                        .toList());
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
            @Valid @RequestBody ProdutoRequest request) {

        CategoriaProduto categoria = service.buscarCategoriaPorId(
                request.getCategoriaId());

        Fornecedor fornecedor = null;

        if (request.getFornecedorId() != null) {
            fornecedor = service.buscarFornecedorPorId(
                    request.getFornecedorId());
        }

        Produto produto = produtoMapper.toEntity(
                request,
                categoria,
                fornecedor);

        Produto produtoAtualizado = service.atualizar(id, produto);

        return ResponseEntity.ok(
                produtoMapper.toResponse(produtoAtualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        service.excluir(id);

        return ResponseEntity.noContent().build();
    }
}