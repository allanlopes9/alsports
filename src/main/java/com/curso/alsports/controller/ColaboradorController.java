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

import com.curso.alsports.dto.ColaboradorAtualizacaoRequest;
import com.curso.alsports.dto.ColaboradorMapper;
import com.curso.alsports.dto.ColaboradorRequest;
import com.curso.alsports.dto.ColaboradorResponse;
import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.Colaborador;
import com.curso.alsports.model.FuncaoColaborador;
import com.curso.alsports.service.ColaboradorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/colaboradores")
@Tag(name = "Colaboradores", description = "Operações de gerenciamento de colaboradores")
public class ColaboradorController {

    private final ColaboradorService service;
    private final ColaboradorMapper colaboradorMapper;

    public ColaboradorController(
            ColaboradorService service,
            ColaboradorMapper colaboradorMapper) {

        this.service = service;
        this.colaboradorMapper = colaboradorMapper;
    }

    @Operation(
        summary = "Cadastrar colaborador",
        description = "Cria o vínculo profissional de uma pessoa existente e ativa. A matrícula deve ser única. A função profissional não representa perfil de acesso."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Colaborador cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ColaboradorResponse.class)
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
            description = "Pessoa já possui cadastro de colaborador ou matrícula já cadastrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ColaboradorResponse> salvar(
            @Valid @RequestBody ColaboradorRequest request) {

        Colaborador colaborador = service.cadastrar(
                request.getPessoaId(),
                request.getMatricula(),
                request.getFuncao(),
                request.getDataAdmissao());

        return ResponseEntity
                .created(URI.create("/api/colaboradores/" + colaborador.getId()))
                .body(colaboradorMapper.toResponse(colaborador));
    }

    @Operation(
        summary = "Listar colaboradores",
        description = "Consulta os colaboradores cadastrados com paginação, ordenação e filtros opcionais pelo nome da pessoa e pela função."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de colaboradores consultada com sucesso"
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
    public ResponseEntity<PaginaResponse<ColaboradorResponse>> listar(
            @Parameter(
                description = "Filtra colaboradores pelo nome da pessoa.",
                example = "Bruno"
            )
            @RequestParam(required = false) String nome,
            @Parameter(
                description = "Filtra colaboradores pela função profissional.",
                example = "VENDEDOR"
            )
            @RequestParam(required = false) FuncaoColaborador funcao,
            @PageableDefault(
                size = 20,
                sort = "id",
                direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(
                PaginaResponse.de(
                        service.pesquisar(nome, funcao, pageable),
                        colaboradorMapper::toResponse));
    }

    @Operation(
        summary = "Buscar colaborador por ID",
        description = "Consulta os dados de um colaborador utilizando seu identificador único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Colaborador encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ColaboradorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Colaborador não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ColaboradorResponse> buscarPorId(
            @Parameter(
                description = "Identificador único do colaborador",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Colaborador colaborador = service.buscarPorId(id);

        return ResponseEntity.ok(
                colaboradorMapper.toResponse(colaborador));
    }

    @Operation(
        summary = "Alterar colaborador",
        description = "Substitui matrícula e função. Pessoa e data de admissão identificam o vínculo e não são substituídas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Colaborador alterado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ColaboradorResponse.class)
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
            description = "Colaborador não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Matrícula já cadastrada para outro colaborador",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ColaboradorResponse> atualizar(
            @Parameter(
                description = "Identificador único do colaborador",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            @Valid @RequestBody ColaboradorAtualizacaoRequest request) {

        Colaborador colaboradorAtualizado = service.atualizar(
                id, request.getMatricula(), request.getFuncao());

        return ResponseEntity.ok(
                colaboradorMapper.toResponse(colaboradorAtualizado));
    }
}
