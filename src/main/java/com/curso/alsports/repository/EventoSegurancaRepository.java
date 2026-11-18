package com.curso.alsports.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.curso.alsports.model.EventoSeguranca;
import com.curso.alsports.model.TipoEventoSeguranca;

public interface EventoSegurancaRepository
        extends JpaRepository<EventoSeguranca, Long> {

    @Query("select evento from EventoSeguranca evento")
    Page<EventoSeguranca> consultarTodos(Pageable pageable);

    @Query("""
            select evento
            from EventoSeguranca evento
            where evento.tipo = :tipo
            """)
    Page<EventoSeguranca> consultarPorTipo(
            @Param("tipo") TipoEventoSeguranca tipo,
            Pageable pageable);
}
