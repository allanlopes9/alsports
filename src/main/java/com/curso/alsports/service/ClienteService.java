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
import com.curso.alsports.model.Cliente;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.repository.ClienteRepository;
import com.curso.alsports.repository.PessoaRepository;

@Service
public class ClienteService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id",
            "telefone",
            "pessoa.nome");

    private final ClienteRepository repository;
    private final PessoaRepository pessoaRepository;

    public ClienteService(
            ClienteRepository repository,
            PessoaRepository pessoaRepository) {

        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public Cliente cadastrar(Long pessoaId, String telefone) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + pessoaId));

        if (!pessoa.isAtiva()) {
            throw new IllegalArgumentException(
                    "Pessoa deve estar ativa para receber o papel de cliente");
        }

        if (repository.existsByPessoaId(pessoaId)) {
            throw new RecursoDuplicadoException(
                    "Pessoa já possui cadastro de cliente");
        }

        return repository.save(new Cliente(pessoa, telefone));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return repository.findOneById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Cliente> pesquisar(String nome, Pageable pageable) {
        validarOrdenacao(pageable);

        Specification<Cliente> filtros =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (nome != null && !nome.isBlank()) {
            String trecho = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";

            filtros = filtros.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("pessoa").get("nome")),
                            trecho));
        }

        return repository.findAll(filtros, pageable);
    }

    @Transactional
    public Cliente atualizar(Long id, String telefone) {
        Cliente cliente = buscarPorId(id);
        cliente.alterarDados(telefone);
        return cliente;
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
