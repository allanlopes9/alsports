package com.curso.alsports.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.Fornecedor;
import com.curso.alsports.model.Status;
import com.curso.alsports.repository.FornecedorRepository;

@Service
public class FornecedorService {

    private final FornecedorRepository repository;

    public FornecedorService(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Fornecedor salvar(Fornecedor fornecedor) {

        if (repository.existsByCnpj(fornecedor.getCnpj())) {
            throw new RecursoDuplicadoException(
                    "Fornecedor com CNPJ já cadastrado: " + fornecedor.getCnpj());
        }

        if (fornecedor.getStatus() == null) {
            fornecedor.setStatus(Status.ATIVO);
        }

        return repository.save(fornecedor);
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Fornecedor não encontrado: " + id));
    }
}
