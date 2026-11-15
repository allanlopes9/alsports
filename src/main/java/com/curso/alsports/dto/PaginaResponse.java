
package com.curso.alsports.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public class PaginaResponse<T> {

    private List<T> conteudo;

    private int pagina;

    private int tamanho;

    private long totalElementos;

    private int totalPaginas;

    private boolean primeira;

    private boolean ultima;

    public PaginaResponse() {
    }

    public PaginaResponse(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

        this.conteudo = conteudo;
        this.pagina = pagina;
        this.tamanho = tamanho;
        this.totalElementos = totalElementos;
        this.totalPaginas = totalPaginas;
        this.primeira = pagina == 0;
        this.ultima = totalPaginas == 0 || pagina >= totalPaginas - 1;
    }

    public static <S, T> PaginaResponse<T> de(
        Page<S> pagina,
        Function<S, T> conversor) {

        PaginaResponse<T> resposta = new PaginaResponse<>(
            pagina.getContent()
                .stream()
                .map(conversor)
                .toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages());

        resposta.setPrimeira(pagina.isFirst());
        resposta.setUltima(pagina.isLast());

        return resposta;
    }

    public List<T> getConteudo() {
        return conteudo;
    }

    public void setConteudo(List<T> conteudo) {
        this.conteudo = conteudo;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = pagina;
    }

    public int getTamanho() {
        return tamanho;
    }

    public void setTamanho(int tamanho) {
        this.tamanho = tamanho;
    }

    public long getTotalElementos() {
        return totalElementos;
    }

    public void setTotalElementos(long totalElementos) {
        this.totalElementos = totalElementos;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public boolean isPrimeira() {
        return primeira;
    }

    public void setPrimeira(boolean primeira) {
        this.primeira = primeira;
    }

    public boolean isUltima() {
        return ultima;
    }

    public void setUltima(boolean ultima) {
        this.ultima = ultima;
    }
}
