package com.curso.alsports.service;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class PessoaService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id",
            "nome",
            "email",
            "dataCadastro",
            "status");

    private final PessoaRepository repository;
    private final ContaAcessoRepository contaRepository;
    private final RegistradorEventosSeguranca auditoria;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public PessoaService(
            PessoaRepository repository,
            ContaAcessoRepository contaRepository,
            RegistradorEventosSeguranca auditoria,
            Clock clock) {

        this.repository = repository;
        this.contaRepository = contaRepository;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional
    public Pessoa cadastrar(Pessoa pessoa) {
        validarUnicidade(pessoa.getCpf(), pessoa.getEmail(), null);
        return repository.save(pessoa);
    }

    @Transactional(readOnly = true)
    public Pessoa buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Pessoa> pesquisar(
            String nome,
            Status status,
            Pageable pageable) {

        validarOrdenacao(pageable);

        Specification<Pessoa> filtros =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (nome != null && !nome.isBlank()) {
            String trecho = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";

            filtros = filtros.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("nome")), trecho));
        }

        if (status != null) {
            filtros = filtros.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }

        return repository.findAll(filtros, pageable);
    }

    @Transactional
    public Pessoa atualizar(Long id, String nome, String email, String cpf) {
        Pessoa pessoa = buscarPorId(id);

        String cpfNormalizado = cpf == null ? null : cpf.trim();
        String emailNormalizado = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);

        validarUnicidade(cpfNormalizado, emailNormalizado, id);
        pessoa.alterarDados(nome, emailNormalizado, cpfNormalizado);

        return pessoa;
    }

    @Transactional(noRollbackFor = OperacaoSegurancaRecusadaException.class)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Pessoa alterarStatus(Long id, Status status) {
        return alterarStatus(id, status, null, null);
    }

    /**
     * Altera o status da pessoa sob o protocolo conta -&gt; pessoa, com
     * descoberta de IDs, locks em ordem crescente, limpeza do contexto e
     * segunda leitura autoritativa.
     *
     * <p>Inativar a pessoa de um administrador efetivo é recusado quando isso
     * deixaria o sistema sem administrador capaz de autenticar.</p>
     */
    @Transactional(noRollbackFor = OperacaoSegurancaRecusadaException.class)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Pessoa alterarStatus(
            Long id,
            Status status,
            String loginAdministrador,
            String enderecoOrigem) {

        if (id == null) {
            throw new IllegalArgumentException("Identificador da pessoa é obrigatório");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status obrigatório");
        }

        List<Long> vinculadas = contaRepository.buscarIdsPorPessoaId(id)
                .stream().sorted().distinct().toList();

        // A decisão sobre o último administrador é global: todas as contas
        // administrativas participam do mesmo conjunto de locks.
        List<Long> contasParaBloquear = new java.util.ArrayList<>(vinculadas);
        contasParaBloquear.addAll(contaRepository.buscarIdsAdministradores());
        contasParaBloquear = contasParaBloquear.stream()
                .sorted().distinct().toList();

        for (Long contaId : contasParaBloquear) {
            contaRepository.buscarPorIdParaAtualizacao(contaId)
                    .orElse(null);
        }

        repository.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + id));

        if (entityManager != null) {
            entityManager.clear();
        }

        Pessoa pessoa = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + id));
        List<Long> contasVinculadas = contaRepository.buscarIdsPorPessoaId(id)
                .stream().sorted().distinct().toList();
        ContaAcesso conta = contasVinculadas.isEmpty()
                ? null
                : contaRepository.findById(contasVinculadas.get(0))
                        .orElseThrow(() -> new RecursoNaoEncontradoException(
                                "Conta de acesso não encontrada: "
                                        + contasVinculadas.get(0)));

        if (pessoa.getStatus() == status) {
            return pessoa;
        }

        Long alvoContaId = conta == null ? null : conta.getId();

        if (status == Status.INATIVO
                && conta != null
                && conta.podeAutenticar()
                && conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR)
                && contarAdministradoresEfetivos() <= 1) {

            auditoria.registrar(
                    TipoEventoSeguranca.OPERACAO_RECUSADA,
                    clock.instant(),
                    idAdministrador(loginAdministrador),
                    alvoContaId,
                    null,
                    enderecoOrigem);
            throw new OperacaoSegurancaRecusadaException(
                    "O sistema deve manter ao menos um administrador ativo");
        }

        if (status == Status.ATIVO) {
            pessoa.ativar();
        } else {
            pessoa.inativar();
        }

        auditoria.registrar(
                TipoEventoSeguranca.STATUS_ALTERADO,
                clock.instant(),
                idAdministrador(loginAdministrador),
                alvoContaId,
                null,
                enderecoOrigem);

        return pessoa;
    }

    private Long idAdministrador(String loginAdministrador) {
        if (loginAdministrador == null || loginAdministrador.isBlank()) {
            return null;
        }
        return contaRepository.findByLoginIgnoreCase(loginAdministrador.trim())
                .map(ContaAcesso::getId)
                .orElse(null);
    }

    private long contarAdministradoresEfetivos() {
        return contaRepository.contarAdministradoresEfetivos(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA,
                Status.ATIVO);
    }

    private void validarUnicidade(String cpf, String email, Long idAtual) {
        boolean cpfDuplicado = idAtual == null
                ? repository.existsByCpf(cpf)
                : repository.existsByCpfAndIdNot(cpf, idAtual);

        boolean emailDuplicado = idAtual == null
                ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, idAtual);

        if (cpfDuplicado) {
            throw new RecursoDuplicadoException("CPF já cadastrado");
        }

        if (emailDuplicado) {
            throw new RecursoDuplicadoException("E-mail já cadastrado");
        }
    }

    private void validarOrdenacao(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "A paginação deve ser informada");
        }

        pageable.getSort().forEach(ordem -> {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw new IllegalArgumentException(
                        "Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
    }
}
