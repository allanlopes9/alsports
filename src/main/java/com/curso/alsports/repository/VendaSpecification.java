package com.curso.alsports.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.curso.alsports.model.StatusVenda;
import com.curso.alsports.model.Venda;

import jakarta.persistence.criteria.Predicate;

public final class VendaSpecification {

    private VendaSpecification() {
    }

    public static Specification<Venda> comFiltros(
            StatusVenda status,
            Long clienteId,
            LocalDate dataInicial,
            LocalDate dataFinal) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (status != null) {
                predicados.add(
                        criteriaBuilder.equal(root.get("status"), status));
            }

            if (clienteId != null) {
                predicados.add(
                        criteriaBuilder.equal(
                                root.get("cliente").get("id"), clienteId));
            }

            if (dataInicial != null) {
                predicados.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("dataVenda"),
                                dataInicial.atStartOfDay()));
            }

            if (dataFinal != null) {
                predicados.add(
                        criteriaBuilder.lessThan(
                                root.get("dataVenda"),
                                dataFinal.plusDays(1).atStartOfDay()));
            }

            return criteriaBuilder.and(
                    predicados.toArray(new Predicate[0]));
        };
    }
}
