package com.simreports.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultadoParseoTest {

    @Test
    void construyeConMapaYAvisosVacios() {
        ResultadoParseo resultado = new ResultadoParseo(Map.of(), List.of());

        assertTrue(resultado.consumos().isEmpty());
        assertTrue(resultado.avisos().isEmpty());
    }

    @Test
    void guardaLosConsumosYAvisosDados() {
        Map<String, Double> consumos = Map.of("6641234567", 150.0, "6647654321", 0.0);
        AvisoParseo aviso = new AvisoParseo(
                TipoAviso.CLAVE_DUPLICADA,
                "6641234567",
                "Apareció 2 veces en el archivo, se sumaron los MB.");

        ResultadoParseo resultado = new ResultadoParseo(consumos, List.of(aviso));

        assertEquals(150.0, resultado.consumos().get("6641234567"));
        assertEquals(1, resultado.avisos().size());
        assertEquals(TipoAviso.CLAVE_DUPLICADA, resultado.avisos().get(0).tipo());
    }

    @Test
    void elMapaDeConsumosEsInmutable() {
        HashMap<String, Double> consumosMutables = new HashMap<>();
        consumosMutables.put("6641234567", 100.0);

        ResultadoParseo resultado = new ResultadoParseo(consumosMutables, List.of());

        // Mutar el mapa original después de construir no debe afectar al resultado.
        consumosMutables.put("6647654321", 999.0);
        assertEquals(1, resultado.consumos().size());

        // El mapa expuesto por el record tampoco debe poder mutarse desde afuera.
        assertThrows(UnsupportedOperationException.class,
                () -> resultado.consumos().put("otra", 1.0));
    }

    @Test
    void elMapaDeConsumosNoPuedeSerNull() {
        assertThrows(NullPointerException.class,
                () -> new ResultadoParseo(null, List.of()));
    }

    @Test
    void laListaDeAvisosNoPuedeSerNull() {
        assertThrows(NullPointerException.class,
                () -> new ResultadoParseo(Map.of(), null));
    }
}
