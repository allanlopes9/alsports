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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.curso.alsports.dto.FornecedorMapper;
import com.curso.alsports.dto.FornecedorRequest;
import com.curso.alsports.dto.FornecedorResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.Fornecedor;
import com.curso.alsports.service.FornecedorService;

@RestController
@RequestMapping("/api/fornecedores")
@Tag(name = "Fornecedores", description = "Operações de gerenciamento de fornecedores")
public class FornecedorController {

    private final FornecedorService service;
    private final FornecedorMapper fornecedorMapper;

    public FornecedorController(
            FornecedorService service,
            FornecedorMapper fornecedorMapper) {

        this.service = service;
        this.fornecedorMapper = fornecedorMapper;
    }

    @Operation(
        summary = "Cadastrar fornecedor",
        description = "Cadastra um novo fornecedor. O CNPJ deve conter exatamente 14 dígitos e não pode estar cadastrado em outro fornecedor. Quando o status não é informado, o fornecedor é criado como ativo."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Fornecedor cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FornecedorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "CNPJ já cadastrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<FornecedorResponse> salvar(
            @Valid @RequestBody FornecedorRequest request) {

        Fornecedor fornecedor = fornecedorMapper.toEntity(request);
        Fornecedor fornecedorSalvo = service.salvar(fornecedor);

        return ResponseEntity
                .created(URI.create("/api/fornecedores/" + fornecedorSalvo.getId()))
                .body(fornecedorMapper.toResponse(fornecedorSalvo));
    }

    @Operation(
        summary = "Listar fornecedores",
        description = "Retorna todos os fornecedores cadastrados, sem paginação."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de fornecedores consultada com sucesso",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(
                    schema = @Schema(implementation = FornecedorResponse.class)
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<FornecedorResponse>> listar() {
        return ResponseEntity.ok(
                service.listar()
                        .stream()
                        .map(fornecedorMapper::toResponse)
                        .toList());
    }

    @Operation(
        summary = "Buscar fornecedor por ID",
        description = "Consulta os dados de um fornecedor utilizando seu identificador único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Fornecedor encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FornecedorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Fornecedor não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponse> buscarPorId(
            @Parameter(
                description = "Identificador único do fornecedor",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Fornecedor fornecedor = service.buscarPorId(id);

        return ResponseEntity.ok(
                fornecedorMapper.toResponse(fornecedor));
    }
}
