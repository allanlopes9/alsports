package com.curso.alsports.service;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.AdministradorRecuperacaoProperties;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Status;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Recuperação operacional de administrador cuja pessoa foi inativada.
 *
 * <p>O mecanismo não cria conta, não altera senha, perfis ou status da conta.
 * Reativa exclusivamente a pessoa vinculada a uma conta administrativa ativa
 * já existente, sob os locks conta -&gt; pessoa e com segunda leitura
 * autoritativa.</p>
 */
@Service
public class AdministradorRecuperacaoService {

    private static final String ORIGEM = "RECUPERACAO_ADMINISTRATIVA";

    private final ContaAcessoRepository contaRepository;
    private final PessoaRepository pessoaRepository;
    private final RegistradorEventosSeguranca auditoria;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public AdministradorRecuperacaoService(
            ContaAcessoRepository contaRepository,
            PessoaRepository pessoaRepository,
            RegistradorEventosSeguranca auditoria,
            Clock clock) {

        this.contaRepository = Objects.requireNonNull(contaRepository);
        this.pessoaRepository = Objects.requireNonNull(pessoaRepository);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * @return {@code true} quando a pessoa foi reativada; {@code false} quando
     *         já estava ativa (idempotente).
     */
    @Transactional
    public boolean recuperar(AdministradorRecuperacaoProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException(
                    "Configuração de recuperação administrativa é obrigatória");
        }
        if (!properties.enabled()) {
            throw new IllegalStateException(
                    "Recuperação administrativa está desabilitada");
        }

        String login = exigirTexto(properties.login(),
                "Login de recuperação é obrigatório");
        String confirmacao = exigirTexto(properties.confirm(),
                "Confirmação de recuperação é obrigatória");
        if (!login.equals(confirmacao)) {
            throw new IllegalArgumentException(
                    "Login e confirmação de recuperação divergem");
        }

        Long contaId = contaRepository.buscarIdPorLogin(login);
        if (contaId == null) {
            throw new RecursoNaoEncontradoException(
                    "Conta administrativa não encontrada para recuperação");
        }

        Long pessoaId = bloquearConta(contaId);

        pessoaRepository.buscarPorIdParaAtualizacao(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + pessoaId));

        if (entityManager != null) {
            entityManager.clear();
        }

        ContaAcesso conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Conta administrativa não encontrada: " + contaId));
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + pessoaId));

        if (!conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR)
                || conta.getStatus() != StatusContaAcesso.ATIVA) {
            throw new IllegalStateException(
                    "A recuperação exige uma conta administrativa ativa");
        }

        if (pessoa.isAtiva()) {
            return false;
        }

        pessoa.ativar();
        auditoria.registrar(
                TipoEventoSeguranca.STATUS_ALTERADO,
                clock.instant(),
                null,
                conta.getId(),
                null,
                ORIGEM);
        return true;
    }

    private Long bloquearConta(Long contaId) {
        List<Long> ids = List.of(contaId).stream().sorted().toList();
        Long pessoaId = null;
        for (Long id : ids) {
            ContaAcesso conta = contaRepository.buscarPorIdParaAtualizacao(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Conta administrativa não encontrada: " + id));
            pessoaId = conta.getPessoa().getId();
        }
        return pessoaId;
    }

    private static String exigirTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }
}
