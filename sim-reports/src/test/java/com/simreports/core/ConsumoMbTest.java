package com.simreports.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumoMbTest {

    @Test
    void dejaDosDecimalesSinRedondear() {
        assertEquals(208.99, ConsumoMb.truncar(208.999));
        assertEquals(4321.98, ConsumoMb.truncar(4321.987654321));
        assertEquals(0.28, ConsumoMb.truncar(0.284));
        assertEquals(1.0, ConsumoMb.truncar(1.0));
        assertEquals(0.0, ConsumoMb.truncar(0.0));
    }

    @Test
    void conservaLosValoresQueYaTraenDosDecimales() {
        // 27.39 * 100 en double es 2738.9999…; truncar ingenuamente daría 27.38.
        assertEquals(27.39, ConsumoMb.truncar(27.39));
        assertEquals(1170.94, ConsumoMb.truncar(1170.94));
    }

    @Test
    void ignoraElRuidoDePuntoFlotanteDeLasSumas() {
        assertEquals(0.3, ConsumoMb.truncar(0.1 + 0.2));
        assertEquals(28.34, ConsumoMb.truncar(28.339999999999996));
    }

    @Test
    void truncaCadaValorDeUnMapa() {
        assertEquals(Map.of("a", 208.99, "b", 27.39), ConsumoMb.truncar(Map.of("a", 208.999, "b", 27.39)));
    }
}
