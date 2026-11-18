package com.curso.alsports.model;

import java.time.Instant;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento estruturado de segurança.
 *
 * <p>Identificadores de conta são valores históricos, sem associação JPA: a
 * trilha sobrevive ao ciclo de vida da conta. O registro não possui campo de
 * mensagem livre, senha, token ou login em texto aberto.</p>
 */
@Entity
@Table(name = "evento_seguranca")
public class EventoSeguranca {

    private static final int TAMANHO_MAXIMO_ORIGEM = 45;
    private static final Pattern HASH_LOGIN = Pattern.compile("[0-9a-f]{64}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoEventoSeguranca tipo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @Column(name = "ator_conta_id")
    private Long atorContaId;

    @Column(name = "alvo_conta_id")
    private Long alvoContaId;

    @Column(name = "identificador_hash", length = 64)
    private String identificadorHash;

    @Column(name = "endereco_origem", length = 45)
    private String enderecoOrigem;

    protected EventoSeguranca() {
        // JPA
    }

    private EventoSeguranca(
            TipoEventoSeguranca tipo,
            Instant ocorridoEm,
            Long atorContaId,
            Long alvoContaId,
            String identificadorHash,
            String enderecoOrigem) {

        validar(tipo, ocorridoEm, atorContaId, alvoContaId,
                identificadorHash, enderecoOrigem);
        this.tipo = tipo;
        this.ocorridoEm = ocorridoEm;
        this.atorContaId = atorContaId;
        this.alvoContaId = alvoContaId;
        this.identificadorHash = identificadorHash;
        this.enderecoOrigem = enderecoOrigem;
    }

    public static EventoSeguranca criar(
            TipoEventoSeguranca tipo,
            Instant ocorridoEm,
            Long atorContaId,
            Long alvoContaId,
            String identificadorHash,
            String enderecoOrigem) {

        return new EventoSeguranca(tipo, ocorridoEm, atorContaId, alvoContaId,
                identificadorHash, enderecoOrigem);
    }

    private static void validar(
            TipoEventoSeguranca tipo,
            Instant ocorridoEm,
            Long atorContaId,
            Long alvoContaId,
            String identificadorHash,
            String enderecoOrigem) {

        if (tipo == null) {
            throw new IllegalArgumentException("Tipo do evento é obrigatório");
        }
        if (ocorridoEm == null) {
            throw new IllegalArgumentException(
                    "Instante do evento é obrigatório");
        }
        validarId(atorContaId, "Identificador do autor");
        validarId(alvoContaId, "Identificador da conta afetada");

        if (identificadorHash != null
                && !HASH_LOGIN.matcher(identificadorHash).matches()) {
            throw new IllegalArgumentException(
                    "Hash do login deve possuir 64 caracteres hexadecimais");
        }
        validarOrigem(enderecoOrigem);
    }

    private static void validarId(Long valor, String nome) {
        if (valor != null && valor <= 0) {
            throw new IllegalArgumentException(nome + " deve ser positivo");
        }
    }

    /**
     * Rejeita CR/LF para impedir injeção de linhas em logs textuais e limita
     * o endereço a 45 caracteres (IPv6 completo incluído).
     */
    public static void validarOrigem(String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (valor.length() > TAMANHO_MAXIMO_ORIGEM
                || valor.contains("\r")
                || valor.contains("\n")) {
            throw new IllegalArgumentException("Endereço de origem inválido");
        }
    }

    public Long getId() {
        return id;
    }

    public TipoEventoSeguranca getTipo() {
        return tipo;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public Long getAtorContaId() {
        return atorContaId;
    }

    public Long getAlvoContaId() {
        return alvoContaId;
    }

    public String getIdentificadorHash() {
        return identificadorHash;
    }

    public String getEnderecoOrigem() {
        return enderecoOrigem;
    }

    @Override
    public String toString() {
        return "EventoSeguranca[tipo=" + tipo
                + ", ocorridoEm=" + ocorridoEm
                + ", atorContaId=" + atorContaId
                + ", alvoContaId=" + alvoContaId
                + ", enderecoOrigem=" + enderecoOrigem + "]";
    }
}
