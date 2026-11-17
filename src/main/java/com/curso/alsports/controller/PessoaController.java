package com.curso.alsports.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.dto.PessoaMapper;
import com.curso.alsports.dto.PessoaRequest;
import com.curso.alsports.dto.PessoaResponse;
import com.curso.alsports.dto.StatusRequest;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Status;
import com.curso.alsports.service.PessoaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pessoas")
@Tag(name = "Pessoas", description = "Operações de gerenciamento de pessoas")
public class PessoaController {

    private final PessoaService service;
    private final PessoaMapper pessoaMapper;

    public PessoaController(
            PessoaService service,
            PessoaMapper pessoaMapper) {

        this.service = service;
        this.pessoaMapper = pessoaMapper;
    }

    @Operation(
        summary = "Cadastrar pessoa",
        description = "Cadastra uma pessoa com CPF válido. O e-mail é normalizado para minúsculas e a data de cadastro é controlada pelo servidor."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Pessoa cadastrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PessoaResponse.class)
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
            description = "CPF ou e-mail já cadastrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<PessoaResponse> salvar(
            @Valid @RequestBody PessoaRequest request) {

        Pessoa pessoa = pessoaMapper.toEntity(request);
        Pessoa pessoaSalva = service.cadastrar(pessoa);

        return ResponseEntity
                .created(URI.create("/api/pessoas/" + pessoaSalva.getId()))
                .body(pessoaMapper.toResponse(pessoaSalva));
    }

    @Operation(
        summary = "Listar pessoas",
        description = "Consulta as pessoas cadastradas com paginação, ordenação e filtros opcionais por nome e situação. Os parâmetros de paginação são page (iniciado em zero), size e sort (campo,direção)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de pessoas consultada com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos para paginação, ordenação ou filtros",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<PaginaResponse<PessoaResponse>> listar(
            @Parameter(
                description = "Filtra pessoas pelo nome, permitindo buscar por parte do texto.",
                example = "Ana"
            )
            @RequestParam(required = false) String nome,
            @Parameter(
                description = "Filtra pessoas pela situação: ATIVO ou INATIVO.",
                example = "ATIVO"
            )
            @RequestParam(required = false) Status status,
            @PageableDefault(
                size = 20,
                sort = "id",
                direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(
                PaginaResponse.de(
                        service.pesquisar(nome, status, pageable),
                        pessoaMapper::toResponse));
    }

    @Operation(
        summary = "Buscar pessoa por ID",
        description = "Consulta os dados de uma pessoa utilizando seu identificador único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pessoa encontrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PessoaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<PessoaResponse> buscarPorId(
            @Parameter(
                description = "Identificador único da pessoa",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Pessoa pessoa = service.buscarPorId(id);

        return ResponseEntity.ok(
                pessoaMapper.toResponse(pessoa));
    }

    @Operation(
        summary = "Alterar pessoa",
        description = "Substitui nome, e-mail e CPF. A data de cadastro e a situação possuem fluxos próprios."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pessoa alterada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PessoaResponse.class)
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
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "CPF ou e-mail já cadastrado para outra pessoa",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<PessoaResponse> atualizar(
            @Parameter(
                description = "Identificador único da pessoa",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            @Valid @RequestBody PessoaRequest request) {

        Pessoa pessoaAtualizada = service.atualizar(
                id, request.getNome(), request.getEmail(), request.getCpf());

        return ResponseEntity.ok(
                pessoaMapper.toResponse(pessoaAtualizada));
    }

    @Operation(
        summary = "Alterar status da pessoa",
        description = "Ativa ou inativa uma pessoa existente. Inativar não remove o histórico de papéis e vendas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status da pessoa alterado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PessoaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Status inválido ou ausente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pessoa não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<PessoaResponse> alterarStatus(
            @Parameter(
                description = "Identificador único da pessoa",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {

        Pessoa pessoaAtualizada = service.alterarStatus(id, request.status());

        return ResponseEntity.ok(
                pessoaMapper.toResponse(pessoaAtualizada));
    }
}
