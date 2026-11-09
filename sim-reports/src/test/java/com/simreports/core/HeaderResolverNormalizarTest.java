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
    void eliminaEspaciosInvisiblesQueExcelYLosCsvSuelenColar() {
        // Espacio de no separación (NBSP), típico al copiar/pegar en Excel.
        assertEquals("CONSUMO(MB)", HeaderResolver.normalizar("CONSUMO (MB)"));
        // BOM al inicio de un .csv guardado como "UTF-8 con BOM".
        assertEquals("ICCID", HeaderResolver.normalizar("﻿ICCID"));
        // Espacio de ancho cero.
        assertEquals("LINEA", HeaderResolver.normalizar("LI​NEA"));
        // NBSP al final, que trim() por sí solo no quita.
        assertEquals("ICCID", HeaderResolver.normalizar("ICCID "));
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
