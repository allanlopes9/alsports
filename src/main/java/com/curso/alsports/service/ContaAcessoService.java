package com.curso.alsports.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.dto.ContaAcessoRequest;
import com.curso.alsports.exception.OperacaoSegurancaRecusadaException;
import com.curso.alsports.exception.RecursoDuplicadoException;
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

@Service
public class ContaAcessoService {

    private static final int LOGIN_MIN_CARACTERES = 3;
    private static final int LOGIN_MAX_CARACTERES = 80;
    private static final int SENHA_MIN_CARACTERES = 12;
    private static final int SENHA_MAX_CARACTERES = 72;
    private static final int SENHA_MAX_BYTES_UTF8 = 72;
    private static final Pattern LOGIN_PERMITIDO =
            Pattern.compile("^[A-Za-z0-9._-]+$");

    private static final RegistradorEventosSeguranca AUDITORIA_NEUTRA =
            (tipo, instante, ator, alvo, hash, origem) -> {
                // sem efeito: apenas para o construtor unitário legado
            };

    private final ContaAcessoRepository repository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistradorEventosSeguranca auditoria;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Construtor de compatibilidade dos testes unitários sem contexto Spring.
     */
    public ContaAcessoService(
            ContaAcessoRepository repository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder) {

        this(repository, pessoaRepository, passwordEncoder,
                AUDITORIA_NEUTRA, Clock.systemUTC());
    }

    @Autowired
    public ContaAcessoService(
            ContaAcessoRepository repository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder,
            RegistradorEventosSeguranca auditoria,
            Clock clock) {

        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso cadastrar(ContaAcessoRequest request) {
        return cadastrar(request, null, null);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso cadastrar(
            ContaAcessoRequest request,
            String loginAdministrador,
            String enderecoOrigem) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Dados da conta de acesso são obrigatórios");
        }

        if (request.pessoaId() == null) {
            throw new IllegalArgumentException("Pessoa é obrigatória");
        }

        String login = validarLogin(request.login());
        validarSenha(request.senha());

        Pessoa pessoa = pessoaRepository.findById(request.pessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + request.pessoaId()));

        if (!pessoa.isAtiva()) {
            throw new IllegalArgumentException(
                    "Pessoa inativa não pode possuir conta de acesso");
        }

        if (repository.existsByPessoaId(request.pessoaId())) {
            throw new RecursoDuplicadoException(
                    "Pessoa já possui conta de acesso");
        }

        if (repository.existsByLoginIgnoreCase(login)) {
            throw new RecursoDuplicadoException("Login já cadastrado");
        }

        String senhaHash = passwordEncoder.encode(request.senha());
        ContaAcesso salva = repository.save(
                new ContaAcesso(pessoa, login, senhaHash));

        auditoria.registrar(
                TipoEventoSeguranca.CONTA_CRIADA,
                clock.instant(),
                idAdministrador(loginAdministrador),
                salva.getId(),
                null,
                enderecoOrigem);

