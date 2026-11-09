package com.simreports.core.matching;

import com.simreports.core.ResultadoParseo;
import com.simreports.core.plantilla.TipoTablaPlantilla;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificadorReporteEscritoTest {

    private static ResultadoMatching corridaConAsignados() {
        // Alestra: 100 asignados + 5 no encontrados. Telcel: 7 asignados.
        MotorCoincidencia motor = new MotorCoincidencia(
                new ResultadoParseo(Map.of("5511112222", 100.0, "5599999999", 5.0), List.of()),
                new ResultadoParseo(Map.of("8952020221396597611", 7.0), List.of()),
                new ResultadoParseo(Map.of(), List.of()));
        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_CASCADA, List.of(
                FilaPlantilla.cascada("5511112222", null),
                FilaPlantilla.cascada(null, "8952020221396597611"))));
        return motor.finalizar();
    }

    @Test
    void finalizarExponeTotalesPorFuenteYTotalAsignado() {
        ResultadoMatching resultado = corridaConAsignados();

        assertEquals(List.of(
                        new TotalesFuente(FuenteConsumo.ALESTRA, 105.0, 100.0, 5.0),
                        new TotalesFuente(FuenteConsumo.TELCEL_ATT, 7.0, 7.0, 0.0)),
                resultado.totales());
        assertEquals(107.0, resultado.totalAsignado());
    }

    @Test
    void noReportaDescuadreSiElArchivoContieneLoAsignado() {
        assertTrue(VerificadorReporteEscrito.verificar(corridaConAsignados(), 107.0).isEmpty());
        // Dentro de la tolerancia por redondeo.
        assertTrue(VerificadorReporteEscrito.verificar(corridaConAsignados(), 107.004).isEmpty());
    }

    @Test
    void reportaDescuadreSiAlArchivoLeFaltanMb() {
        // P. ej. el escritor se saltó la fila de Telcel.
        Optional<DescuadreReporteEscrito> descuadre = VerificadorReporteEscrito.verificar(corridaConAsignados(), 100.0);

        assertTrue(descuadre.isPresent());
        assertEquals(107.0, descuadre.get().totalEsperado());
        assertEquals(100.0, descuadre.get().totalLeido());
        assertEquals(7.0, descuadre.get().diferencia());
    }

    @Test
    void reportaDescuadreSiElArchivoTieneMbDeMas() {
        // P. ej. se sumó CONSUMO_TOTAL además de CONSUMO(MB)_1 y _2.
        assertTrue(VerificadorReporteEscrito.verificar(corridaConAsignados(), 214.0).isPresent());
    }

    @Test
    void reportaDescuadreSiElTotalLeidoNoEsUnNumero() {
        assertTrue(VerificadorReporteEscrito.verificar(corridaConAsignados(), Double.NaN).isPresent());
    }
}
