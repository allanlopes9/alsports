package com.curso.alsports.dto;

import com.curso.alsports.model.Fornecedor;

public final class FornecedorMapper {

    private FornecedorMapper() {
    }

    public static FornecedorResponse toResponse(Fornecedor fornecedor) {
        FornecedorResponse response = new FornecedorResponse();
        response.setId(fornecedor.getId());
        response.setRazaoSocial(fornecedor.getRazaoSocial());
        response.setCnpj(fornecedor.getCnpj());
        response.setStatus(fornecedor.getStatus());
        return response;
    }

    public static Fornecedor toEntity(FornecedorRequest request) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setRazaoSocial(request.getRazaoSocial());
        fornecedor.setCnpj(request.getCnpj());
        return fornecedor;
    }
}