        return salva;
    }

    @Transactional(readOnly = true)
    public ContaAcesso buscarPorLogin(String login) {
        String loginNormalizado = login == null ? "" : login.trim();

        if (loginNormalizado.isEmpty()) {
            throw new RecursoNaoEncontradoException(
                    "Conta de acesso não encontrada");
        }

        return repository.findByLoginIgnoreCase(loginNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Conta de acesso não encontrada"));
    }

    @Transactional
    public void alterarSenha(
            String login,
            String senhaAtual,
            String novaSenha) {

        alterarSenha(login, senhaAtual, novaSenha, null);
    }

    @Transactional
    public void alterarSenha(
            String login,
            String senhaAtual,
            String novaSenha,
            String enderecoOrigem) {

        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new IllegalArgumentException("Senha atual é obrigatória");
        }

        validarSenha(novaSenha);

        ContaAcesso conta = buscarPorLogin(login);

        if (!passwordEncoder.matches(senhaAtual, conta.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual inválida");
        }

        if (passwordEncoder.matches(novaSenha, conta.getSenhaHash())) {
            throw new IllegalArgumentException(
                    "Nova senha deve ser diferente da senha atual");
        }

        conta.alterarSenha(passwordEncoder.encode(novaSenha));

        auditoria.registrar(
                TipoEventoSeguranca.SENHA_ALTERADA,
                clock.instant(),
                conta.getId(),
                conta.getId(),
                null,
                enderecoOrigem);
    }

    /**
     * Substitui os perfis de uma conta. A conta autenticada é identificada
     * pelo login do token e não por dados enviados no corpo da requisição.
     */
    @Transactional(noRollbackFor = OperacaoSegurancaRecusadaException.class)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarPerfis(
            Long contaId,
            Set<PerfilAcesso> novosPerfis,
            String loginAdministrador) {

        return alterarPerfis(contaId, novosPerfis, loginAdministrador, null);
    }

    @Transactional(noRollbackFor = OperacaoSegurancaRecusadaException.class)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarPerfis(
            Long contaId,
            Set<PerfilAcesso> novosPerfis,
            String loginAdministrador,
            String enderecoOrigem) {

        if (contaId == null) {
            throw new IllegalArgumentException(
                    "Identificador da conta é obrigatório");
        }

        if (novosPerfis == null || novosPerfis.isEmpty()) {
            throw new IllegalArgumentException(
                    "A conta deve possuir ao menos um perfil");
        }
        for (PerfilAcesso perfil : novosPerfis) {
            if (perfil == null) {
                throw new IllegalArgumentException("Perfil é obrigatório");
            }
        }

        ContaAcesso conta = carregarContaBloqueada(contaId);
        String administrador = validarLoginAdministrador(loginAdministrador);

        boolean removeAdministrador = conta.possuiPerfil(
                PerfilAcesso.ADMINISTRADOR)
                && !novosPerfis.contains(PerfilAcesso.ADMINISTRADOR);

        if (removeAdministrador
                && conta.getLogin().equalsIgnoreCase(administrador)) {
            recusar("O administrador não pode remover o próprio perfil "
                    + "ADMINISTRADOR", administrador, conta.getId(),
                    enderecoOrigem);
        }

        if (removeAdministrador
                && conta.podeAutenticar()
                && contarAdministradoresEfetivos() <= 1) {
            recusar("O sistema deve manter ao menos um administrador ativo",
                    administrador, conta.getId(), enderecoOrigem);
        }

        if (conta.getPerfis().equals(Set.copyOf(novosPerfis))) {
            return conta;
        }

        conta.substituirPerfis(novosPerfis);

        auditoria.registrar(
                TipoEventoSeguranca.PERFIS_ALTERADOS,
                clock.instant(),
                idAdministrador(administrador),
                conta.getId(),
                null,
                enderecoOrigem);

        return conta;
    }

    /**
     * Ativa ou bloqueia uma conta. O bloqueio da própria conta e a remoção do
     * último administrador efetivo são recusados para preservar a capacidade
     * de administração do sistema.
     */
    @Transactional(noRollbackFor = OperacaoSegurancaRecusadaException.class)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarStatus(
            Long contaId,
            StatusContaAcesso novoStatus,
            String loginAdministrador) {

        return alterarStatus(contaId, novoStatus, loginAdministrador, null);
    }

    @Transactional(noRollbackFor = OperacaoSegurancaRecusadaException.class)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarStatus(
            Long contaId,
            StatusContaAcesso novoStatus,
            String loginAdministrador,
            String enderecoOrigem) {

        if (contaId == null) {
            throw new IllegalArgumentException(
                    "Identificador da conta é obrigatório");
        }

        if (novoStatus == null) {
            throw new IllegalArgumentException("Status é obrigatório");
        }

        ContaAcesso conta = carregarContaBloqueada(contaId);
        String administrador = validarLoginAdministrador(loginAdministrador);

        if (novoStatus == StatusContaAcesso.BLOQUEADA) {
            if (conta.getLogin().equalsIgnoreCase(administrador)) {
                recusar("O administrador não pode bloquear a própria conta",
                        administrador, conta.getId(), enderecoOrigem);
            }

            if (conta.podeAutenticar()
                    && conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR)
                    && contarAdministradoresEfetivos() <= 1) {
                recusar("O sistema deve manter ao menos um administrador ativo",
                        administrador, conta.getId(), enderecoOrigem);
            }
        }

        if (conta.getStatus() == novoStatus) {
            return conta;
        }

        if (novoStatus == StatusContaAcesso.BLOQUEADA) {
            conta.bloquear();
        } else {
            conta.ativar();
        }

        auditoria.registrar(
                TipoEventoSeguranca.STATUS_ALTERADO,
                clock.instant(),
                idAdministrador(administrador),
                conta.getId(),
                null,
                enderecoOrigem);

        return conta;
    }

    /**
     * Protocolo: descoberta dos IDs, lock na ordem crescente, limpeza do
     * contexto e segunda leitura autoritativa dentro da mesma transação.
     *
     * <p>Todas as contas administrativas são bloqueadas porque a contagem do
     * último administrador é uma decisão global. Bloquear apenas a conta alvo
     * permitiria que duas transações removessem administradores diferentes ao
     * mesmo tempo, cada uma vendo a outra ainda ativa.</p>
     */
    private ContaAcesso carregarContaBloqueada(Long contaId) {
        if (entityManager == null) {
            return buscarDetalhada(contaId);
        }

        List<Long> ids = new java.util.ArrayList<>(List.of(contaId));
        ids.addAll(repository.buscarIdsAdministradores());
        ids = ids.stream().sorted().distinct().toList();

        for (Long id : ids) {
            repository.buscarPorIdParaAtualizacao(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Conta de acesso não encontrada: " + id));
        }

        entityManager.clear();
        return buscarDetalhada(contaId);
    }

    private void recusar(
            String mensagem,
            String loginAdministrador,
            Long alvoContaId,
            String enderecoOrigem) {

        auditoria.registrar(
                TipoEventoSeguranca.OPERACAO_RECUSADA,
                clock.instant(),
                idAdministrador(loginAdministrador),
                alvoContaId,
                null,
                enderecoOrigem);
        throw new OperacaoSegurancaRecusadaException(mensagem);
    }

    private Long idAdministrador(String loginAdministrador) {
        if (loginAdministrador == null || loginAdministrador.isBlank()) {
            return null;
        }
        return repository.findByLoginIgnoreCase(loginAdministrador.trim())
                .map(ContaAcesso::getId)
                .orElse(null);
    }

    private ContaAcesso buscarDetalhada(Long contaId) {
        return repository.findById(contaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Conta de acesso não encontrada: " + contaId));
    }

    private long contarAdministradoresEfetivos() {
        return repository.contarAdministradoresEfetivos(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA,
                Status.ATIVO);
    }

    private static String validarLoginAdministrador(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrador autenticado é obrigatório");
        }

        return login.trim();
    }

    static String validarLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login é obrigatório");
        }

        String loginNormalizado = login.trim();

        if (loginNormalizado.length() < LOGIN_MIN_CARACTERES
                || loginNormalizado.length() > LOGIN_MAX_CARACTERES) {

            throw new IllegalArgumentException(
                    "Login deve possuir entre 3 e 80 caracteres");
        }

        if (!LOGIN_PERMITIDO.matcher(loginNormalizado).matches()) {
            throw new IllegalArgumentException(
                    "Login deve conter apenas letras, números, ponto, "
                            + "sublinhado ou hífen");
        }

        return loginNormalizado;
    }

    static void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }

        if (senha.length() < SENHA_MIN_CARACTERES
                || senha.length() > SENHA_MAX_CARACTERES) {

            throw new IllegalArgumentException(
                    "Senha deve possuir entre 12 e 72 caracteres");
        }

        if (senha.getBytes(StandardCharsets.UTF_8).length
                > SENHA_MAX_BYTES_UTF8) {

            throw new IllegalArgumentException(
                    "Senha deve possuir no máximo 72 bytes em UTF-8");
        }
    }
}
