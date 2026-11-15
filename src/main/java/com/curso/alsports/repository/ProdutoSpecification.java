
package com.curso.alsports.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.curso.alsports.model.Produto;

import jakarta.persistence.criteria.Predicate;

public final class ProdutoSpecification {

    private ProdutoSpecification() {
    }

    public static Specification<Produto> comFiltros(
        String nome,
        Boolean ativo,
        Long categoriaId,
        Long fornecedorId,
        Integer quantidadeMinima,
        Integer quantidadeMaxima,
        Boolean abaixoEstoqueMinimo) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (nome != null && !nome.isBlank()) {
                predicados.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("nome")),
                        "%" + nome.trim().toLowerCase(Locale.ROOT) + "%"));
            }

            if (ativo != null) {
                predicados.add(
                    criteriaBuilder.equal(root.get("ativo"), ativo));
            }

            if (categoriaId != null) {
                predicados.add(
                    criteriaBuilder.equal(
                        root.get("categoria").get("id"), categoriaId));
            }

            if (fornecedorId != null) {
                predicados.add(
                    criteriaBuilder.equal(
                        root.get("fornecedor").get("id"), fornecedorId));
            }

            if (quantidadeMinima != null) {
                predicados.add(
                    criteriaBuilder.greaterThanOrEqualTo(
                        root.get("quantidade"), quantidadeMinima));
            }

            if (quantidadeMaxima != null) {
                predicados.add(
                    criteriaBuilder.lessThanOrEqualTo(
                        root.get("quantidade"), quantidadeMaxima));
            }

            if (Boolean.TRUE.equals(abaixoEstoqueMinimo)) {
                predicados.add(
                    criteriaBuilder.lessThan(
                        criteriaBuilder.toBigDecimal(
                            root.<Integer>get("quantidade")),
                        root.<java.math.BigDecimal>get("estoqueMinimo")));
            }

            return criteriaBuilder.and(predicados.toArray(new Predicate[0]));
        };
    }
}
