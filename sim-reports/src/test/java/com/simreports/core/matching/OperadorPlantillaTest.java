package com.simreports.core.matching;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperadorPlantillaTest {

    @ParameterizedTest
    @ValueSource(strings = {"ALESTRA", "Alestra", " alestra ", "MULTICARRIER", "Multicarrier"})
    void alestraYMulticarrierSonAlestra(String valor) {
        assertEquals(OperadorPlantilla.ALESTRA, OperadorPlantilla.desde(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TELCEL", "Telcel", "AT&T", "AT&T_2", "ATT001", "ATT002", "ATT02", "att"})
    void telcelYVariantesDeAttSonTelcelAtt(String valor) {
        // Valores reales de la plantilla de octubre 2026.
        assertEquals(OperadorPlantilla.TELCEL_ATT, OperadorPlantilla.desde(valor));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "STARLINK", "OUI", "Proveedor de internet"})
    void vacioUOtrosValoresNoSeReconocen(String valor) {
        assertEquals(OperadorPlantilla.NO_RECONOCIDO, OperadorPlantilla.desde(valor));
    }
}
