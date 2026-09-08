package com.curso.alsports.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoriaProduto {

    private final String nome;
    private Status status;
    private final List<Produto> produtos = new ArrayList<>();

    public CategoriaProduto(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da categoria ÃƒÂ© obrigatÃƒÂ³rio");
        }

        this.nome = nome.trim();
        this.status = Status.ATIVO;
    }

    public String getNome() {
        return nome;
    }

    public Status getStatus() {
        return status;
    }

    public List<Produto> getProdutos() {
        return Collections.unmodifiableList(produtos);
    }

    public void adicionarProduto(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("Produto nÃƒÂ£o pode ser nulo");
        }

        if (produtos.stream().anyMatch(p -> p.getNome().equals(produto.getNome()))) {
            throw new IllegalArgumentException("Ja existe um produto com este nome na categoria");
        }

        if (produto.getCategoria() != null && produto.getCategoria() != this) {
            throw new IllegalArgumentException("Produto jÃƒÂ¡ pertence a outra categoria");
        }

        produtos.add(produto);
        produto.associarA(this);
    }

    public void ativar() {
        this.status = Status.ATIVO;
    }

    public void inativar() {
        this.status = Status.INATIVO;
    }
}
