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

import com.curso.alsports.dto.ClienteAtualizacaoRequest;
import com.curso.alsports.dto.ClienteMapper;
import com.curso.alsports.dto.ClienteRequest;
import com.curso.alsports.dto.ClienteResponse;
import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.Cliente;
import com.curso.alsports.service.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Operações de gerenciamento de clientes")
public class ClienteController {

    private final ClienteService service;
    private final ClienteMapper clienteMapper;

    public ClienteController(
            ClienteService service,
            ClienteMapper clienteMapper) {

        this.service = service;
        this.clienteMapper = clienteMapper;
    }

    @Operation(
        summary = "Cadastrar cliente",
        description = "Associa o papel de cliente a uma pessoa existente e ativa. Uma pessoa não pode possuir o mesmo papel duas vezes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Cliente cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ClienteResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos ou pessoa inativa",
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
            description = "Pessoa já possui cadastro de cliente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ClienteResponse> salvar(
            @Valid @RequestBody ClienteRequest request) {

        Cliente cliente = service.cadastrar(
                request.getPessoaId(), request.getTelefone());

        return ResponseEntity
                .created(URI.create("/api/clientes/" + cliente.getId()))
                .body(clienteMapper.toResponse(cliente));
    }

    @Operation(
        summary = "Listar clientes",
        description = "Consulta os clientes cadastrados com paginação, ordenação e filtro opcional pelo nome da pessoa."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de clientes consultada com sucesso"
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
    public ResponseEntity<PaginaResponse<ClienteResponse>> listar(
            @Parameter(
                description = "Filtra clientes pelo nome da pessoa.",
                example = "Ana"
            )
            @RequestParam(required = false) String nome,
            @PageableDefault(
                size = 20,
                sort = "id",
                direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(
                PaginaResponse.de(
                        service.pesquisar(nome, pageable),
                        clienteMapper::toResponse));
    }

    @Operation(
        summary = "Buscar cliente por ID",
        description = "Consulta os dados de um cliente utilizando seu identificador único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cliente encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ClienteResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(
            @Parameter(
                description = "Identificador único do cliente",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Cliente cliente = service.buscarPorId(id);

        return ResponseEntity.ok(
                clienteMapper.toResponse(cliente));
    }

    @Operation(
        summary = "Alterar cliente",
        description = "Substitui o telefone do cliente. A pessoa associada não é substituída."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cliente alterado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ClienteResponse.class)
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
            description = "Cliente não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar(
            @Parameter(
                description = "Identificador único do cliente",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {

        Cliente clienteAtualizado = service.atualizar(
                id, request.getTelefone());

        return ResponseEntity.ok(
                clienteMapper.toResponse(clienteAtualizado));
    }
}
