package com.curso.alsports.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curso.alsports.dto.LoginRequest;
import com.curso.alsports.dto.RefreshTokenRequest;
import com.curso.alsports.dto.TokenResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.service.SessaoAutenticacaoService;
import com.curso.alsports.service.TokensAutenticacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/autenticacao")
@Tag(
    name = "Autenticação",
    description = "Emissão, renovação e encerramento de sessões com access JWT e refresh opaco"
)
public class AutenticacaoController {

    private final SessaoAutenticacaoService service;

    public AutenticacaoController(SessaoAutenticacaoService service) {
        this.service = service;
    }

    @Operation(
        summary = "Iniciar sessão",
        description = "Autentica login e senha com o provider da Aula 12 e devolve um par "
            + "access/refresh. Credenciais inválidas, conta bloqueada ou pessoa "
            + "inativa recebem a mesma resposta genérica 401."
    )
    @SecurityRequirements
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sessão iniciada; par de tokens emitido",
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
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(paraResponse(service.iniciar(
                request.login(), request.senha(), origem(httpRequest))));
    }

    @Operation(
        summary = "Renovar sessão",
        description = "Consome o refresh token atual, gera novo par na mesma família e "
            + "revoga a família inteira se um token já utilizado for reapresentado. "
            + "O cliente deve substituir os dois tokens antigos."
    )
    @SecurityRequirements
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sessão renovada; novo par de tokens emitido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String refreshToken = request == null ? null : request.refreshToken();
        return ResponseEntity.ok(paraResponse(
                service.renovar(refreshToken, origem(httpRequest))));
    }

    @Operation(
        summary = "Logout local",
        description = "Revoga a família do refresh token informado. Não exige access token "
            + "e não revela se o token existia ou já estava revogado."
    )
    @SecurityRequirements
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Sessão encerrada"),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String refreshToken = request == null ? null : request.refreshToken();
        service.encerrar(refreshToken, origem(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Logout global",
        description = "Revoga todas as famílias ainda ativas da conta autenticada. "
            + "Exige Bearer access token válido."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Todas as sessões da conta foram encerradas"),
        @ApiResponse(responseCode = "401", description = "Access token ausente ou inválido")
    })
    @PostMapping("/logout-todos")
    public ResponseEntity<Void> logoutTodos(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        service.encerrarTodas(authentication.getName(), origem(httpRequest));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Usa apenas o endereço da conexão observado pelo container. Cabeçalhos
     * encaminhados não são lidos automaticamente porque podem ser forjados
     * quando a cadeia de proxies confiáveis não foi configurada.
     */
    private String origem(HttpServletRequest httpRequest) {
        return httpRequest.getRemoteAddr();
    }

    private TokenResponse paraResponse(TokensAutenticacao tokens) {
        return new TokenResponse(
                tokens.accessToken(),
                tokens.tokenType(),
                tokens.expiresIn(),
                tokens.expiresAt(),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresAt());
    }
}
