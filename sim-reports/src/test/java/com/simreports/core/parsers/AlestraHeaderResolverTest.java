package com.simreports.core.parsers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlestraHeaderResolverTest {

    private final AlestraHeaderResolver resolver = new AlestraHeaderResolver();

    @ParameterizedTest
    @ValueSource(strings = {"LINEA", "linea", " Linea "})
    void reconoceLinea(String header) {
        assertEquals("LINEA", resolver.resolver(header).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tráfico MB", "TRÁFICO MB", "tráfico mb"})
    void reconoceTraficoMbComoConsumo(String header) {
        assertEquals("CONSUMO", resolver.resolver(header).orElseThrow());
    }

    @Test
    void reconoceVarianteSinAcentoComoSinonimoDefensivo() {
        assertEquals("CONSUMO", resolver.resolver("Trafico MB").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"IMSI", "MSISDN", "OTRA_COLUMNA", ""})
    void noReconoceColumnasQueNoSonCampoLogico(String header) {
        assertTrue(resolver.resolver(header).isEmpty());
    }
}
