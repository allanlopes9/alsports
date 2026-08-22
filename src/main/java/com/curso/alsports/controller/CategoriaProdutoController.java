package com.curso.alsports.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<CategoriaProduto> salvar(
            @RequestBody CategoriaProduto categoria) {

        CategoriaProduto categoriaSalva = service.salvar(categoria);

        return ResponseEntity.ok(categoriaSalva);
    }

    @GetMapping
    public ResponseEntity<List<CategoriaProduto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaProduto> buscarPorId(
            @PathVariable Long id) {

        CategoriaProduto categoria = service.buscarPorId(id);

        if (categoria == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(categoria);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaProduto> atualizar(
            @PathVariable Long id,
            @RequestBody CategoriaProduto categoria) {

        CategoriaProduto categoriaAtualizada = service.atualizar(id, categoria);

        if (categoriaAtualizada == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(categoriaAtualizada);
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