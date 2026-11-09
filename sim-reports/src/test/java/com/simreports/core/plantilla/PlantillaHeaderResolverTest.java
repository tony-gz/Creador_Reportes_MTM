package com.simreports.core.plantilla;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantillaHeaderResolverTest {

    private final PlantillaHeaderResolver resolver = new PlantillaHeaderResolver();

    @ParameterizedTest
    @ValueSource(strings = {
            "LINEA", "linea", " Linea ",
            "LINEA_1", "LINEA_2",
            "ICCID", "iccid",
            "CONSUMO(MB)", "consumo(mb)", "CONSUMO (MB)", "CONSUMO   (MB)",
            "CONSUMO(MB)_1", "CONSUMO (MB)_1",
            "CONSUMO(MB)_2"
    })
    void reconoceLosCamposLogicosDeLaPlantillaConVariantesReales(String headerReal) {
        assertTrue(resolver.resolver(headerReal).isPresent(),
                "Debería reconocer: " + headerReal);
    }

    @Test
    void resuelveAlCampoLogicoNormalizado() {
        assertEquals(Optional.of("CONSUMO(MB)"), resolver.resolver("CONSUMO (MB)"));
        assertEquals(Optional.of("LINEA_1"), resolver.resolver("linea_1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SERIE", "UNIDAD", "MARCA", "MODELO", "CONSUMO_TOTAL", ""})
    void noReconoceColumnasQueNoSonCampoLogico(String headerReal) {
        assertTrue(resolver.resolver(headerReal).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIM", "sim", "Carrier", "CARRIER", " Carrier "})
    void reconoceLaColumnaDelOperadorComoSimYCarrier(String headerReal) {
        // D-016: TIENDAS EXTRA la llama "Carrier"; el resto de hojas, "SIM".
        assertEquals(Optional.of("SIM"), resolver.resolver(headerReal));
    }

    @Test
    void noReconoceNull() {
        assertTrue(resolver.resolver(null).isEmpty());
    }
}
