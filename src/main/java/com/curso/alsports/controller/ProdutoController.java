package com.curso.alsports.controller;

import java.math.BigDecimal;
import java.net.URI;

import jakarta.validation.Valid;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.curso.alsports.dto.MovimentacaoEstoqueRequest;
import com.curso.alsports.dto.PaginaResponse;
import com.curso.alsports.dto.ProdutoMapper;
import com.curso.alsports.dto.ProdutoRequest;
import com.curso.alsports.dto.ProdutoResponse;
import com.curso.alsports.dto.ProdutoUpdateRequest;
import com.curso.alsports.dto.StatusRequest;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.Produto;
import com.curso.alsports.service.ProdutoService;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos", description = "Operações de gerenciamento de produtos e estoque")
public class ProdutoController {

    private final ProdutoService service;
    private final ProdutoMapper produtoMapper;

    public ProdutoController(
        ProdutoService service,
        ProdutoMapper produtoMapper) {

        this.service = service;
        this.produtoMapper = produtoMapper;
    }

    @Operation(
        summary = "Cadastrar produto",
        description = "Realiza o cadastro de um novo produto, vinculando-o a uma categoria e, opcionalmente, a um fornecedor."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Produto cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProdutoResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos ou corpo da requisição em formato JSON inválido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Categoria ou fornecedor não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Código de barras ou nome já utilizado por outro produto",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ProdutoResponse> salvar(
        @Valid @RequestBody ProdutoRequest request) {

        Produto produto = produtoMapper.toEntity(request);

        Produto produtoSalvo = service.cadastrar(
            produto,
            request.getCategoriaId(),
            request.getFornecedorId());

        return ResponseEntity
            .created(URI.create("/api/produtos/" + produtoSalvo.getId()))
            .body(produtoMapper.toResponse(produtoSalvo));
    }

    @Operation(
        summary = "Listar produtos",
        description = "Consulta os produtos cadastrados com paginação, ordenação e filtros opcionais por nome, situação, categoria, fornecedor e quantidade em estoque. Os parâmetros de paginação são page (iniciado em zero), size e sort (campo,direção)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de produtos consultada com sucesso"
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
    public ResponseEntity<PaginaResponse<ProdutoResponse>> listar(

        @Parameter(
            description = "Filtra produtos pelo nome, permitindo buscar por parte do texto.",
            example = "Bola"
        )
        @RequestParam(required = false) String nome,

        @Parameter(
            description = "Filtra produtos pela situação: true para ativos e false para inativos.",
            example = "true"
        )
        @RequestParam(required = false) Boolean ativo,

        @Parameter(
            description = "Identificador da categoria utilizada no filtro.",
            example = "1"
        )
        @RequestParam(required = false) Long categoriaId,

        @Parameter(
            description = "Identificador do fornecedor utilizado no filtro.",
            example = "1"
        )
        @RequestParam(required = false) Long fornecedorId,

        @Parameter(
            description = "Quantidade mínima em estoque para filtrar os produtos, com até 3 casas decimais.",
            example = "5.000"
        )
        @RequestParam(required = false) BigDecimal quantidadeMinima,

        @Parameter(
            description = "Quantidade máxima em estoque para filtrar os produtos, com até 3 casas decimais.",
            example = "50.000"
        )
        @RequestParam(required = false) BigDecimal quantidadeMaxima,

        @Parameter(
            description = "Filtra produtos abaixo do estoque mínimo: true para abaixo do mínimo e false para os demais.",
            example = "true"
        )
        @RequestParam(required = false) Boolean abaixoEstoqueMinimo,

        @PageableDefault(
            size = 20,
            sort = "id",
            direction = Sort.Direction.ASC)
        Pageable pageable) {

        if (quantidadeMinima != null
            && quantidadeMinima.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "A quantidade mínima não pode ser negativa");
        }

        if (quantidadeMaxima != null
            && quantidadeMaxima.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "A quantidade máxima não pode ser negativa");
        }

        if (quantidadeMinima != null
            && quantidadeMaxima != null
            && quantidadeMinima.compareTo(quantidadeMaxima) > 0) {

            throw new IllegalArgumentException(
                "A quantidade mínima não pode ser maior que a máxima");
        }

        return ResponseEntity.ok(
            PaginaResponse.de(
                service.pesquisar(
                    nome,
                    ativo,
                    categoriaId,
                    fornecedorId,
                    quantidadeMinima,
                    quantidadeMaxima,
                    abaixoEstoqueMinimo,
                    pageable),
                produtoMapper::toResponse));
    }

    @Operation(
        summary = "Buscar produto por ID",
        description = "Consulta os dados de um produto utilizando seu identificador único."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Produto encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProdutoResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Produto não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(
        @Parameter(
            description = "Identificador único do produto",
            example = "1",
            required = true
        )
        @PathVariable Long id) {

        Produto produto = service.buscarPorId(id);

        return ResponseEntity.ok(
            produtoMapper.toResponse(produto));
    }

    @Operation(
        summary = "Alterar produto",
        description = "Substitui nome, estoque mínimo, preço, categoria e fornecedor. Código de barras, saldo em estoque, data de cadastro e situação possuem fluxos próprios."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Produto alterado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProdutoResponse.class)
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
            description = "Produto, categoria ou fornecedor não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Nome já utilizado por outro produto",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(
        @Parameter(
            description = "Identificador único do produto",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @Valid @RequestBody ProdutoUpdateRequest request) {

        Produto produtoAtualizado = service.atualizar(
            id,
            request.getNome(),
            request.getEstoqueMinimo(),
            request.getPreco(),
            request.getCategoriaId(),
            request.getFornecedorId());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @Operation(
        summary = "Receber estoque",
        description = "Bloqueia o produto com lock pessimista de escrita e soma uma quantidade positiva ao saldo atual. A operação não é idempotente: repetir a chamada repete o efeito."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Entrada de estoque registrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProdutoResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Quantidade inválida ou que excede o limite permitido para o estoque",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Produto não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/{id}/estoque/entradas")
    public ResponseEntity<ProdutoResponse> receberEstoque(
        @Parameter(
            description = "Identificador único do produto",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        Produto produtoAtualizado = service.receberEstoque(
            id, request.quantidade());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @Operation(
        summary = "Retirar estoque",
        description = "Bloqueia o produto com lock pessimista de escrita e subtrai uma quantidade positiva do saldo atual, sem permitir saldo negativo. A operação não é idempotente: repetir a chamada repete o efeito."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Saída de estoque registrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProdutoResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Quantidade inválida ou maior que o estoque disponível",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Produto não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/{id}/estoque/saidas")
    public ResponseEntity<ProdutoResponse> retirarEstoque(
        @Parameter(
            description = "Identificador único do produto",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        Produto produtoAtualizado = service.retirarEstoque(
            id, request.quantidade());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @Operation(
        summary = "Alterar status do produto",
        description = "Ativa ou inativa um produto existente."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status do produto alterado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProdutoResponse.class)
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
            description = "Produto não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ProdutoResponse> alterarStatus(
        @Parameter(
            description = "Identificador único do produto",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @Valid @RequestBody StatusRequest request) {

        Produto produtoAtualizado = service.alterarStatus(
            id, request.status());

        return ResponseEntity.ok(
            produtoMapper.toResponse(produtoAtualizado));
    }

    @Operation(
        summary = "Excluir produto",
        description = "Exclui fisicamente um produto existente pelo identificador."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Produto excluído com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Produto não encontrado",
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
