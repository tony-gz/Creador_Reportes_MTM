package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;
import com.simreports.core.plantilla.TipoTablaPlantilla;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReporteFinalCorridaTest {

    private static ResultadoParseo parseo(Map<String, Double> consumos) {
        return new ResultadoParseo(consumos, List.of());
    }

    private static ResultadoParseo vacio() {
        return new ResultadoParseo(Map.of(), List.of());
    }

    /** Corrida con una repetida en la misma hoja, una entre hojas y 3 claves sin cliente. */
    private static String reporte() {
        AvisoParseo avisoAlestra = new AvisoParseo(TipoAviso.CLAVE_VACIA_O_MALFORMADA, "fila 9", "línea vacía");
        MotorCoincidencia motor = new MotorCoincidencia(
                new ResultadoParseo(Map.of(
                        "5500000001", 100.0,
                        "5500000002", 50.0,
                        "5500000003", 1234.5,
                        "5500000004", 7.25), List.of(avisoAlestra)),
                parseo(Map.of("89520700000000000011", 5.81)),
                vacio());

        motor.resolverTabla(new TablaConFilas("DENTALIA", "DENTALIA - Tabla1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5500000001").enFila(13),
                        FilaPlantilla.simplePorLinea("5500000001").enFila(49))));
        motor.resolverTabla(new TablaConFilas("ALSEA", "ALSEA - Tabla1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5500000002").enFila(18))));
        motor.resolverTabla(new TablaConFilas("BIMBO", "BIMBO - Tabla1", TipoTablaPlantilla.DUAL_SIM,
                List.of(FilaPlantilla.dualSim(null, "5500000002").enFila(12))));

        return ReporteFinalCorrida.generar(motor.finalizar());
    }

    @Test
    void listaLaRepetidaDentroDeUnaHojaConLaFilaQueConservaElConsumo() {
        String texto = reporte();

        assertTrue(texto.contains("2. SIMs repetidas dentro de una hoja (la primera conserva el consumo): 1"), texto);
        assertTrue(texto.contains("DENTALIA · ALESTRA 5500000001 (100.00 MB): fila 13 conserva el consumo; fila 49 en 0"),
                texto);
    }

    @Test
    void listaLaRepetidaEntreHojasConCadaHojaYSuFila() {
        String texto = reporte();

        assertTrue(texto.contains("3. SIMs repetidas en hojas distintas (se cuentan en cada hoja): 1"), texto);
        assertTrue(texto.contains("ALESTRA 5500000002 (50.00 MB en cada hoja): ALSEA fila 18; BIMBO fila 12 (LINEA_2)"),
                texto);
    }

    @Test
    void listaElConsumoSinClienteDeMayorAMenorPorFuente() {
        String texto = reporte();

        assertTrue(texto.contains("4. Consumo sin cliente (con consumo en la fuente, en ninguna hoja de la plantilla): 3"),
                texto);
        assertTrue(texto.contains("ALESTRA: 2 líneas, 1,241.75 MB"), texto);
        assertTrue(texto.contains("TELCEL_ATT: 1 ICCID, 5.81 MB"), texto);
        int mayor = texto.indexOf("5500000003");
        int menor = texto.indexOf("5500000004");
        assertTrue(mayor > 0 && menor > mayor, "de mayor a menor consumo:\n" + texto);
        assertTrue(texto.contains("1,234.50 MB"), texto);
        assertTrue(texto.contains("89520700000000000011"), texto);
    }

    @Test
    void muestraTotalesConLoContadoEnOtraHojaYConsistenciaQueCuadra() {
        String texto = reporte();

        assertTrue(texto.contains("ALESTRA: origen 1,391.75 MB | asignado 200.00 MB | sin cliente 1,241.75 MB"
                + " | contado de nuevo en otra hoja 50.00 MB"), texto);
        assertTrue(texto.contains("5. Consistencia de totales: cuadra"), texto);
    }

    @Test
    void enOtrosAvisosOmiteLosDeSimsRepetidasPeroConservaLosDeLosParsers() {
        String texto = reporte();

        assertTrue(texto.contains("6. Otros avisos: 1"), texto);
        assertTrue(texto.contains("CLAVE_VACIA_O_MALFORMADA fila 9: línea vacía"), texto);
    }

    @Test
    void corridaSinNovedadesMuestraLasSeccionesVacias() {
        MotorCoincidencia motor = new MotorCoincidencia(parseo(Map.of("5500000001", 1.0)), vacio(), vacio());
        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5500000001"))));

        String texto = ReporteFinalCorrida.generar(motor.finalizar());

        assertTrue(texto.contains("dentro de una hoja (la primera conserva el consumo): 0"), texto);
        assertTrue(texto.contains("en hojas distintas (se cuentan en cada hoja): 0"), texto);
        assertTrue(texto.contains("en ninguna hoja de la plantilla): 0"), texto);
        assertFalse(texto.contains("contado de nuevo"), texto);
    }

    @Test
    void filaPlantillaRechazaNumeroDeFilaMenorA1() {
        assertThrows(IllegalArgumentException.class, () -> FilaPlantilla.simplePorLinea("5500000001").enFila(0));
    }
}
