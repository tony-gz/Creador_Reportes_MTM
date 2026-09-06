package com.simreports.core.parsers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttHeaderResolverTest {

    private final AttHeaderResolver resolver = new AttHeaderResolver();

    @ParameterizedTest
    @ValueSource(strings = {"ICCID", "iccid", " Iccid "})
    void reconoceIccid(String header) {
        assertEquals("ICCID", resolver.resolver(header).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Consumo_MB", "CONSUMO_MB", "consumo_mb"})
    void reconoceConsumoMb(String header) {
        assertEquals("CONSUMO", resolver.resolver(header).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USO (MB)", "MSISDN", "OTRA_COLUMNA", ""})
    void noReconoceColumnasQueNoSonCampoLogico(String header) {
        assertTrue(resolver.resolver(header).isEmpty());
    }
}
