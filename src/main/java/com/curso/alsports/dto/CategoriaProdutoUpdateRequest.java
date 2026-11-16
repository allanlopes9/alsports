package com.curso.alsports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para alterar uma categoria de produtos")
public record CategoriaProdutoUpdateRequest(

    @Schema(
        description = "Novo nome da categoria, com no máximo 120 caracteres",
        example = "Futebol"
    )
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome deve possuir no máximo 120 caracteres")
    String nome

) {
}
