package com.curso.alsports.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curso.alsports.dto.AlteracaoSenhaRequest;
import com.curso.alsports.dto.ContaAcessoMapper;
import com.curso.alsports.dto.ContaAcessoRequest;
import com.curso.alsports.dto.ContaAcessoResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.service.ContaAcessoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contas-acesso")
@Tag(
    name = "Contas de acesso",
    description = "Cadastro e manutenção da credencial de acesso vinculada a uma pessoa"
)
public class ContaAcessoController {

    private final ContaAcessoService service;
    private final ContaAcessoMapper mapper;

    public ContaAcessoController(
            ContaAcessoService service,
            ContaAcessoMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "Cadastrar conta de acesso",
        description = "Cria uma credencial para uma pessoa ativa. A conta inicia ATIVA, "
            + "recebe somente o perfil OPERADOR e o request não pode escolher perfis. "
            + "A senha é persistida apenas como hash BCrypt e nunca é devolvida."
    )
    @SecurityRequirements
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Conta cadastrada com sucesso, sem exposição de credenciais",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContaAcessoResponse.class)
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
            description = "Pessoa ou login já possui conta de acesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ContaAcessoResponse> cadastrar(
            @Valid @RequestBody ContaAcessoRequest request) {

        ContaAcesso conta = service.cadastrar(request);

        return ResponseEntity
                .created(URI.create("/api/contas-acesso/" + conta.getId()))
                .body(mapper.toResponse(conta));
    }

    @Operation(
        summary = "Consultar a própria conta",
        description = "Retorna a conta identificada pelo usuário autenticado no HTTP Basic. "
            + "A resposta não inclui senha nem hash."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conta consultada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContaAcessoResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credencial ausente ou inválida"
        )
    })
    @GetMapping("/me")
    public ResponseEntity<ContaAcessoResponse> minhaConta(
            Authentication authentication) {

        ContaAcesso conta = service.buscarPorLogin(authentication.getName());

        return ResponseEntity.ok(mapper.toResponse(conta));
    }

    @Operation(
        summary = "Alterar a própria senha",
        description = "Confere a senha atual, rejeita uma nova senha igual e persiste "
            + "somente o novo hash BCrypt. A senha anterior deixa de autenticar."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Senha alterada com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Senha atual inválida, nova senha igual ou fora dos limites",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credencial ausente ou inválida"
        )
    })
    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(
            Authentication authentication,
            @Valid @RequestBody AlteracaoSenhaRequest request) {

        service.alterarSenha(
                authentication.getName(),
                request.senhaAtual(),
                request.novaSenha());

        return ResponseEntity.noContent().build();
    }
}
