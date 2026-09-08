package com.curso.alsports.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.curso.alsports.dto.FornecedorMapper;
import com.curso.alsports.dto.FornecedorRequest;
import com.curso.alsports.dto.FornecedorResponse;
import com.curso.alsports.model.Fornecedor;
import com.curso.alsports.service.FornecedorService;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final FornecedorService service;
    private final FornecedorMapper fornecedorMapper;

    public FornecedorController(
            FornecedorService service,
            FornecedorMapper fornecedorMapper) {

        this.service = service;
        this.fornecedorMapper = fornecedorMapper;
    }

    @PostMapping
    public ResponseEntity<FornecedorResponse> salvar(
            @Valid @RequestBody FornecedorRequest request) {

        Fornecedor fornecedor = fornecedorMapper.toEntity(request);
        Fornecedor fornecedorSalvo = service.salvar(fornecedor);

        return ResponseEntity
                .created(URI.create("/fornecedores/" + fornecedorSalvo.getId()))
                .body(fornecedorMapper.toResponse(fornecedorSalvo));
    }

    @GetMapping
    public ResponseEntity<List<FornecedorResponse>> listar() {
        return ResponseEntity.ok(
                service.listar()
                        .stream()
                        .map(fornecedorMapper::toResponse)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponse> buscarPorId(
            @PathVariable Long id) {

        Fornecedor fornecedor = service.buscarPorId(id);

        return ResponseEntity.ok(
                fornecedorMapper.toResponse(fornecedor));
    }
}
