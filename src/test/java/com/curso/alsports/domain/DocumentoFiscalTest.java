package com.curso.alsports.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentoFiscalTest {

    @Test
    void deveAceitarCpfComDigitosVerificadoresValidos() {
        assertTrue(DocumentoFiscal.cpfValido("52998224725"));
        assertTrue(DocumentoFiscal.cpfValido("11144477735"));
    }

    @Test
    void deveRecusarCpfComDigitoVerificadorIncorreto() {
        assertFalse(DocumentoFiscal.cpfValido("52998224724"));
        assertFalse(DocumentoFiscal.cpfValido("12345678900"));
    }

    @Test
    void deveRecusarCpfComFormatoInvalido() {
        assertFalse(DocumentoFiscal.cpfValido(null));
        assertFalse(DocumentoFiscal.cpfValido(""));
        assertFalse(DocumentoFiscal.cpfValido("5299822472"));
        assertFalse(DocumentoFiscal.cpfValido("529982247250"));
        assertFalse(DocumentoFiscal.cpfValido("529.982.247-25"));
        assertFalse(DocumentoFiscal.cpfValido("5299822472A"));
    }

    @Test
    void deveRecusarCpfComTodosOsDigitosRepetidos() {
        assertFalse(DocumentoFiscal.cpfValido("00000000000"));
        assertFalse(DocumentoFiscal.cpfValido("11111111111"));
        assertFalse(DocumentoFiscal.cpfValido("99999999999"));
    }

    @Test
    void deveAceitarCnpjComDigitosVerificadoresValidos() {
        assertTrue(DocumentoFiscal.cnpjValido("11222333000181"));
        assertTrue(DocumentoFiscal.cnpjValido("11444777000161"));
    }

    @Test
    void deveRecusarCnpjComDigitoVerificadorIncorreto() {
        assertFalse(DocumentoFiscal.cnpjValido("11222333000182"));
        assertFalse(DocumentoFiscal.cnpjValido("00000000000001"));
    }

    @Test
    void deveRecusarCnpjComFormatoInvalido() {
        assertFalse(DocumentoFiscal.cnpjValido(null));
        assertFalse(DocumentoFiscal.cnpjValido(""));
        assertFalse(DocumentoFiscal.cnpjValido("1122233300018"));
        assertFalse(DocumentoFiscal.cnpjValido("112223330001810"));
        assertFalse(DocumentoFiscal.cnpjValido("11.222.333/0001-81"));
        assertFalse(DocumentoFiscal.cnpjValido("1122233300018A"));
    }

    @Test
    void deveRecusarCnpjComTodosOsDigitosRepetidos() {
        assertFalse(DocumentoFiscal.cnpjValido("00000000000000"));
        assertFalse(DocumentoFiscal.cnpjValido("11111111111111"));
    }
}
