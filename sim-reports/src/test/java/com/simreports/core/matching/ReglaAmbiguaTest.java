package com.simreports.core.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglaAmbiguaTest {

    @Test
    void contextoFilaRechazaNumeroDeAparicionMenorA2() {
        assertThrows(IllegalArgumentException.class, () ->
                new ContextoFila("5511112222", FuenteConsumo.ALESTRA, 100.0, "HOJA1", 1));
    }

    @Test
    void contextoFilaAceptaNumeroDeAparicionDesde2() {
        ContextoFila contexto = new ContextoFila("5511112222", FuenteConsumo.ALESTRA, 100.0, "HOJA1", 2);
        assertEquals(2, contexto.numeroDeAparicion());
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
    void reglaDefaultDejaLaAsignacionEnCeroYGeneraAdvertenciaConDatosDelContexto() {
        ReglaAmbigua regla = new PrimeraCoincidenciaGanaReglaAmbigua();
        ContextoFila contexto = new ContextoFila("5511112222", FuenteConsumo.ALESTRA, 100.0, "BIMBO - Table1", 2);

        ResolucionAmbigua resolucion = regla.resolver(contexto);

        assertEquals(0.0, resolucion.asignacion().consumoMb());
        assertFalse(resolucion.asignacion().tieneMatch());
        assertTrue(resolucion.advertenciaOpcional().isPresent());
        String detalle = resolucion.advertenciaOpcional().get();
        assertTrue(detalle.contains("5511112222"));
        assertTrue(detalle.contains("BIMBO - Table1"));
        assertTrue(detalle.contains("100.0"));
    }
}