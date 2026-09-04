package com.curso.alsports.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.curso.alsports.dto.CategoriaProdutoMapper;
import com.curso.alsports.dto.CategoriaProdutoRequest;
import com.curso.alsports.dto.CategoriaProdutoResponse;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.service.CategoriaProdutoService;

@RestController
@RequestMapping("/categorias")
public class CategoriaProdutoController {

    private final CategoriaProdutoService service;
    private final CategoriaProdutoMapper categoriaProdutoMapper;

    public CategoriaProdutoController(
            CategoriaProdutoService service,
            CategoriaProdutoMapper categoriaProdutoMapper) {

        this.service = service;
        this.categoriaProdutoMapper = categoriaProdutoMapper;
    }

    @PostMapping
    public ResponseEntity<CategoriaProdutoResponse> salvar(
            @Valid @RequestBody CategoriaProdutoRequest request) {

        CategoriaProduto categoria = categoriaProdutoMapper.toEntity(request);

        CategoriaProduto categoriaSalva = service.salvar(categoria);

        return ResponseEntity
                .created(java.net.URI.create("/categorias/" + categoriaSalva.getId()))
                .body(categoriaProdutoMapper.toResponse(categoriaSalva));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaProdutoResponse>> listar() {
        return ResponseEntity.ok(
                service.listar()
                        .stream()
                        .map(categoriaProdutoMapper::toResponse)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaProdutoResponse> buscarPorId(
            @PathVariable Long id) {

        CategoriaProduto categoria = service.buscarPorId(id);

        return ResponseEntity.ok(
                categoriaProdutoMapper.toResponse(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaProdutoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaProdutoRequest request) {

        CategoriaProduto categoria = categoriaProdutoMapper.toEntity(request);

        CategoriaProduto categoriaAtualizada =
                service.atualizar(id, categoria);

        return ResponseEntity.ok(
                categoriaProdutoMapper.toResponse(categoriaAtualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        service.excluir(id);

        return ResponseEntity.noContent().build();
    }
}