
package com.curso.alsports.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ResponseEntity<ApiError> tratarRefreshTokenInvalido(
        RefreshTokenInvalidoException ex,
        HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(erro);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> tratarNaoEncontrado(
        RecursoNaoEncontradoException ex,
        HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(erro);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ApiError> tratarDuplicado(
        RecursoDuplicadoException ex,
        HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(erro);
    }

    @ExceptionHandler(RecursoEmUsoException.class)
    public ResponseEntity<ApiError> tratarRecursoEmUso(
        RecursoEmUsoException ex,
        HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarValidacao(
        MethodArgumentNotValidException ex,
        HttpServletRequest request) {

        Map<String, String> fields = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage(),
                (mensagem1, mensagem2) -> mensagem1));

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Um ou mais campos possuem dados inválidos.",
            request.getRequestURI(),
            fields);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(erro);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> tratarRegraDeNegocio(
        RuntimeException ex,
        HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(erro);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> tratarIntegridade(
        DataIntegrityViolationException ex,
        HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            "O registro não pode ser removido porque está vinculado a outros dados. "
                + "Considere inativá-lo em vez de excluí-lo.",
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(erro);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ApiError> tratarCredenciaisInvalidas(
        CredenciaisInvalidasException ex,
        HttpServletRequest request) {

        return resposta401(ex.getMessage(), request);
    }

    /**
     * A divergência de geração é uma condição técnica de concorrência: a
     * resposta permanece genericamente 401 para não revelar o motivo ao
     * cliente, mas a exceção não é uma credencial inválida e por isso não é
     * preservada pelo rollback controlado do login.
     */
    @ExceptionHandler(SessaoGeracaoDivergenteException.class)
    public ResponseEntity<ApiError> tratarGeracaoDivergente(
        SessaoGeracaoDivergenteException ex,
        HttpServletRequest request) {

        return resposta401("Login ou senha inválidos", request);
    }

    private ResponseEntity<ApiError> resposta401(
        String mensagem, HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            mensagem,
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(erro);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> tratarJsonInvalido(
        HttpMessageNotReadableException ex,
        HttpServletRequest request) {

        ApiError erro = new ApiError(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "O corpo da requisição está malformado ou contém dados inválidos.",
            request.getRequestURI(),
            null);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(erro);
    }
}
