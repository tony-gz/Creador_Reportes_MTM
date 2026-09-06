package com.simreports.core.parsers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelcelHeaderResolverTest {

    private final TelcelHeaderResolver resolver = new TelcelHeaderResolver();

    @ParameterizedTest
    @ValueSource(strings = {"ICCID", "iccid", " Iccid "})
    void reconoceIccid(String header) {
        assertEquals("ICCID", resolver.resolver(header).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USO (MB)", "uso (mb)", "USO(MB)"})
    void reconoceUsoMbComoConsumo(String header) {
        assertEquals("CONSUMO", resolver.resolver(header).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DURACIÓN (UTC)", "MSISDN", "IMSI", "USO (KB)", "USO (BYTES)", ""
    })
    void noReconoceElRestoDeColumnas(String header) {
        assertTrue(resolver.resolver(header).isEmpty());
    }
}
