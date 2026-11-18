package com.curso.alsports.service;

import java.time.Instant;

/**
 * Par de tokens devolvido pelo serviço de sessão.
 *
 * <p>O refresh token opaco existe apenas neste objeto de transporte interno.
 * Ele nunca é persistido em texto puro e nunca entra em logs ou DTOs de erro.</p>
 *
 * @param accessToken           JWT HS256 assinado
 * @param tokenType             esquema Bearer
 * @param expiresIn             validade do access token em segundos
 * @param expiresAt             expiração absoluta do access token
 * @param refreshToken          valor opaco a ser guardado pelo cliente
 * @param refreshTokenExpiresAt limite absoluto da família de refresh tokens
 */
public record TokensAutenticacao(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {
}
