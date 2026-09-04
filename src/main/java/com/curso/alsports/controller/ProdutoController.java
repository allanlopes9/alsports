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
import com.curso.alsports.repository.CategoriaProdutoRepository;
import com.curso.alsports.repository.FornecedorRepository;

import com.curso.alsports.model.Produto;
import com.curso.alsports.service.ProdutoService;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    private final CategoriaProdutoRepository categoriaProdutoRepository;
    private final FornecedorRepository fornecedorRepository;

    public ProdutoController(
            ProdutoService service,
            CategoriaProdutoRepository categoriaProdutoRepository,
            FornecedorRepository fornecedorRepository) {

        this.service = service;
        this.categoriaProdutoRepository = categoriaProdutoRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> salvar(
            @Valid @RequestBody ProdutoRequest request) {

        CategoriaProduto categoria = categoriaProdutoRepository
                .findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException(
                        "Categoria não encontrada: " + request.getCategoriaId()));

        Fornecedor fornecedor = null;

        if (request.getFornecedorId() != null) {
            fornecedor = fornecedorRepository
                    .findById(request.getFornecedorId())
                    .orElseThrow(() -> new RuntimeException(
                            "Fornecedor não encontrado: " + request.getFornecedorId()));
        }

        Produto produto = ProdutoMapper.toEntity(
                request,
                categoria,
                fornecedor);

        Produto produtoSalvo = service.salvar(produto);

        return ResponseEntity.ok(
                ProdutoMapper.toResponse(produtoSalvo));
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listar() {
        return ResponseEntity.ok(
                service.listar()
                        .stream()
                        .map(ProdutoMapper::toResponse)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(
            @PathVariable Long id) {

        Produto produto = service.buscarPorId(id);

        if (produto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                ProdutoMapper.toResponse(produto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoRequest request) {

        CategoriaProduto categoria = categoriaProdutoRepository
                .findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException(
                        "Categoria não encontrada: " + request.getCategoriaId()));

        Fornecedor fornecedor = null;

        if (request.getFornecedorId() != null) {
            fornecedor = fornecedorRepository
                    .findById(request.getFornecedorId())
                    .orElseThrow(() -> new RuntimeException(
                            "Fornecedor não encontrado: " + request.getFornecedorId()));
        }

        Produto produto = ProdutoMapper.toEntity(
                request,
                categoria,
                fornecedor);

        Produto produtoAtualizado = service.atualizar(id, produto);

        if (produtoAtualizado == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                ProdutoMapper.toResponse(produtoAtualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        boolean excluido = service.excluir(id);

        if (!excluido) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}