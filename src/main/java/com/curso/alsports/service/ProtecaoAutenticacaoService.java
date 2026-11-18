package com.curso.alsports.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.ProtecaoAutenticacaoProperties;
import com.curso.alsports.model.ProtecaoAutenticacao;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ProtecaoAutenticacaoRepository;

/**
 * Proteção persistente contra tentativas on-line de adivinhar senhas.
 *
 * <p>Todos os métodos participam da transação do caso de uso de autenticação.
 * O lock da linha é adquirido uma única vez, no início do login, e mantido até
 * o commit ou rollback da transação inteira. Os registros posteriores reutilizam
 * a entidade gerenciada pelo primeiro nível de cache, sem {@code REQUIRES_NEW}
 * e sem nova aquisição de lock.</p>
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

    /**
     * Adquire o lock de proteção antes de qualquer lock de conta. O upsert
     * atômico evita corrida de insert na primeira tentativa de um login.
     */
    @Override
    @Transactional
    public boolean permitirTentativa(String login, String enderecoOrigem) {
        String chave = calcularHashLogin(login);
        repository.criarSeNecessario(chave);

        ProtecaoAutenticacao protecao = repository
                .buscarParaAtualizacao(chave)
                .orElseThrow(() -> new IllegalStateException(
                        "Falha ao localizar contador de autenticação"));

        if (protecao.estaBloqueada(clock.instant())) {
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

    /**
     * Reutiliza a entidade já bloqueada por {@link #permitirTentativa} na mesma
     * transação. Não há {@code REQUIRES_NEW} nem novo {@code FOR UPDATE}.
     */
    @Override
    @Transactional
    public void registrarFalha(
            String login, String enderecoOrigem, Long alvoContaId) {

        String chave = calcularHashLogin(login);
        ProtecaoAutenticacao protecao = repository
                .findByChaveLogin(chave)
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

    /**
     * Reutiliza o mesmo lock de proteção adquirido antes da autenticação.
     */
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
