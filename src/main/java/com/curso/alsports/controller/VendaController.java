package com.curso.alsports.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.curso.alsports.dto.ItemVendaRequest;
import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.dto.VendaMapper;
import com.curso.alsports.dto.VendaRequest;
import com.curso.alsports.dto.VendaResponse;
import com.curso.alsports.dto.VendaResumoResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.StatusVenda;
import com.curso.alsports.model.Venda;
import com.curso.alsports.service.VendaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vendas")
@Tag(name = "Vendas", description = "Operações de abertura, composição, finalização e cancelamento de vendas")
public class VendaController {

    private final VendaService service;
    private final VendaMapper vendaMapper;

    public VendaController(
            VendaService service,
            VendaMapper vendaMapper) {

        this.service = service;
        this.vendaMapper = vendaMapper;
    }

    @Operation(
        summary = "Abrir venda",
        description = "Abre uma venda com cliente e vendedor ativos. O vendedor deve possuir função VENDEDOR ou GERENTE. A venda inicia com status ABERTA e sem itens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Venda aberta com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VendaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Participante inativo ou função não permitida para venda",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente ou colaborador não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<VendaResponse> cadastrar(
            @Valid @RequestBody VendaRequest request) {

        Venda venda = service.cadastrar(
                request.getClienteId(), request.getVendedorId());

        return ResponseEntity
                .created(URI.create("/api/vendas/" + venda.getId()))
                .body(vendaMapper.toResponse(venda));
    }

    @Operation(
        summary = "Listar vendas",
        description = "Consulta resumos de vendas com paginação, ordenação e filtros opcionais por status, cliente e intervalo de datas. A listagem não carrega os itens. A ordenação aceita id, dataVenda e status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de vendas consultada com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetros inválidos, intervalo de datas invertido ou campo de ordenação não permitido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<PaginaResponse<VendaResumoResponse>> listar(
            @Parameter(
                description = "Filtra vendas pelo status.",
                example = "ABERTA"
            )
            @RequestParam(required = false) StatusVenda status,
            @Parameter(
                description = "Filtra vendas pelo identificador do cliente.",
                example = "1"
            )
            @RequestParam(required = false) Long clienteId,
            @Parameter(
                description = "Data inicial inclusiva do período.",
                example = "2026-11-01"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataInicial,
            @Parameter(
                description = "Data final inclusiva do período.",
                example = "2026-11-30"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataFinal,
            @PageableDefault(
                size = 20,
                sort = "id",
                direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(
                PaginaResponse.de(
                        service.pesquisar(
                                status,
                                clienteId,
                                dataInicial,
                                dataFinal,
                                pageable),
                        vendaMapper::toResumo));
    }

    @Operation(
        summary = "Buscar venda por ID",
        description = "Consulta a representação completa de uma venda, incluindo participantes, itens, valores históricos e total calculado."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Venda encontrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VendaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<VendaResponse> buscarPorId(
            @Parameter(
                description = "Identificador único da venda",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Venda venda = service.buscarPorId(id);

        return ResponseEntity.ok(
                vendaMapper.toResponse(venda));
    }

    @Operation(
        summary = "Adicionar item à venda",
        description = "Inclui um produto ativo em uma venda ABERTA. O preço unitário é capturado neste momento e não muda se o preço do produto for alterado depois. Produto repetido é rejeitado."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item incluído com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VendaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Venda não está aberta, produto inativo, quantidade inválida ou produto repetido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda ou produto não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/{id}/itens")
    public ResponseEntity<VendaResponse> adicionarItem(
            @Parameter(
                description = "Identificador único da venda",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            @Valid @RequestBody ItemVendaRequest request) {

        Venda venda = service.adicionarItem(
                id, request.getProdutoId(), request.getQuantidade());

        return ResponseEntity.ok(
                vendaMapper.toResponse(venda));
    }

    @Operation(
        summary = "Remover item da venda",
        description = "Remove um item de uma venda ABERTA. O item removido é excluído fisicamente por orphanRemoval."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item removido com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VendaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Venda não está aberta ou item não pertence à venda",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @DeleteMapping("/{vendaId}/itens/{itemId}")
    public ResponseEntity<VendaResponse> removerItem(
            @Parameter(
                description = "Identificador único da venda",
                example = "1",
                required = true
            )
            @PathVariable Long vendaId,
            @Parameter(
                description = "Identificador único do item",
                example = "1",
                required = true
            )
            @PathVariable Long itemId) {

        Venda venda = service.removerItem(vendaId, itemId);

        return ResponseEntity.ok(
                vendaMapper.toResponse(venda));
    }

    @Operation(
        summary = "Finalizar venda",
        description = "Finaliza uma venda ABERTA com ao menos um item, validando participantes e produtos ativos. Os produtos são bloqueados com lock pessimista de escrita em ordem determinística pelo identificador, e a baixa de estoque de todos os itens ocorre na mesma transação. Se qualquer item não possuir saldo suficiente, nada é alterado."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Venda finalizada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VendaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Venda vazia, venda já encerrada, participante inativo, produto inativo ou saldo insuficiente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/{id}/finalizacao")
    public ResponseEntity<VendaResponse> finalizar(
            @Parameter(
                description = "Identificador único da venda",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Venda venda = service.finalizar(id);

        return ResponseEntity.ok(
                vendaMapper.toResponse(venda));
    }

    @Operation(
        summary = "Cancelar venda",
        description = "Cancela uma venda ABERTA sem movimentar estoque. Vendas finalizadas não podem ser canceladas nesta etapa."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Venda cancelada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VendaResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Venda não está aberta",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Venda não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/{id}/cancelamento")
    public ResponseEntity<VendaResponse> cancelar(
            @Parameter(
                description = "Identificador único da venda",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Venda venda = service.cancelar(id);

        return ResponseEntity.ok(
                vendaMapper.toResponse(venda));
    }
}
