package com.curso.alsports.dto;

import com.curso.alsports.model.Status;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "Status é obrigatório")
        Status status) {
}
