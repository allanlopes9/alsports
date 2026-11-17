package com.curso.alsports.validation;

import com.curso.alsports.domain.DocumentoFiscal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || DocumentoFiscal.cpfValido(value);
    }
}
