package com.simreports.core.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglaAmbiguaTest {

    private static ContextoFila contexto(boolean yaAparecioEnEstaHoja) {
        return new ContextoFila("5511112222", FuenteConsumo.ALESTRA, 100.0,
                "BIMBO", "BIMBO - Table1", 12, 2, yaAparecioEnEstaHoja);
    }

    @Test
    void contextoFilaRechazaNumeroDeAparicionMenorA2() {
        assertThrows(IllegalArgumentException.class, () ->
                new ContextoFila("5511112222", FuenteConsumo.ALESTRA, 100.0, "HOJA1", "HOJA1", null, 1, false));
    }

    @Test
    void contextoFilaAceptaNumeroDeAparicionDesde2() {
        assertEquals(2, contexto(true).numeroDeAparicion());
    }

    @Test
    void contextoFilaDescribeLaUbicacionConYSinFila() {
        assertEquals("'BIMBO' fila 12", contexto(true).ubicacion());
        ContextoFila sinFila = new ContextoFila("5511112222", FuenteConsumo.ALESTRA, 100.0,
                "BIMBO", "BIMBO - Table1", null, 2, true);
        assertEquals("'BIMBO'", sinFila.ubicacion());
    }

    @Test
    void resolucionAmbiguaExponeAdvertenciaOpcionalVacaSiEsNull() {
        ResolucionAmbigua resolucion = new ResolucionAmbigua(Asignacion.sinMatch(), null);
        assertTrue(resolucion.advertenciaOpcional().isEmpty());
    }

    @Test
    void resolucionAmbiguaExponeAdvertenciaOpcionalPresenteSiTieneTexto() {
        ResolucionAmbigua resolucion = new ResolucionAmbigua(Asignacion.sinMatch(), "revisar duplicado");
        assertEquals("revisar duplicado", resolucion.advertenciaOpcional().orElseThrow());
    }

    @Test
    void reglaDefaultEnLaMismaHojaDejaLaRepeticionEnCeroYAvisa() {
        // D-006: dentro de una hoja, solo la primera aparición cuenta.
        ResolucionAmbigua resolucion = new PrimeraEnLaHojaGanaReglaAmbigua().resolver(contexto(true));

        assertEquals(0.0, resolucion.asignacion().consumoMb());
        assertFalse(resolucion.asignacion().tieneMatch());
        String detalle = resolucion.advertenciaOpcional().orElseThrow();
        assertTrue(detalle.contains("5511112222"));
        assertTrue(detalle.contains("'BIMBO' fila 12"));
        assertTrue(detalle.contains("100.0"));
    }

    @Test
    void reglaDefaultEnOtraHojaCuentaElConsumoCompletoYAvisa() {
        // D-001: entre hojas distintas, se cuenta en cada una.
        ResolucionAmbigua resolucion = new PrimeraEnLaHojaGanaReglaAmbigua().resolver(contexto(false));

        assertEquals(100.0, resolucion.asignacion().consumoMb());
        assertEquals(FuenteConsumo.ALESTRA, resolucion.asignacion().fuente());
        assertTrue(resolucion.advertenciaOpcional().orElseThrow().contains("otra hoja"));
    }
}
