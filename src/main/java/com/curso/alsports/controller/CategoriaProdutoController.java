package com.curso.alsports.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.curso.alsports.dto.CategoriaProdutoMapper;
import com.curso.alsports.dto.CategoriaProdutoRequest;
import com.curso.alsports.dto.CategoriaProdutoResponse;

import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.service.CategoriaProdutoService;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/categorias")
public class CategoriaProdutoController {

    private final CategoriaProdutoService service;

    public CategoriaProdutoController(CategoriaProdutoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CategoriaProdutoResponse> salvar(
            @Valid @RequestBody CategoriaProdutoRequest request) {

        CategoriaProduto categoria = CategoriaProdutoMapper.toEntity(request);

        CategoriaProduto categoriaSalva = service.salvar(categoria);

        return ResponseEntity.ok(
                CategoriaProdutoMapper.toResponse(categoriaSalva));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaProdutoResponse>> listar() {
        return ResponseEntity.ok(
                service.listar()
                        .stream()
                        .map(CategoriaProdutoMapper::toResponse)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaProdutoResponse> buscarPorId(
            @PathVariable Long id) {

        CategoriaProduto categoria = service.buscarPorId(id);

        if (categoria == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                CategoriaProdutoMapper.toResponse(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaProdutoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaProdutoRequest request) {

        CategoriaProduto categoria = CategoriaProdutoMapper.toEntity(request);

        CategoriaProduto categoriaAtualizada = service.atualizar(id, categoria);

        if (categoriaAtualizada == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                CategoriaProdutoMapper.toResponse(categoriaAtualizada));
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