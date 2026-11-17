package com.curso.alsports.model;

public enum FuncaoColaborador {

    VENDEDOR,
    ESTOQUISTA,
    GERENTE,
    ADMINISTRATIVO;

    public boolean podeRealizarVenda() {
        return this == VENDEDOR || this == GERENTE;
    }
}
