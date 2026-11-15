package com.curso.alsports.controller;

import java.net.URI;

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

import jakarta.validation.Valid;

import com.curso.alsports.dto.CategoriaProdutoMapper;
import com.curso.alsports.dto.CategoriaProdutoRequest;
import com.curso.alsports.dto.CategoriaProdutoResponse;
import com.curso.alsports.dto.CategoriaProdutoUpdateRequest;
import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.dto.StatusRequest;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.service.CategoriaProdutoService;

@RestController
@RequestMapping("/api/categorias")
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
            .created(URI.create("/api/categorias/" + categoriaSalva.getId()))
            .body(categoriaProdutoMapper.toResponse(categoriaSalva));
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<CategoriaProdutoResponse>> listar(
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) Boolean ativo,
        @PageableDefault(
            size = 20,
            sort = "id",
            direction = Sort.Direction.ASC)
        Pageable pageable) {

        return ResponseEntity.ok(
            PaginaResponse.de(
                service.pesquisar(nome, ativo, pageable),
                categoriaProdutoMapper::toResponse));
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
        @Valid @RequestBody CategoriaProdutoUpdateRequest request) {

        CategoriaProduto categoriaAtualizada =
            service.alterar(id, request.nome());

        return ResponseEntity.ok(
            categoriaProdutoMapper.toResponse(categoriaAtualizada));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CategoriaProdutoResponse> alterarStatus(
        @PathVariable Long id,
        @Valid @RequestBody StatusRequest request) {

        CategoriaProduto categoriaAtualizada =
            service.alterarStatus(id, request.status());

        return ResponseEntity.ok(
            categoriaProdutoMapper.toResponse(categoriaAtualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        service.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
