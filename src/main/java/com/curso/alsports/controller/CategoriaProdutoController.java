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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.curso.alsports.dto.CategoriaProdutoMapper;
import com.curso.alsports.dto.CategoriaProdutoRequest;
import com.curso.alsports.dto.CategoriaProdutoResponse;
import com.curso.alsports.dto.CategoriaProdutoUpdateRequest;
import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.dto.StatusRequest;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.CategoriaProduto;
import com.curso.alsports.service.CategoriaProdutoService;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias", description = "Operações de gerenciamento de categorias de produtos")
public class CategoriaProdutoController {

    private final CategoriaProdutoService service;
    private final CategoriaProdutoMapper categoriaProdutoMapper;

    public CategoriaProdutoController(
        CategoriaProdutoService service,
        CategoriaProdutoMapper categoriaProdutoMapper) {

        this.service = service;
        this.categoriaProdutoMapper = categoriaProdutoMapper;
    }

    @Operation(
        summary = "Cadastrar categoria",
        description = "Cadastra uma nova categoria de produtos. O nome é normalizado e deve ser único, sem diferenciar maiúsculas de minúsculas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Categoria cadastrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoriaProdutoResponse.class)
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
            description = "Nome já utilizado por outra categoria",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<CategoriaProdutoResponse> salvar(
        @Valid @RequestBody CategoriaProdutoRequest request) {

        CategoriaProduto categoria = categoriaProdutoMapper.toEntity(request);

        CategoriaProduto categoriaSalva = service.salvar(categoria);

        return ResponseEntity
            .created(URI.create("/api/categorias/" + categoriaSalva.getId()))
            .body(categoriaProdutoMapper.toResponse(categoriaSalva));
    }

    @Operation(
        summary = "Listar categorias",
        description = "Consulta as categorias cadastradas com paginação, ordenação e filtros opcionais por nome e situação. Os parâmetros de paginação são page (iniciado em zero), size e sort (campo,direção)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de categorias consultada com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos para paginação ou ordenação",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<PaginaResponse<CategoriaProdutoResponse>> listar(
        @Parameter(
            description = "Filtra categorias pelo nome, sem diferenciar maiúsculas de minúsculas.",
            example = "Futebol"
        )
        @RequestParam(required = false) String nome,
        @Parameter(
            description = "Filtra categorias pela situação: true para ativas e false para inativas.",
            example = "true"
        )
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

    @Operation(
        summary = "Buscar categoria por ID",
        description = "Consulta os dados de uma categoria utilizando seu identificador único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Categoria encontrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoriaProdutoResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Categoria não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaProdutoResponse> buscarPorId(
        @Parameter(
            description = "Identificador único da categoria",
            example = "1",
            required = true
        )
        @PathVariable Long id) {

        CategoriaProduto categoria = service.buscarPorId(id);

        return ResponseEntity.ok(
            categoriaProdutoMapper.toResponse(categoria));
    }

    @Operation(
        summary = "Alterar categoria",
        description = "Substitui o nome da categoria. O nome é normalizado e deve ser único, sem diferenciar maiúsculas de minúsculas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Categoria alterada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoriaProdutoResponse.class)
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
            description = "Categoria não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Nome já utilizado por outra categoria",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaProdutoResponse> atualizar(
        @Parameter(
            description = "Identificador único da categoria",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @Valid @RequestBody CategoriaProdutoUpdateRequest request) {

        CategoriaProduto categoriaAtualizada =
            service.alterar(id, request.nome());

        return ResponseEntity.ok(
            categoriaProdutoMapper.toResponse(categoriaAtualizada));
    }

    @Operation(
        summary = "Alterar status da categoria",
        description = "Ativa ou inativa uma categoria existente."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status da categoria alterado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoriaProdutoResponse.class)
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
            description = "Categoria não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<CategoriaProdutoResponse> alterarStatus(
        @Parameter(
            description = "Identificador único da categoria",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @Valid @RequestBody StatusRequest request) {

        CategoriaProduto categoriaAtualizada =
            service.alterarStatus(id, request.status());

        return ResponseEntity.ok(
            categoriaProdutoMapper.toResponse(categoriaAtualizada));
    }

    @Operation(
        summary = "Excluir categoria",
        description = "Exclui fisicamente a categoria somente quando nenhum produto a utiliza."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Categoria excluída com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Categoria não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Categoria em uso por produtos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        service.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
