package com.simreports.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizadorClaveTest {

    @Test
    void quitaSeparadoresDeFormatoDeUnaLinea() {
        assertEquals("5512345678", NormalizadorClave.normalizar("(55) 1234-5678"));
        assertEquals("5512345678", NormalizadorClave.normalizar("55 1234 5678"));
        assertEquals("5512345678", NormalizadorClave.normalizar("55 1234 5678"));
    }

    @Test
    void quitaComillasSimplesLiteralesYEspaciosInvisibles() {
        assertEquals("8952020221396597611", NormalizadorClave.normalizar(" '8952020221396597611' "));
        assertEquals("8952020221396597611", NormalizadorClave.normalizar("﻿8952020221396597611​"));
        assertEquals("8952020221396597611", NormalizadorClave.normalizar("' 8952020221396597611 '"));
    }

    @Test
    void quitaElSufijoDecimalCeroDeUnNumeroPasadoATexto() {
        assertEquals("5512345678", NormalizadorClave.normalizar("5512345678.0"));
        assertEquals("5512345678", NormalizadorClave.normalizar("5512345678.000"));
        // Un decimal real no se toca: no es una clave válida y no debe
        // convertirse en otra clave que sí exista.
        assertEquals("5512345678.5", NormalizadorClave.normalizar("5512345678.5"));
    }

    @Test
    void pasaAMayusculasElDigitoDeRellenoHexadecimal() {
        assertEquals("8952020221396597611F", NormalizadorClave.normalizar("8952020221396597611f"));
    }

    @Test
    void noQuitaPrefijosDePais() {
        assertEquals("+525512345678", NormalizadorClave.normalizar("+52 55 1234 5678"));
    }

    @Test
    void devuelveCadenaVaciaParaNullOSoloEspacios() {
        assertEquals("", NormalizadorClave.normalizar(null));
        assertEquals("", NormalizadorClave.normalizar("   "));
        assertEquals("", NormalizadorClave.normalizar("''"));
    }

    @Test
    void esIdempotente() {
        String una = NormalizadorClave.normalizar(" '(55) 1234-5678' ");
        assertEquals(una, NormalizadorClave.normalizar(una));
    }
}
