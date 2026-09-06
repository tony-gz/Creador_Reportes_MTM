package com.simreports.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderResolverNormalizarTest {

    @Test
    void convierteAMayusculas() {
        assertEquals("LINEA", HeaderResolver.normalizar("linea"));
        assertEquals("ICCID", HeaderResolver.normalizar("Iccid"));
    }

    @Test
    void eliminaEspaciosAlInicioYFinal() {
        assertEquals("ICCID", HeaderResolver.normalizar("  ICCID  "));
    }

    @Test
    void eliminaTodosLosEspaciosInternosNoSoloLosColapsa() {
        // Caso real de Fase 0: "CONSUMO (MB)" con un solo espacio antes
        // del paréntesis debe matchear con "CONSUMO(MB)".
        assertEquals("CONSUMO(MB)", HeaderResolver.normalizar("CONSUMO (MB)"));
        // Y también si trajera varios espacios seguidos.
        assertEquals("CONSUMO(MB)", HeaderResolver.normalizar("CONSUMO   (MB)"));
    }

    @Test
    void devuelveCadenaVaciaParaNull() {
        assertEquals("", HeaderResolver.normalizar(null));
    }

    @Test
    void devuelveCadenaVaciaParaSoloEspacios() {
        assertEquals("", HeaderResolver.normalizar("   "));
    }
}
