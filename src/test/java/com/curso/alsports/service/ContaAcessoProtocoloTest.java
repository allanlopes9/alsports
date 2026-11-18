package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.util.ReflectionTestUtils;

import com.curso.alsports.exception.OperacaoSegurancaRecusadaException;
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

/**
 * Valida o protocolo de locks e segunda leitura das operações que decidem a
 * invariante do último administrador.
 */
@ExtendWith(MockitoExtension.class)
class ContaAcessoProtocoloTest {

    private static final Instant AGORA =
            Instant.parse("2026-11-17T12:00:00Z");

    @Mock
    private ContaAcessoRepository repository;

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private RegistradorEventosSeguranca auditoria;

    @Mock
    private EntityManager entityManager;

    private ContaAcessoService service;

    @BeforeEach
    void setUp() {
        service = new ContaAcessoService(
                repository,
                pessoaRepository,
                PasswordEncoderFactories.createDelegatingPasswordEncoder(),
                auditoria,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @Test
    void deveBloquearAdministradoresEmOrdemCrescenteAntesDaSegundaLeitura() {
        ContaAcesso alvo = conta(30L, "operador.teste", false);
        Map<Long, ContaAcesso> contas = Map.of(
                10L, conta(10L, "admin.um", true),
                20L, conta(20L, "admin.dois", true),
                30L, alvo);

        when(repository.buscarIdsAdministradores())
                .thenReturn(List.of(20L, 10L));
        prepararLocks(contas);
        when(repository.findById(30L)).thenReturn(Optional.of(alvo));

        ContaAcesso alterada = service.alterarStatus(
                30L, StatusContaAcesso.BLOQUEADA, "admin.teste", "10.0.0.1");

        assertThat(alterada.getStatus())
                .isEqualTo(StatusContaAcesso.BLOQUEADA);

        InOrder ordem = inOrder(repository, entityManager, auditoria);
        ordem.verify(repository).buscarPorIdParaAtualizacao(10L);
        ordem.verify(repository).buscarPorIdParaAtualizacao(20L);
        ordem.verify(repository).buscarPorIdParaAtualizacao(30L);
        ordem.verify(entityManager).clear();
        ordem.verify(repository).findById(30L);
        ordem.verify(auditoria).registrar(
                TipoEventoSeguranca.STATUS_ALTERADO,
                AGORA,
                null,
                30L,
                null,
                "10.0.0.1");
    }

    @Test
    void estadoJaAplicadoNaoDeveRegistrarEventoDuplicado() {
        ContaAcesso alvo = conta(30L, "operador.teste", false);
        alvo.bloquear();
        when(repository.buscarPorIdParaAtualizacao(30L))
                .thenReturn(Optional.of(alvo));
        when(repository.findById(30L)).thenReturn(Optional.of(alvo));

        ContaAcesso alterada = service.alterarStatus(
                30L, StatusContaAcesso.BLOQUEADA, "admin.teste", null);

        assertThat(alterada).isSameAs(alvo);
        verifyNoInteractions(auditoria);
    }

    @Test
    void recusaDeUltimoAdministradorDeveSerAuditadaSemAlterarStatus() {
        ContaAcesso alvo = conta(10L, "ultimo.admin", true);
        when(repository.buscarIdsAdministradores()).thenReturn(List.of(10L));
        when(repository.buscarPorIdParaAtualizacao(10L))
                .thenReturn(Optional.of(alvo));
        when(repository.findById(10L)).thenReturn(Optional.of(alvo));
        when(repository.contarAdministradoresEfetivos(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA,
                Status.ATIVO)).thenReturn(1L);

        assertThatThrownBy(() -> service.alterarStatus(
                10L,
                StatusContaAcesso.BLOQUEADA,
                "outro.admin",
                "10.0.0.2"))
                .isInstanceOf(OperacaoSegurancaRecusadaException.class)
                .hasMessage("O sistema deve manter ao menos um administrador ativo");

        assertThat(alvo.getStatus()).isEqualTo(StatusContaAcesso.ATIVA);
        verify(auditoria).registrar(
                TipoEventoSeguranca.OPERACAO_RECUSADA,
                AGORA,
                null,
                10L,
                null,
                "10.0.0.2");
    }

    @Test
    void devePermitirBloqueioQuandoExistirOutroAdministradorEfetivo() {
        ContaAcesso alvo = conta(10L, "admin.um", true);
        when(repository.buscarIdsAdministradores()).thenReturn(List.of(10L));
        when(repository.buscarPorIdParaAtualizacao(10L))
                .thenReturn(Optional.of(alvo));
        when(repository.findById(10L)).thenReturn(Optional.of(alvo));
        when(repository.contarAdministradoresEfetivos(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA,
                Status.ATIVO)).thenReturn(2L);

        service.alterarStatus(
                10L, StatusContaAcesso.BLOQUEADA, "outro.admin", null);

        assertThat(alvo.getStatus())
                .isEqualTo(StatusContaAcesso.BLOQUEADA);
        verify(auditoria).registrar(
                TipoEventoSeguranca.STATUS_ALTERADO,
                AGORA,
                null,
                10L,
                null,
                null);
    }

    @Test
    void contaInexistenteDeveFalharSemAuditar() {
        when(repository.buscarPorIdParaAtualizacao(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterarStatus(
                99L, StatusContaAcesso.BLOQUEADA, "admin.teste", null))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Conta de acesso não encontrada: 99");

        verifyNoInteractions(auditoria);
    }

    private void prepararLocks(Map<Long, ContaAcesso> contas) {
        Map<Long, ContaAcesso> copies = new HashMap<>(contas);
        when(repository.buscarPorIdParaAtualizacao(anyLong()))
                .thenAnswer(invocacao -> Optional.ofNullable(
                        copies.get(invocacao.getArgument(0, Long.class))));
    }

    private static ContaAcesso conta(
            Long id, String login, boolean administrador) {

        Pessoa pessoa = new Pessoa(
                "Pessoa " + login,
                login + "@example.com",
                "52998224725",
                LocalDate.of(2026, 1, 1));
        ContaAcesso conta = new ContaAcesso(
                pessoa, login, "{bcrypt}hash");
        ReflectionTestUtils.setField(conta, "id", id);
        if (administrador) {
            conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        }
        return conta;
    }
}
