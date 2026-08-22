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

import com.curso.alsports.model.Produto;
import com.curso.alsports.service.ProdutoService;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Produto> salvar(
            @RequestBody Produto produto) {

        Produto produtoSalvo = service.salvar(produto);

        return ResponseEntity.ok(produtoSalvo);
    }

    @GetMapping
    public ResponseEntity<List<Produto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarPorId(
            @PathVariable Long id) {

        Produto produto = service.buscarPorId(id);

        if (produto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(produto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizar(
            @PathVariable Long id,
            @RequestBody Produto produto) {

        Produto produtoAtualizado = service.atualizar(id, produto);

        if (produtoAtualizado == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(produtoAtualizado);
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