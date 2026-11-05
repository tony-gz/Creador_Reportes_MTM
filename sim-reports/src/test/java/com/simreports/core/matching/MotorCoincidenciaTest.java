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

    // ---------- ReglaAmbigua: default (primera coincidencia gana) ----------

    @Test
    void mismaLineaDosVecesConReglaDefaultDejaLaSegundaEnCeroYNoDisparaAdvertenciaDeConsistencia() {
        MotorCoincidencia motor = new MotorCoincidencia(
                parseo(Map.of("5511112222", 100.0)), vacio(), vacio());

        ResultadoTabla resultado1 = motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));
        ResultadoTabla resultado2 = motor.resolverTabla(new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));

        // Primera aparición: consumo real, con match.
        assertEquals(100.0, resultado1.filas().get(0).consumo().consumoMb());
        assertTrue(resultado1.filas().get(0).consumo().tieneMatch());

        // Segunda aparición: 0 MB, sin fuente (delegada en ReglaAmbigua).
        Asignacion segunda = resultado2.filas().get(0).consumo();
        assertEquals(0.0, segunda.consumoMb());
        assertFalse(segunda.tieneMatch());

        ResultadoMatching finalResult = motor.finalizar();

        // El total escrito (100 + 0) coincide con el origen (100) -> sin advertencia.
        assertTrue(finalResult.advertenciasConsistencia().isEmpty());
        assertTrue(finalResult.noEncontrados().isEmpty());

        // Pero sí queda un aviso explícito de la clave duplicada.
        assertEquals(1, finalResult.avisosParseo().size());
        AvisoParseo aviso = finalResult.avisosParseo().get(0);
        assertEquals(TipoAviso.CLAVE_DUPLICADA, aviso.tipo());
        assertEquals("5511112222", aviso.clave());
        assertTrue(aviso.detalle().contains("HOJA2"));
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

        motor.resolverTabla(new TablaConFilas("HOJA1", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));
        ResultadoTabla resultado2 = motor.resolverTabla(new TablaConFilas("HOJA2", TipoTablaPlantilla.SIMPLE_LINEA,
                List.of(FilaPlantilla.simplePorLinea("5511112222"))));

        assertEquals(100.0, resultado2.filas().get(0).consumo().consumoMb());

        ResultadoMatching finalResult = motor.finalizar();

        // Con esta regla el total escrito (200) sí difiere del origen (100),
        // así que la consistencia SÍ debe marcar advertencia, y no hay aviso
        // explícito porque la regla personalizada decidió no generar uno.
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
}
