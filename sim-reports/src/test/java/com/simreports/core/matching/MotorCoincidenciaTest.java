package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;
import com.simreports.core.plantilla.TipoTablaPlantilla;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MotorCoincidenciaTest {

    private static ResultadoParseo parseo(Map<String, Double> consumos) {
        return new ResultadoParseo(consumos, List.of());
    }

    private static ResultadoParseo vacio() {
        return new ResultadoParseo(Map.of(), List.of());
    }

    // ---------- SIMPLE_LINEA ----------

    @Test
    void simpleLineaConMatchDevuelveConsumoDeAlestra() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        Asignacion asignacion = resultado.filas().get(0).consumo();
        assertEquals(100.0, asignacion.consumoMb());
        assertEquals(FuenteConsumo.ALESTRA, asignacion.fuente());
        assertTrue(asignacion.tieneMatch());
    }

    @Test
    void simpleLineaSinMatchDevuelveCero() {
        MotorCoincidencia motor = new MotorCoincidencia(vacio(), vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        Asignacion asignacion = resultado.filas().get(0).consumo();
        assertEquals(0.0, asignacion.consumoMb());
        assertFalse(asignacion.tieneMatch());
        assertNull(asignacion.fuente());
    }

    @Test
    void simpleLineaConLineaEnBlancoDevuelveCeroSinLanzarError() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea(null)));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        assertEquals(0.0, resultado.filas().get(0).consumo().consumoMb());
    }

    // ---------- SIMPLE_ICCID ----------

    @Test
    void simpleIccidConMatchDevuelveConsumoDeTelcelOAtt() {
        MotorCoincidencia motor = new MotorCoincidencia(
                vacio(), parseo(Map.of("8952020221396597611", 250.0)), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_ICCID,
                List.of(FilaPlantilla.simplePorIccid("8952020221396597611")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        Asignacion asignacion = resultado.filas().get(0).consumo();
        assertEquals(250.0, asignacion.consumoMb());
        assertEquals(FuenteConsumo.TELCEL_ATT, asignacion.fuente());
    }

    // ---------- SIMPLE_CASCADA ----------

    @Test
    void cascadaPrefiereAlestraCuandoLineaMatchea() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)),
                parseo(Map.of("8952020221396597611", 250.0)),
                vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA3", TipoTablaPlantilla.SIMPLE_CASCADA,
                List.of(FilaPlantilla.cascada("5511112222", "8952020221396597611")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        Asignacion asignacion = resultado.filas().get(0).consumo();
        assertEquals(100.0, asignacion.consumoMb());
        assertEquals(FuenteConsumo.ALESTRA, asignacion.fuente());
    }

    @Test
    void cascadaCaeAIccidCuandoLineaNoMatchea() {
        MotorCoincidencia motor = new MotorCoincidencia(
                vacio(),
                parseo(Map.of("8952020221396597611", 250.0)),
                vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA3", TipoTablaPlantilla.SIMPLE_CASCADA,
                List.of(FilaPlantilla.cascada("5511112222", "8952020221396597611")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        Asignacion asignacion = resultado.filas().get(0).consumo();
        assertEquals(250.0, asignacion.consumoMb());
        assertEquals(FuenteConsumo.TELCEL_ATT, asignacion.fuente());
    }

    @Test
    void cascadaSinMatchEnNingunLadoDevuelveCero() {
        MotorCoincidencia motor = new MotorCoincidencia(vacio(), vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA3", TipoTablaPlantilla.SIMPLE_CASCADA,
                List.of(FilaPlantilla.cascada("5511112222", "8952020221396597611")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        Asignacion asignacion = resultado.filas().get(0).consumo();
        assertEquals(0.0, asignacion.consumoMb());
        assertFalse(asignacion.tieneMatch());
    }

    @Test
    void cascadaConAmbosCamposEnBlancoDevuelveCeroSinError() {
        MotorCoincidencia motor = new MotorCoincidencia(vacio(), vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA3", TipoTablaPlantilla.SIMPLE_CASCADA,
                List.of(FilaPlantilla.cascada(null, null)));

        ResultadoTabla resultado = motor.resolverTabla(tabla);

        assertEquals(0.0, resultado.filas().get(0).consumo().consumoMb());
    }

    // ---------- DUAL_SIM ----------

    @Test
    void dualSimSumaAmbasLineasEnConsumoTotal() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511110001", 40.0, "5511110002", 60.0)), vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA_ROUTER", TipoTablaPlantilla.DUAL_SIM,
                List.of(FilaPlantilla.dualSim("5511110001", "5511110002")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);
        ResultadoFilaPlantilla fila = resultado.filas().get(0);

        assertEquals(40.0, fila.consumo1().consumoMb());
        assertEquals(FuenteConsumo.ALESTRA, fila.consumo1().fuente());
        assertEquals(60.0, fila.consumo2().consumoMb());
        assertEquals(FuenteConsumo.ALESTRA, fila.consumo2().fuente());
        assertEquals(100.0, fila.consumoTotal());
        assertNull(fila.consumo());
    }

    @Test
    void dualSimConUnaSoloLineaSinMatchSoloSumaLaEncontrada() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511110001", 40.0)), vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA_ROUTER", TipoTablaPlantilla.DUAL_SIM,
                List.of(FilaPlantilla.dualSim("5511110001", "5511119999")));

        ResultadoTabla resultado = motor.resolverTabla(tabla);
        ResultadoFilaPlantilla fila = resultado.filas().get(0);

        assertEquals(40.0, fila.consumo1().consumoMb());
        assertEquals(0.0, fila.consumo2().consumoMb());
        assertFalse(fila.consumo2().tieneMatch());
        assertEquals(40.0, fila.consumoTotal());
    }

    // ---------- No encontrados ----------

    @Test
    void reportaComoNoEncontradaUnaClaveDeOrigenQueNuncaFueConsultada() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0, "5511113333", 200.0)),
                vacio(), vacio());

        TablaConFilas tabla = new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222")));
        motor.resolverTabla(tabla);

        ResultadoMatching resultado = motor.finalizar();

        assertEquals(1, resultado.noEncontrados().size());
        NoEncontrado noEncontrado = resultado.noEncontrados().get(0);
        assertEquals(FuenteConsumo.ALESTRA, noEncontrado.fuente());
        assertEquals("5511113333", noEncontrado.clave());
        assertEquals(200.0, noEncontrado.consumoMb());
    }

    @Test
    void noReportaNoEncontradosSiTodasLasClavesFueronConsultadas() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)),
                parseo(Map.of("8952020221396597611", 250.0)),
                vacio());

        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));
        motor.resolverTabla(new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_ICCID,
                List.of(FilaPlantilla.simplePorIccid("8952020221396597611"))));

        ResultadoMatching resultado = motor.finalizar();

        assertTrue(resultado.noEncontrados().isEmpty());
        assertTrue(resultado.advertenciasConsistencia().isEmpty());
    }

    // ---------- Consistencia de totales ----------

    @Test
    void consistenciaCuadraCuandoTotalAsignadoMasNoEncontradoIgualaElOrigen() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0, "5511113333", 50.0)),
                vacio(), vacio());

        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));

        ResultadoMatching resultado = motor.finalizar();

        // 100 asignado + 50 no encontrado = 150 = total origen -> sin advertencia
        assertTrue(resultado.advertenciasConsistencia().isEmpty());
    }

    // ---------- Dos decimales redondeados (D-022) ----------

    @Test
    void redondeaLosConsumosADosDecimalesAntesDeAsignarYDeSumarTotales() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 4321.987654321, "5511113333", 0.289)),
                parseo(Map.of("89520700000000000011", 208.999)), vacio());

        ResultadoTabla linea = motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));
        ResultadoTabla iccid = motor.resolverTabla(new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_ICCID,
                List.of(FilaPlantilla.simplePorIccid("89520700000000000011"))));
        ResultadoMatching resultado = motor.finalizar();

        assertEquals(4321.99, linea.filas().get(0).consumo().consumoMb());
        assertEquals(209.0, iccid.filas().get(0).consumo().consumoMb());
        assertEquals(0.29, resultado.noEncontrados().get(0).consumoMb());
        TotalesFuente alestra = resultado.totales().stream()
                .filter(t -> t.fuente() == FuenteConsumo.ALESTRA).findFirst().orElseThrow();
        assertEquals(4322.28, alestra.totalOrigen(), 1e-9);
        assertTrue(resultado.advertenciasConsistencia().isEmpty());
    }

    // ---------- Avisos heredados ----------

    @Test
    void propagaAvisosHeredadosDeAlestraYDeLaCombinacionTelcelAtt() {
        AvisoParseo avisoAlestra = new AvisoParseo(TipoAviso.CLAVE_DUPLICADA, "5511112222", "duplicada en Alestra");
        ResultadoParseo alestra = new ResultadoParseo(Map.of("5511112222", 100.0), List.of(avisoAlestra));
        ResultadoParseo telcel = new ResultadoParseo(Map.of("111", 10.0), List.of());
        ResultadoParseo att = new ResultadoParseo(Map.of("111", 5.0), List.of()); // provoca aviso de combinación

        MotorCoincidencia motor = new MotorCoincidencia(alestra, telcel, att);
        ResultadoMatching resultado = motor.finalizar();

        assertEquals(2, resultado.avisosParseo().size());
        assertTrue(resultado.avisosParseo().contains(avisoAlestra));
        assertTrue(resultado.avisosParseo().stream()
                .anyMatch(a -> a.tipo() == TipoAviso.CLAVE_DUPLICADA && a.clave().equals("111")));
    }

    // ---------- resolverTablas (batch) ----------

    @Test
    void resolverTablasProcesaVariasTablasEnUnaSolaLlamada() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)),
                parseo(Map.of("8952020221396597611", 250.0)),
                vacio());

        List<TablaConFilas> tablas = List.of(
                new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                        List.of(FilaPlantilla.simplePorLinea("5511112222"))),
                new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_ICCID,
                        List.of(FilaPlantilla.simplePorIccid("8952020221396597611"))));

        List<ResultadoTabla> resultados = motor.resolverTablas(tablas);

        assertEquals(2, resultados.size());
        assertEquals("HOJA1", resultados.get(0).identificador());
        assertEquals(100.0, resultados.get(0).filas().get(0).consumo().consumoMb());
        assertEquals("HOJA2", resultados.get(1).identificador());
        assertEquals(250.0, resultados.get(1).filas().get(0).consumo().consumoMb());
    }

    // ---------- SIMs repetidas: regla default (D-001 / D-006) ----------

    @Test
    void mismaLineaEnDosHojasSeCuentaEnLasDosYLaConsistenciaDescuentaLoRepetido() {
        // D-001: cada hoja se entrega por separado; la SIM cuenta en las dos.
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());

        ResultadoTabla resultado1 = motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222").enFila(18))));
        ResultadoTabla resultado2 = motor.resolverTabla(new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222").enFila(12))));

        assertEquals(100.0, resultado1.filas().get(0).consumo().consumoMb());
        assertEquals(100.0, resultado2.filas().get(0).consumo().consumoMb());
        assertEquals(FuenteConsumo.ALESTRA, resultado2.filas().get(0).consumo().fuente());

        ResultadoMatching finalResult = motor.finalizar();

        // Escrito 200 = origen 100 + 100 repetido a propósito -> sin advertencia.
        assertTrue(finalResult.advertenciasConsistencia().isEmpty());
        TotalesFuente alestra = finalResult.totales().get(0);
        assertEquals(200.0, alestra.totalAsignado());
        assertEquals(100.0, alestra.totalRepetidoEntreHojas());
        assertEquals(200.0, finalResult.totalAsignado());

        assertEquals(1, finalResult.simsRepetidas().size());
        SimRepetida sim = finalResult.simsRepetidas().get(0);
        assertTrue(sim.repetidaEntreHojas());
        assertFalse(sim.repetidaEnLaMismaHoja());
        assertEquals(List.of("HOJA1", "HOJA2"), sim.hojas());
        assertEquals(List.of(
                        new AparicionSim("HOJA1", "HOJA1", 18, "LINEA", 100.0),
                        new AparicionSim("HOJA2", "HOJA2", 12, "LINEA", 100.0)),
                sim.apariciones());

        // Aviso informativo.
        assertEquals(1, finalResult.avisosParseo().size());
        assertEquals(TipoAviso.CLAVE_DUPLICADA, finalResult.avisosParseo().get(0).tipo());
    }

    @Test
    void mismaLineaDosVecesEnLaMismaHojaDejaLaSegundaEnCero() {
        // D-006: dentro de una hoja, la primera aparición gana.
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());

        ResultadoTabla resultado = motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222").enFila(13),
                        FilaPlantilla.simplePorLinea("5511112222").enFila(49))));

        assertEquals(100.0, resultado.filas().get(0).consumo().consumoMb());
        Asignacion segunda = resultado.filas().get(1).consumo();
        assertEquals(0.0, segunda.consumoMb());
        assertFalse(segunda.tieneMatch());

        ResultadoMatching finalResult = motor.finalizar();

        assertTrue(finalResult.advertenciasConsistencia().isEmpty());
        assertEquals(0.0, finalResult.totales().get(0).totalRepetidoEntreHojas());
        assertTrue(finalResult.noEncontrados().isEmpty());

        SimRepetida sim = finalResult.simsRepetidas().get(0);
        assertTrue(sim.repetidaEnLaMismaHoja());
        assertFalse(sim.repetidaEntreHojas());
        assertEquals(13, sim.apariciones().get(0).filaExcel());
        assertEquals(100.0, sim.apariciones().get(0).consumoAsignado());
        assertEquals(49, sim.apariciones().get(1).filaExcel());
        assertEquals(0.0, sim.apariciones().get(1).consumoAsignado());

        AvisoParseo aviso = finalResult.avisosParseo().get(0);
        assertEquals("5511112222", aviso.clave());
        assertTrue(aviso.detalle().contains("'HOJA1' fila 49"));
    }

    @Test
    void dosTablasDeLaMismaHojaCuentanComoLaMismaHoja() {
        // P. ej. los dos bloques de ALSEA: misma hoja, tablas distintas.
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());

        motor.resolverTabla(new TablaConFilas("ALSEA", "ALSEA - Tabla1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));
        ResultadoTabla segunda = motor.resolverTabla(new TablaConFilas("ALSEA", "ALSEA - Tabla2",
                TipoTablaPlantilla.SIMPLE_LINEA, List.of(FilaPlantilla.simplePorLinea("5511112222"))));

        assertEquals(0.0, segunda.filas().get(0).consumo().consumoMb());
        assertEquals(100.0, motor.finalizar().totales().get(0).totalAsignado());
    }

    @Test
    void repetidaEnDosHojasYDentroDeLaSegundaCuentaUnaVezPorHoja() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());

        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));
        ResultadoTabla hoja2 = motor.resolverTabla(new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"),
                        FilaPlantilla.simplePorLinea("5511112222"))));

        assertEquals(100.0, hoja2.filas().get(0).consumo().consumoMb());
        assertEquals(0.0, hoja2.filas().get(1).consumo().consumoMb());

        ResultadoMatching finalResult = motor.finalizar();
        assertEquals(200.0, finalResult.totales().get(0).totalAsignado());
        assertEquals(100.0, finalResult.totales().get(0).totalRepetidoEntreHojas());
        assertTrue(finalResult.advertenciasConsistencia().isEmpty());
        SimRepetida sim = finalResult.simsRepetidas().get(0);
        assertTrue(sim.repetidaEnLaMismaHoja());
        assertTrue(sim.repetidaEntreHojas());
        assertEquals(3, sim.apariciones().size());
    }

    @Test
    void dualSimRegistraElCampoDeLaLineaRepetida() {
        // D-001 real: SIM "Multicarrier" en una hoja y LINEA_2 de un router en otra.
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511110001", 10.0, "5511110002", 20.0)), vacio(), vacio());

        motor.resolverTabla(new TablaConFilas("ALSEA", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511110002").enFila(18))));
        ResultadoTabla bimbo = motor.resolverTabla(new TablaConFilas("BIMBO", TipoTablaPlantilla.DUAL_SIM,
                List.of(FilaPlantilla.dualSim("5511110001", "5511110002").enFila(12))));

        assertEquals(30.0, bimbo.filas().get(0).consumoTotal());
        AparicionSim enBimbo = motor.finalizar().simsRepetidas().get(0).apariciones().get(1);
        assertEquals(new AparicionSim("BIMBO", "BIMBO", 12, "LINEA_2", 20.0), enBimbo);
    }

    @Test
    void sinRepeticionesNoHaySimsRepetidas() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());
        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));

        assertTrue(motor.finalizar().simsRepetidas().isEmpty());
    }

    @Test
    void mismoIccidTresVecesGeneraAvisoEnLaSegundaYEnLaTercera() {
        MotorCoincidencia motor = new MotorCoincidencia(
                vacio(), parseo(Map.of("8952020221396597611", 250.0)), vacio());

        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_ICCID,
                List.of(FilaPlantilla.simplePorIccid("8952020221396597611"))));
        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_ICCID,
                List.of(FilaPlantilla.simplePorIccid("8952020221396597611"))));
        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_ICCID,
                List.of(FilaPlantilla.simplePorIccid("8952020221396597611"))));

        ResultadoMatching resultado = motor.finalizar();

        assertEquals(2, resultado.avisosParseo().size());
        assertTrue(resultado.avisosParseo().stream().allMatch(a -> a.tipo() == TipoAviso.CLAVE_DUPLICADA));
    }

    @Test
    void cascadaConLineaRepetidaNoCaeAIccidEnLaSegundaAparicion() {
        // Regresión: la regla default devuelve una asignación sin fuente
        // para la 2a aparición; la cascada NO debe interpretarlo como "la
        // línea no matcheó" y buscar el ICCID en Telcel + ATT.
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)),
                parseo(Map.of("8952020221396597611", 7.0)),
                vacio());

        ResultadoTabla resultado = motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_CASCADA,
                List.of(
                        FilaPlantilla.cascada("5511112222", "8952020221396590000"),
                        FilaPlantilla.cascada("5511112222", "8952020221396597611"))));

        Asignacion segunda = resultado.filas().get(1).consumo();
        assertEquals(0.0, segunda.consumoMb());
        assertNotEquals(FuenteConsumo.TELCEL_ATT, segunda.fuente());

        ResultadoMatching finalResult = motor.finalizar();
        // El ICCID nunca se consultó, así que queda en "no encontrados".
        assertTrue(finalResult.noEncontrados().stream().anyMatch(
                n -> n.fuente() == FuenteConsumo.TELCEL_ATT && n.clave().equals("8952020221396597611")));
        assertTrue(finalResult.avisosParseo().stream().anyMatch(
                a -> a.tipo() == TipoAviso.CLAVE_DUPLICADA && a.clave().equals("5511112222")));
        assertTrue(finalResult.advertenciasConsistencia().isEmpty());
    }

    // ---------- ReglaAmbigua: inyección de una regla personalizada ----------

    @Test
    void permiteInyectarUnaReglaAmbiguaPersonalizada() {
        // Regla de prueba: en vez de dejar la duplicada en 0, la deja pasar
        // igual (comportamiento distinto al default), sin generar aviso.
        ReglaAmbigua siempreRepiteSinAvisar = contexto ->
                new ResolucionAmbigua(new Asignacion(contexto.consumoMb(), contexto.fuente()), null);

        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio(), siempreRepiteSinAvisar);

        motor.resolverTabla(new TablaConFilas("HOJA1", "HOJA1 - Tabla1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));
        ResultadoTabla resultado2 = motor.resolverTabla(new TablaConFilas("HOJA1", "HOJA1 - Tabla2",
                TipoTablaPlantilla.SIMPLE_LINEA, List.of(FilaPlantilla.simplePorLinea("5511112222"))));

        assertEquals(100.0, resultado2.filas().get(0).consumo().consumoMb());

        ResultadoMatching finalResult = motor.finalizar();

        // Con esta regla el total escrito (200) sí difiere del origen (100) y,
        // como la repetición es dentro de la MISMA hoja, no se descuenta: la
        // consistencia SÍ debe marcar advertencia. No hay aviso explícito
        // porque la regla personalizada decidió no generar uno.
        assertEquals(1, finalResult.advertenciasConsistencia().size());
        assertTrue(finalResult.avisosParseo().isEmpty());
    }

    // ---------- Normalización de claves de la plantilla ----------

    @Test
    void normalizaLasClavesDeLaPlantillaAntesDeBuscarlas() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)),
                parseo(Map.of("8952020221396597611", 7.0)),
                vacio());

        ResultadoTabla resultado = motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_CASCADA,
                List.of(
                        FilaPlantilla.cascada("(55) 1111-2222", null),
                        FilaPlantilla.cascada("   ", " '8952020221396597611' "))));

        assertEquals(100.0, resultado.filas().get(0).consumo().consumoMb());
        assertEquals(7.0, resultado.filas().get(1).consumo().consumoMb());
        assertTrue(motor.finalizar().noEncontrados().isEmpty());
    }

    @Test
    void noEncontradosSalenOrdenadosPorFuenteYClave() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5533333333", 1.0, "5511111111", 2.0, "5522222222", 3.0)),
                parseo(Map.of("8952000000000000009", 4.0, "8952000000000000001", 5.0)),
                vacio());

        List<NoEncontrado> noEncontrados = motor.finalizar().noEncontrados();

        assertEquals(List.of("5511111111", "5522222222", "5533333333", "8952000000000000001", "8952000000000000009"),
                noEncontrados.stream().map(NoEncontrado::clave).toList());
        assertEquals(FuenteConsumo.ALESTRA, noEncontrados.get(0).fuente());
        assertEquals(FuenteConsumo.TELCEL_ATT, noEncontrados.get(4).fuente());
    }

    // ---------- Columna SIM / Carrier (D-016) ----------

    private static final String LINEA = "5511112222";
    private static final String ICCID = "89520700000000000011";

    /** Alestra tiene la línea; Telcel tiene el ICCID. */
    private static MotorCoincidencia motorConLineaEIccid() {
        return new MotorCoincidencia(parseo(Map.of(LINEA, 100.0)), parseo(Map.of(ICCID, 7.0)), vacio());
    }

    private static ResultadoMatching unaFila(MotorCoincidencia motor, FilaPlantilla fila, Asignacion esperada) {
        ResultadoTabla resultado = motor.resolverTabla(new TablaConFilas("TIENDAS EXTRA", TipoTablaPlantilla.SIMPLE_CASCADA,
                List.of(fila.enFila(283))));
        assertEquals(esperada, resultado.filas().get(0).consumo());
        return motor.finalizar();
    }

    @Test
    void carrierAlestraBuscaSoloPorLinea() {
        unaFila(motorConLineaEIccid(), FilaPlantilla.cascada(LINEA, ICCID).conSim("ALESTRA"),
                new Asignacion(100.0, FuenteConsumo.ALESTRA));
    }

    @Test
    void carrierAlestraConLineaQueNoEstaNoCaeAIccidYAvisa() {
        // El caso de TIENDAS EXTRA fila 283: LINEA "pendiente" + ICCID de Telcel.
        ResultadoMatching resultado = unaFila(motorConLineaEIccid(),
                FilaPlantilla.cascada("pendiente", ICCID).conSim("ALESTRA"), Asignacion.sinMatch());

        AvisoParseo aviso = resultado.avisosParseo().get(0);
        assertEquals(TipoAviso.CARRIER_NO_COINCIDE, aviso.tipo());
        assertEquals(ICCID, aviso.clave());
        assertTrue(aviso.detalle().contains("'TIENDAS EXTRA' fila 283"), aviso.detalle());
        assertTrue(aviso.detalle().contains("Telcel/ATT"), aviso.detalle());
        // El ICCID no se asignó: queda como consumo sin cliente.
        assertTrue(resultado.noEncontrados().stream().anyMatch(n -> n.clave().equals(ICCID)));
        assertTrue(resultado.advertenciasConsistencia().isEmpty());
    }

    @Test
    void multicarrierEsAlestra() {
        unaFila(motorConLineaEIccid(), FilaPlantilla.cascada("pendiente", ICCID).conSim("Multicarrier"),
                Asignacion.sinMatch());
    }

    @Test
    void carrierTelcelOAttBuscaSoloPorIccidAunqueLaLineaEsteEnAlestra() {
        ResultadoMatching resultado = unaFila(motorConLineaEIccid(),
                FilaPlantilla.cascada(LINEA, ICCID).conSim("Telcel"), new Asignacion(7.0, FuenteConsumo.TELCEL_ATT));
        // La línea de Alestra no se consultó.
        assertTrue(resultado.noEncontrados().stream().anyMatch(n -> n.clave().equals(LINEA)));
        assertTrue(resultado.avisosParseo().isEmpty());

        unaFila(motorConLineaEIccid(), FilaPlantilla.cascada(LINEA, ICCID).conSim("AT&T_2"),
                new Asignacion(7.0, FuenteConsumo.TELCEL_ATT));
    }

    @Test
    void carrierTelcelConIccidQueNoEstaYLineaEnAlestraQuedaEnCeroYAvisa() {
        ResultadoMatching resultado = unaFila(motorConLineaEIccid(),
                FilaPlantilla.cascada(LINEA, "89520700000000000099").conSim("ATT001"), Asignacion.sinMatch());

        AvisoParseo aviso = resultado.avisosParseo().get(0);
        assertEquals(TipoAviso.CARRIER_NO_COINCIDE, aviso.tipo());
        assertEquals(LINEA, aviso.clave());
    }

    @Test
    void carrierNoReconocidoOVacioUsaLaCascadaDeSiempre() {
        unaFila(motorConLineaEIccid(), FilaPlantilla.cascada("pendiente", ICCID).conSim("STARLINK"),
                new Asignacion(7.0, FuenteConsumo.TELCEL_ATT));
        unaFila(motorConLineaEIccid(), FilaPlantilla.cascada("pendiente", ICCID),
                new Asignacion(7.0, FuenteConsumo.TELCEL_ATT));
        unaFila(motorConLineaEIccid(), FilaPlantilla.cascada(LINEA, ICCID).conSim("  "),
                new Asignacion(100.0, FuenteConsumo.ALESTRA));
    }

    @Test
    void carrierTelcelEnTablaSoloDeLineaQuedaEnCero() {
        // Telcel/ATT se busca solo por ICCID; una tabla sin ICCID no tiene con qué.
        MotorCoincidencia motor = motorConLineaEIccid();
        ResultadoTabla resultado = motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea(LINEA).conSim("TELCEL"))));

        assertEquals(Asignacion.sinMatch(), resultado.filas().get(0).consumo());
        assertEquals(TipoAviso.CARRIER_NO_COINCIDE, motor.finalizar().avisosParseo().get(0).tipo());
    }

    @Test
    void dualSimIgnoraLaColumnaSim() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511110001", 10.0, "5511110002", 20.0)), vacio(), vacio());
        ResultadoTabla resultado = motor.resolverTabla(new TablaConFilas("BIMBO", TipoTablaPlantilla.DUAL_SIM,
                List.of(FilaPlantilla.dualSim("5511110001", "5511110002").conSim("TELCEL"))));

        assertEquals(30.0, resultado.filas().get(0).consumoTotal());
    }
}
