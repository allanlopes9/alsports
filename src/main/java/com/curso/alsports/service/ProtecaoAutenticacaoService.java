package com.curso.alsports.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.ProtecaoAutenticacaoProperties;
import com.curso.alsports.model.ProtecaoAutenticacao;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ProtecaoAutenticacaoRepository;

/**
 * Proteção persistente contra tentativas on-line de adivinhar senhas.
 *
 * <p>Falhas e recusas usam transação própria ({@code REQUIRES_NEW}) porque a
 * resposta HTTP é 401: se o registro fizesse parte da transação do caso de
 * uso, o rollback da exceção apagaria o contador. O sucesso participa da
 * transação do chamador para que o evento não afirme uma operação que depois
 * sofreu rollback.</p>
 */
@Service
public class ProtecaoAutenticacaoService implements ProtecaoLogin {

    private final ProtecaoAutenticacaoRepository repository;
    private final RegistradorEventosSeguranca auditoria;
    private final ProtecaoAutenticacaoProperties properties;
    private final Clock clock;

    public ProtecaoAutenticacaoService(
            ProtecaoAutenticacaoRepository repository,
            RegistradorEventosSeguranca auditoria,
            ProtecaoAutenticacaoProperties properties,
            Clock clock) {

        this.repository = repository;
        this.auditoria = auditoria;
        this.properties = properties;
        this.clock = clock;
    }

    public static String normalizarLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login é obrigatório");
        }
        return login.trim().toLowerCase(Locale.ROOT);
    }

    public static String calcularHashLogin(String login) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(normalizarLogin(login)
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não disponível", ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean permitirTentativa(String login, String enderecoOrigem) {
        String chave = calcularHashLogin(login);
        ProtecaoAutenticacao protecao = repository
                .findByChaveLogin(chave)
                .orElse(null);

        if (protecao != null && protecao.estaBloqueada(clock.instant())) {
            auditoria.registrar(
                    TipoEventoSeguranca.LOGIN_RECUSADO_BLOQUEIO,
                    clock.instant(),
                    null,
                    null,
                    chave,
                    enderecoOrigem);
            return false;
        }
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(
            String login, String enderecoOrigem, Long alvoContaId) {

        String chave = calcularHashLogin(login);
        repository.criarSeNecessario(chave);

        ProtecaoAutenticacao protecao = repository
                .buscarParaAtualizacao(chave)
                .orElseThrow(() -> new IllegalStateException(
                        "Falha ao localizar contador de autenticação"));

        boolean ativouBloqueio = protecao.registrarFalha(
                clock.instant(),
                properties.maxFailedAttempts(),
                properties.failureWindow(),
                properties.lockDuration());
        repository.save(protecao);

        auditoria.registrar(
                TipoEventoSeguranca.LOGIN_FALHA,
                clock.instant(),
                null,
                alvoContaId,
                chave,
                enderecoOrigem);

        if (ativouBloqueio) {
            auditoria.registrar(
                    TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO,
                    clock.instant(),
                    null,
                    alvoContaId,
                    chave,
                    enderecoOrigem);
        }
    }

    @Override
    @Transactional
    public void registrarSucesso(
            String login, String enderecoOrigem, Long contaId) {

        String chave = calcularHashLogin(login);
        repository.findByChaveLogin(chave).ifPresent(protecao -> {
            protecao.registrarSucesso();
            repository.save(protecao);
        });

        auditoria.registrar(
                TipoEventoSeguranca.LOGIN_SUCESSO,
                clock.instant(),
                contaId,
                contaId,
                chave,
                enderecoOrigem);
    }
}
