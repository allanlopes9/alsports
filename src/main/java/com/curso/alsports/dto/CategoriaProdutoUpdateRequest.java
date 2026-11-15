package com.curso.alsports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaProdutoUpdateRequest(

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome deve possuir no máximo 120 caracteres")
    String nome

) {
}
