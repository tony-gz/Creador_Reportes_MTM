package com.simreports.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumoMbTest {

    @Test
    void redondeaADosDecimalesComoExcel() {
        assertEquals(369.14, ConsumoMb.redondear(369.137366294861)); // no se regalan MB
        assertEquals(209.0, ConsumoMb.redondear(208.999));
        assertEquals(4321.99, ConsumoMb.redondear(4321.987654321));
        assertEquals(0.28, ConsumoMb.redondear(0.284));
        assertEquals(0.13, ConsumoMb.redondear(0.125)); // de 5 en adelante sube
        assertEquals(1.0, ConsumoMb.redondear(1.0));
        assertEquals(0.0, ConsumoMb.redondear(0.0));
    }

    @Test
    void conservaLosValoresQueYaTraenDosDecimales() {
        assertEquals(27.39, ConsumoMb.redondear(27.39));
        assertEquals(1170.94, ConsumoMb.redondear(1170.94));
    }

    @Test
    void ignoraElRuidoDePuntoFlotanteDeLasSumas() {
        assertEquals(0.3, ConsumoMb.redondear(0.1 + 0.2));
        assertEquals(28.34, ConsumoMb.redondear(28.339999999999996));
    }

    @Test
    void redondeaCadaValorDeUnMapa() {
        assertEquals(Map.of("a", 209.0, "b", 27.39), ConsumoMb.redondear(Map.of("a", 208.999, "b", 27.39)));
    }
}
