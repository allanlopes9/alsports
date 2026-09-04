package com.curso.alsports.dto;

import com.curso.alsports.model.Fornecedor;

import org.springframework.stereotype.Component;

@Component
public class FornecedorMapper {

    public FornecedorResponse toResponse(Fornecedor fornecedor) {
        FornecedorResponse response = new FornecedorResponse();
        response.setId(fornecedor.getId());
        response.setRazaoSocial(fornecedor.getRazaoSocial());
        response.setCnpj(fornecedor.getCnpj());
        response.setStatus(fornecedor.getStatus());
        return response;
    }

    public Fornecedor toEntity(FornecedorRequest request) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setRazaoSocial(request.getRazaoSocial());
        fornecedor.setCnpj(request.getCnpj());
        return fornecedor;
    }
}