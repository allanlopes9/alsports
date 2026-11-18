package com.curso.alsports.controller;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.curso.alsports.dto.EventoSegurancaResponse;
import com.curso.alsports.exception.ApiError;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.service.AuditoriaSegurancaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Consulta somente leitura da trilha estruturada de segurança.
 *
 * <p>O filtro é um enum fechado. Não existe endpoint de escrita, exclusão ou
 * alteração; a trilha é produzida somente pelos casos de uso autenticados.</p>
 */
@RestController
@RequestMapping("/api/auditoria-seguranca")
@Tag(
    name = "Auditoria de segurança",
    description = "Consulta administrativa somente leitura dos eventos de autenticação e administração"
)
public class AuditoriaSegurancaController {

    private final AuditoriaSegurancaService service;

    public AuditoriaSegurancaController(AuditoriaSegurancaService service) {
        this.service = service;
    }

    @Operation(
        summary = "Consultar eventos de segurança",
        description = "Restringido a ADMINISTRADOR. Pagina os eventos em ordem estável, "
            + "mais recentes primeiro, com filtro opcional por tipo. A resposta não "
            + "contém hash de login, senha, tokens, cabeçalhos ou corpos de requisição."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Página de eventos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Paginação ou tipo de evento inválido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token ausente ou inválido"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Autenticado sem perfil ADMINISTRADOR"
        )
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<EventoSegurancaResponse>> consultar(
            @Parameter(description = "Página iniciando em zero", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamanho da página, de 1 a 100", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(
                description = "Tipo do evento; quando ausente, todos são retornados",
                example = "LOGIN_FALHA"
            )
            @RequestParam(required = false) String tipo) {

        return ResponseEntity.ok(service.consultar(interpretarTipo(tipo), page, size));
    }

    private TipoEventoSeguranca interpretarTipo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return TipoEventoSeguranca.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Tipo de evento de segurança desconhecido");
        }
    }
}
