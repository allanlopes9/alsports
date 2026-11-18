package com.curso.alsports.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.config.AdministradorInicialProperties;
import com.curso.alsports.domain.DocumentoFiscal;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Provisiona a primeira conta administrativa a partir de configuração
 * externa.
 *
 * <p>O processo é idempotente: se já existir administrador ativo, nada é
 * criado. Pessoa e conta são gravadas na mesma transação. A senha aberta
 * nunca é persistida nem registrada em log.</p>
 *
 * <p>Para reduzir a janela entre descoberta e decisão, os administradores já
 * existentes são descobertos por ID, bloqueados em ordem crescente e relidos
 * depois da limpeza do contexto. Ainda assim, em múltiplas instâncias sem um
 * administrador prévio, a coordenação deve ser feita pela plataforma.</p>
 */
@Service
public class AdministradorInicialService {

    private final ContaAcessoRepository contaAcessoRepository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public AdministradorInicialService(
            ContaAcessoRepository contaAcessoRepository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder,
            Clock clock) {

        this.contaAcessoRepository = contaAcessoRepository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public Optional<ContaAcesso> criarSeNecessario(
            AdministradorInicialProperties properties) {

        if (properties == null) {
            throw new IllegalArgumentException(
                    "Configuração do administrador inicial é obrigatória");
        }

        bloquearAdministradoresExistentes();

        if (contarAdministradoresAtivos() > 0) {
            return Optional.empty();
        }

        String nome = exigirTexto(
                properties.nome(), "Nome do administrador inicial é obrigatório");
        String email = exigirTexto(
                properties.email(), "E-mail do administrador inicial é obrigatório");
        String cpf = exigirTexto(
                properties.cpf(), "CPF do administrador inicial é obrigatório");
        String login = ContaAcessoService.validarLogin(properties.login());
        ContaAcessoService.validarSenha(properties.senha());

        if (!DocumentoFiscal.cpfValido(cpf)) {
            throw new IllegalArgumentException(
                    "CPF do administrador inicial é inválido");
        }

        if (contaAcessoRepository.existsByLoginIgnoreCase(login)) {
            throw new IllegalStateException(
                    "Login do administrador inicial já está em uso");
        }

        if (pessoaRepository.existsByCpf(cpf)) {
            throw new IllegalStateException(
                    "CPF do administrador inicial já está cadastrado");
        }

        if (pessoaRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException(
                    "E-mail do administrador inicial já está cadastrado");
        }

        Pessoa pessoa = pessoaRepository.save(new Pessoa(
                nome, email, cpf, LocalDate.now(clock)));

        ContaAcesso conta = new ContaAcesso(
                pessoa,
                login,
                passwordEncoder.encode(properties.senha()));
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);

        return Optional.of(contaAcessoRepository.save(conta));
    }

    /**
     * Descoberta de IDs, locks em ordem crescente e segunda leitura da contagem
     * depois de limpar o primeiro nível de cache.
     */
    private void bloquearAdministradoresExistentes() {
        if (entityManager == null) {
            return;
        }

        List<Long> ids = contaAcessoRepository.buscarIdsAdministradores()
                .stream()
                .sorted()
                .distinct()
                .toList();

        for (Long id : ids) {
            contaAcessoRepository.buscarPorIdParaAtualizacao(id)
                    .orElse(null);
        }

        entityManager.clear();
    }

    private long contarAdministradoresAtivos() {
        return contaAcessoRepository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA);
    }

    private static String exigirTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }

        return valor.trim();
    }
}
