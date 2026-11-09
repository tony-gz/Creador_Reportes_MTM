package com.simreports.core.plantilla;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolverTablaPlantillaTest {

    private final ResolverTablaPlantilla resolver = new ResolverTablaPlantilla();

    @Test
    void clasificaTablaSimpleCascadaConLineaEIccid() {
        // Caso real más común: 15 de las 18 hojas (spec 03, regla 1).
        TablaPlantillaResuelta resultado = resolver.resolver(
                List.of("LINEA", "ICCID", "CONSUMO(MB)", "SIM", "SERIE"));

        assertEquals(TipoTablaPlantilla.SIMPLE_CASCADA, resultado.tipo());
        assertEquals("LINEA", resultado.columnaPorCampoLogico().get("LINEA"));
        assertEquals("ICCID", resultado.columnaPorCampoLogico().get("ICCID"));
        assertEquals("CONSUMO(MB)", resultado.columnaPorCampoLogico().get("CONSUMO(MB)"));
        assertEquals("SIM", resultado.columnaPorCampoLogico().get("SIM"));
        assertEquals(List.of("SERIE"), resultado.encabezadosNoReconocidos());
    }

    @Test
    void laColumnaCarrierQuedaComoCampoSim() {
        TablaPlantillaResuelta resultado = resolver.resolver(
                List.of("Carrier", "LINEA", "ICCID", "CONSUMO(MB)"));

        assertEquals(TipoTablaPlantilla.SIMPLE_CASCADA, resultado.tipo());
        assertEquals("Carrier", resultado.columnaPorCampoLogico().get("SIM"));
    }

    @Test
    void clasificaTablaSimplePorLinea() {
        TablaPlantillaResuelta resultado = resolver.resolver(List.of("LINEA", "CONSUMO(MB)"));

        assertEquals(TipoTablaPlantilla.SIMPLE_LINEA, resultado.tipo());
        assertTrue(resultado.encabezadosNoReconocidos().isEmpty());
    }

    @Test
    void clasificaTablaSimplePorIccid() {
        TablaPlantillaResuelta resultado = resolver.resolver(List.of("ICCID", "CONSUMO(MB)"));

        assertEquals(TipoTablaPlantilla.SIMPLE_ICCID, resultado.tipo());
    }

    @Test
    void clasificaTablaDualSimConVarianteDeEspacioEnConsumo() {
        // Solo 3 de las 18 hojas (spec 03, regla 4). Incluye ICCID (se
        // ignora para el matching dual) y CONSUMO_TOTAL (se recalcula,
        // nunca se lee).
        TablaPlantillaResuelta resultado = resolver.resolver(
                List.of("LINEA_1", "LINEA_2", "ICCID", "CONSUMO (MB)_1", "CONSUMO(MB)_2",
                        "CONSUMO_TOTAL", "MARCA"));

        assertEquals(TipoTablaPlantilla.DUAL_SIM, resultado.tipo());
        assertEquals("LINEA_1", resultado.columnaPorCampoLogico().get("LINEA_1"));
        assertEquals("LINEA_2", resultado.columnaPorCampoLogico().get("LINEA_2"));
        assertEquals("CONSUMO (MB)_1", resultado.columnaPorCampoLogico().get("CONSUMO(MB)_1"));
        assertEquals("CONSUMO(MB)_2", resultado.columnaPorCampoLogico().get("CONSUMO(MB)_2"));
        // CONSUMO_TOTAL no es un campo lógico leído; queda como "no
        // reconocido" para revisión manual, igual que MARCA.
        assertEquals(List.of("CONSUMO_TOTAL", "MARCA"), resultado.encabezadosNoReconocidos());
    }

    @Test
    void fallaSiTablaDualEstaIncompleta() {
        // Le falta LINEA_2 y CONSUMO(MB)_2: no se debe adivinar.
        TablaPlantillaNoReconocidaException ex = assertThrows(
                TablaPlantillaNoReconocidaException.class,
                () -> resolver.resolver(List.of("LINEA_1", "CONSUMO(MB)_1")));

        assertTrue(ex.getMessage().contains("dual SIM"));
    }

    @Test
    void fallaSiNoHayColumnaDeConsumoParaSimpleCascada() {
        assertThrows(TablaPlantillaNoReconocidaException.class,
                () -> resolver.resolver(List.of("LINEA", "ICCID", "SIM")));
    }

    @Test
    void fallaSiNoHayColumnaDeConsumoParaSimplePorLinea() {
        assertThrows(TablaPlantillaNoReconocidaException.class,
                () -> resolver.resolver(List.of("LINEA", "SIM")));
    }

    @Test
    void fallaSiNoHayColumnaDeConsumoParaSimplePorIccid() {
        assertThrows(TablaPlantillaNoReconocidaException.class,
                () -> resolver.resolver(List.of("ICCID", "SIM")));
    }

    @Test
    void fallaSiNoSeReconoceNingunaColumnaDeLineaOIccid() {
        TablaPlantillaNoReconocidaException ex = assertThrows(
                TablaPlantillaNoReconocidaException.class,
                () -> resolver.resolver(List.of("SIM", "SERIE", "MARCA")));

        assertTrue(ex.getMessage().contains("No se pudo clasificar"));
    }

    @Test
    void fallaSiDosEncabezadosDistintosResuelvenAlMismoCampoLogico() {
        // "LINEA" y "linea " normalizan al mismo campo lógico "LINEA".
        TablaPlantillaNoReconocidaException ex = assertThrows(
                TablaPlantillaNoReconocidaException.class,
                () -> resolver.resolver(List.of("LINEA", "linea ", "CONSUMO(MB)")));

        assertTrue(ex.getMessage().contains("LINEA"));
    }
}
