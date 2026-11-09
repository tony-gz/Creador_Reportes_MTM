package com.simreports.core.parsers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LectorNumeroTest {

    @Test
    void leeNumerosSimples() {
        assertEquals(1234.5, LectorNumero.parsear("1234.5"));
        assertEquals(-3.0, LectorNumero.parsear("-3"));
        assertEquals(0.5, LectorNumero.parsear(".5"));
        assertEquals(0.0015, LectorNumero.parsear("1.5E-3"));
        assertEquals(7.0, LectorNumero.parsear(" '7' "));
    }

    @Test
    void leeMilesConComaYDecimalConPunto() {
        assertEquals(1234.5, LectorNumero.parsear("1,234.5"));
        assertEquals(1234567.0, LectorNumero.parsear("1,234,567"));
    }

    @Test
    void leeMilesConPuntoYDecimalConComa() {
        assertEquals(1234.5, LectorNumero.parsear("1.234,5"));
        assertEquals(1234567.25, LectorNumero.parsear("1.234.567,25"));
    }

    @Test
    void leeDecimalConComaSinMiles() {
        assertEquals(1.5, LectorNumero.parsear("1,5"));
        assertEquals(0.004, LectorNumero.parsear("0,004"));
        assertEquals(12345.6, LectorNumero.parsear("12345,6"));
    }

    @Test
    void interpretaElCasoAmbiguoUnaComaYTresDigitosComoMiles() {
        assertEquals(1234.0, LectorNumero.parsear("1,234"));
    }

    @Test
    void aceptaEspaciosComoSeparadorDeMiles() {
        assertEquals(1234.5, LectorNumero.parsear("1 234,5"));
        assertEquals(1234.5, LectorNumero.parsear("1 234.5"));
    }

    @Test
    void rechazaTextoQueNoEsUnNumeroLegible() {
        assertNull(LectorNumero.parsear(null));
        assertNull(LectorNumero.parsear(""));
        assertNull(LectorNumero.parsear("NaN"));
        assertNull(LectorNumero.parsear("Infinity"));
        assertNull(LectorNumero.parsear("10d"));
        assertNull(LectorNumero.parsear("1,2,3"));
        assertNull(LectorNumero.parsear("abc"));
    }
}
