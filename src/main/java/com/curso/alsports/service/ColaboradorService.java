package com.curso.alsports.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.Colaborador;
import com.curso.alsports.model.FuncaoColaborador;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.repository.ColaboradorRepository;
import com.curso.alsports.repository.PessoaRepository;

@Service
public class ColaboradorService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id",
            "matricula",
            "funcao",
            "dataAdmissao",
            "pessoa.nome");

    private final ColaboradorRepository repository;
    private final PessoaRepository pessoaRepository;

    public ColaboradorService(
            ColaboradorRepository repository,
            PessoaRepository pessoaRepository) {

        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public Colaborador cadastrar(
            Long pessoaId,
            String matricula,
            FuncaoColaborador funcao,
            LocalDate dataAdmissao) {

        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + pessoaId));

        if (!pessoa.isAtiva()) {
            throw new IllegalArgumentException(
                    "Pessoa deve estar ativa para receber o papel de colaborador");
        }

        if (repository.existsByPessoaId(pessoaId)) {
            throw new RecursoDuplicadoException(
                    "Pessoa já possui cadastro de colaborador");
        }

        String matriculaNormalizada = matricula == null
                ? null
                : matricula.trim();

        if (matriculaNormalizada != null
                && repository.existsByMatricula(matriculaNormalizada)) {
            throw new RecursoDuplicadoException(
                    "Matrícula já cadastrada: " + matriculaNormalizada);
        }

        return repository.save(new Colaborador(
                pessoa, matriculaNormalizada, funcao, dataAdmissao));
    }

    @Transactional(readOnly = true)
    public Colaborador buscarPorId(Long id) {
        return repository.findOneById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Colaborador não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Colaborador> pesquisar(
            String nome,
            FuncaoColaborador funcao,
            Pageable pageable) {

        validarOrdenacao(pageable);

        Specification<Colaborador> filtros =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (nome != null && !nome.isBlank()) {
            String trecho = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";

            filtros = filtros.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("pessoa").get("nome")),
                            trecho));
        }

        if (funcao != null) {
            filtros = filtros.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("funcao"), funcao));
        }

        return repository.findAll(filtros, pageable);
    }

    @Transactional
    public Colaborador atualizar(
            Long id,
            String matricula,
            FuncaoColaborador funcao) {

        Colaborador colaborador = buscarPorId(id);

        String matriculaNormalizada = matricula == null
                ? null
                : matricula.trim();

        if (matriculaNormalizada != null
                && repository.existsByMatriculaAndIdNot(matriculaNormalizada, id)) {
            throw new RecursoDuplicadoException(
                    "Matrícula já cadastrada: " + matriculaNormalizada);
        }

        colaborador.alterarDados(matriculaNormalizada, funcao);

        return colaborador;
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
