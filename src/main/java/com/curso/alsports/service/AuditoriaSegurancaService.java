package com.curso.alsports.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.dto.EventoSegurancaResponse;
import com.curso.alsports.model.EventoSeguranca;
import com.curso.alsports.model.TipoEventoSeguranca;
import com.curso.alsports.repository.EventoSegurancaRepository;

/**
 * Escrita e consulta da trilha de eventos de segurança.
 *
 * <p>A gravação participa da transação da ação sensível: se o evento não
 * puder ser persistido, a alteração correspondente também reverte. A consulta
 * exige ADMINISTRADOR tanto no controlador quanto aqui.</p>
 */
@Service
public class AuditoriaSegurancaService implements RegistradorEventosSeguranca {

    private static final Sort ORDEM_ESTAVEL =
            Sort.by(Sort.Direction.DESC, "ocorridoEm")
                    .and(Sort.by(Sort.Direction.DESC, "id"));

    private final EventoSegurancaRepository repository;

    public AuditoriaSegurancaService(EventoSegurancaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void registrar(
            TipoEventoSeguranca tipo,
            Instant ocorridoEm,
            Long atorContaId,
            Long alvoContaId,
            String identificadorHash,
            String enderecoOrigem) {

        repository.save(EventoSeguranca.criar(
                tipo, ocorridoEm, atorContaId, alvoContaId,
                identificadorHash, enderecoOrigem));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Page<EventoSegurancaResponse> consultar(
            TipoEventoSeguranca tipo, int page, int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Paginação inválida: page deve ser >= 0 e size entre 1 e 100");
        }

        Pageable paginacao = PageRequest.of(page, size, ORDEM_ESTAVEL);
        Page<EventoSeguranca> eventos = tipo == null
                ? repository.consultarTodos(paginacao)
                : repository.consultarPorTipo(tipo, paginacao);

        return eventos.map(this::paraResponse);
    }

    private EventoSegurancaResponse paraResponse(EventoSeguranca evento) {
        return new EventoSegurancaResponse(
                evento.getId(),
                evento.getTipo(),
                evento.getOcorridoEm(),
                evento.getAtorContaId(),
                evento.getAlvoContaId(),
                evento.getEnderecoOrigem());
    }
}
