package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;
import com.curso.alsports.service.ContaAcessoService;
import com.curso.alsports.service.RegistradorEventosSeguranca;

/**
 * Demonstra que a trilha não é decorativa: se a gravação do evento falhar, a
 * alteração sensível não pode ser confirmada. Quando a auditoria volta a
 * funcionar, alteração e evento são confirmados juntos.
 */
@SpringBootTest
@ActiveProfiles("test")
class Aula17AuditoriaObrigatoriaTest {

    private static final String ADMINISTRADOR = "auditoria.admin.didatico";

    private final PasswordEncoder passwordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ContaAcessoRepository contaRepository;

    @Autowired
    private ContaAcessoService contaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private RegistradorEventosSeguranca auditoria;

    private Long contaId;
    private Long pessoaId;

    @AfterEach
    void limparDadosExclusivos() {
        if (contaId != null) {
            jdbcTemplate.update(
                    "delete from evento_seguranca where ator_conta_id = ? "
                    + "or alvo_conta_id = ?", contaId, contaId);
            jdbcTemplate.update(
                    "delete from sessao_refresh_token where conta_acesso_id = ?",
                    contaId);
            jdbcTemplate.update("delete from conta_acesso where id = ?", contaId);
        }
        if (pessoaId != null) {
            jdbcTemplate.update("delete from pessoa where id = ?", pessoaId);
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void falhaNaAuditoriaDeveReverterStatusESucessoPosteriorDeveConfirmar() {
        Pessoa pessoa = pessoaRepository.save(new Pessoa(
                "Auditoria Obrigatória " + System.nanoTime(),
                "auditoria." + System.nanoTime() + "@example.com",
                cpfValido(System.nanoTime()),
                LocalDate.of(1991, 2, 3)));
        pessoaId = pessoa.getId();

        ContaAcesso conta = new ContaAcesso(
                pessoa,
                "auditoria.alvo." + System.nanoTime(),
                passwordEncoder.encode("senha-didatica-aula17"));
        conta.substituirPerfis(Set.of(PerfilAcesso.OPERADOR));
        contaId = contaRepository.saveAndFlush(conta).getId();

        long eventosIniciais = contarEventosStatus();

        doThrow(new IllegalStateException("auditoria obrigatória indisponível"))
                .when(auditoria).registrar(
                        eq(TipoEventoSeguranca.STATUS_ALTERADO),
                        any(), any(), any(), any(), any());

        assertThatThrownBy(() -> contaService.alterarStatus(
                contaId, StatusContaAcesso.BLOQUEADA, ADMINISTRADOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auditoria obrigatória");

        assertThat(statusBanco()).isEqualTo("ATIVA");
        assertThat(contarEventosStatus()).isEqualTo(eventosIniciais);

        doCallRealMethod().when(auditoria).registrar(
                eq(TipoEventoSeguranca.STATUS_ALTERADO),
                any(), any(), any(), any(), any());

        contaService.alterarStatus(
                contaId, StatusContaAcesso.BLOQUEADA, ADMINISTRADOR);

        assertThat(statusBanco()).isEqualTo("BLOQUEADA");
        assertThat(contarEventosStatus()).isEqualTo(eventosIniciais + 1);
    }

    private String statusBanco() {
        return jdbcTemplate.queryForObject(
                "select status from conta_acesso where id = ?",
                String.class, contaId);
    }

    private long contarEventosStatus() {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from evento_seguranca "
                + "where tipo = 'STATUS_ALTERADO' and alvo_conta_id = ?",
                Long.class, contaId);
        return total == null ? 0L : total;
    }

    private static String cpfValido(long semente) {
        String base = String.format(
                Locale.ROOT, "%09d", Math.abs(semente) % 1_000_000_000L);
        char primeiro = base.charAt(0);
        boolean repetidos = base.chars().allMatch(d -> d == primeiro);
        String nove = repetidos ? "1" + base.substring(1) : base;
        String dez = nove + digito(nove, 10);
        return dez + digito(dez, 11);
    }

    private static int digito(String base, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += Character.getNumericValue(base.charAt(i)) * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
