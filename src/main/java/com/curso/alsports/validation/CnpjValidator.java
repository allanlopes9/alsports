package com.curso.alsports.validation;

import com.curso.alsports.domain.DocumentoFiscal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<CnpjValido, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || DocumentoFiscal.cnpjValido(value);
    }
}
