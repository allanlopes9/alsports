package com.curso.alsports.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.Status;
import com.curso.alsports.repository.PessoaRepository;

@Service
public class PessoaService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id",
            "nome",
            "email",
            "dataCadastro",
            "status");

    private final PessoaRepository repository;

    public PessoaService(PessoaRepository repository) {
        this.repository = repository;
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

    @Transactional
    public Pessoa alterarStatus(Long id, Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status obrigatório");
        }

        Pessoa pessoa = buscarPorId(id);

        if (status == Status.ATIVO) {
            pessoa.ativar();
        } else {
            pessoa.inativar();
        }

        return pessoa;
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
