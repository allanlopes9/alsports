package com.curso.alsports.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curso.alsports.dto.LoginRequest;
import com.curso.alsports.dto.TokenResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.service.AutenticacaoService;
import com.curso.alsports.service.TokenAcesso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/autenticacao")
@Tag(
    name = "Autenticação",
    description = "Emissão do access token JWT usado nas rotas protegidas"
)
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @Operation(
        summary = "Emitir access token JWT",
        description = "Autentica login e senha com o mesmo provider da Aula 12 e "
            + "emite um JWT HS256 com validade absoluta de 30 minutos. "
            + "Credenciais inválidas, conta bloqueada ou pessoa inativa "
            + "recebem a mesma resposta genérica 401."
    )
    @SecurityRequirements
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Credenciais válidas; token emitido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Corpo inválido, login ou senha em branco ou acima do limite",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciais inválidas, conta bloqueada ou pessoa inativa",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request) {

        TokenAcesso token = service.autenticar(
                request.login(), request.senha());

        return ResponseEntity.ok(new TokenResponse(
                token.valor(),
                "Bearer",
                token.duracaoSegundos(),
                token.expiraEm()));
    }
}
