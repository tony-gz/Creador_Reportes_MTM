package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;
import com.simreports.core.TipoAviso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reporte final de la corrida en texto, para mostrarlo al usuario al
 * terminar (UI y hoja de resumen del .xlsx de salida, Fase 2 punto 4).
 * Decisiones D-006 y D-011 (spec 10).
 *
 * <p>Secciones, siempre en este orden y aunque estén vacías:</p>
 * <ol>
 *   <li>Totales por fuente.</li>
 *   <li>SIMs repetidas dentro de una hoja: cuál fila conservó el consumo y
 *       cuáles quedaron en 0 (D-006).</li>
 *   <li>SIMs repetidas en hojas distintas: se cuentan en cada hoja
 *       (D-001).</li>
 *   <li>Consumo sin cliente: la lista de líneas e ICCID con consumo que no
 *       están en ninguna hoja de la plantilla, de mayor a menor consumo
 *       (D-011).</li>
 *   <li>Advertencias de consistencia de totales.</li>
 *   <li>Otros avisos: los de los archivos de origen y los de filas cuyo
 *       operador (columna SIM/Carrier) no coincide con donde está la SIM
 *       (D-016). Se omiten los avisos de clave repetida en la plantilla,
 *       porque ya salen en las secciones 2 y 3.</li>
 * </ol>
 */
public final class ReporteFinalCorrida {

    private static final Locale MX = Locale.forLanguageTag("es-MX");

    private ReporteFinalCorrida() {
    }

    public static String generar(ResultadoMatching resultado) {
        Objects.requireNonNull(resultado, "resultado no puede ser null");
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE FINAL DE LA CORRIDA\n");
        seccionTotales(sb, resultado);
        seccionRepetidasMismaHoja(sb, resultado);
        seccionRepetidasEntreHojas(sb, resultado);
        seccionSinCliente(sb, resultado);
        seccionConsistencia(sb, resultado);
        seccionAvisos(sb, resultado);
        return sb.toString();
    }

    private static void seccionTotales(StringBuilder sb, ResultadoMatching resultado) {
        sb.append("\n1. Totales por fuente\n");
        for (TotalesFuente t : resultado.totales()) {
            sb.append("   ").append(t.fuente()).append(": origen ").append(mb(t.totalOrigen()))
                    .append(" | asignado ").append(mb(t.totalAsignado()))
                    .append(" | sin cliente ").append(mb(t.totalNoEncontrado()));
            if (t.totalRepetidoEntreHojas() != 0.0) {
                sb.append(" | contado de nuevo en otra hoja ").append(mb(t.totalRepetidoEntreHojas()));
            }
            sb.append('\n');
        }
    }

    private static void seccionRepetidasMismaHoja(StringBuilder sb, ResultadoMatching resultado) {
        List<String> lineas = new ArrayList<>();
        for (SimRepetida sim : resultado.simsRepetidas()) {
            for (Map.Entry<String, List<AparicionSim>> hoja : porHoja(sim).entrySet()) {
                List<AparicionSim> apariciones = hoja.getValue();
                if (apariciones.size() < 2) {
                    continue;
                }
                StringBuilder l = new StringBuilder("   - ").append(hoja.getKey()).append(" · ")
                        .append(sim.fuente()).append(' ').append(sim.clave())
                        .append(" (").append(mb(sim.consumoMb())).append("): ");
                List<String> conConsumo = new ArrayList<>();
                List<String> enCero = new ArrayList<>();
                for (AparicionSim a : apariciones) {
                    (a.consumoAsignado() != 0.0 || a == apariciones.get(0) ? conConsumo : enCero).add(fila(a));
                }
                l.append(String.join(", ", conConsumo)).append(" conserva el consumo");
                if (!enCero.isEmpty()) {
                    l.append("; ").append(String.join(", ", enCero)).append(" en 0");
                }
                lineas.add(l.toString());
            }
        }
        sb.append("\n2. SIMs repetidas dentro de una hoja (la primera conserva el consumo): ")
                .append(lineas.size()).append('\n');
        lineas.forEach(l -> sb.append(l).append('\n'));
    }

    private static void seccionRepetidasEntreHojas(StringBuilder sb, ResultadoMatching resultado) {
        List<String> lineas = new ArrayList<>();
        for (SimRepetida sim : resultado.simsRepetidas()) {
            if (!sim.repetidaEntreHojas()) {
                continue;
            }
            List<String> ubicaciones = new ArrayList<>();
            for (Map.Entry<String, List<AparicionSim>> hoja : porHoja(sim).entrySet()) {
                ubicaciones.add(hoja.getKey() + " " + fila(hoja.getValue().get(0)));
            }
            lineas.add("   - " + sim.fuente() + " " + sim.clave() + " (" + mb(sim.consumoMb()) + " en cada hoja): "
                    + String.join("; ", ubicaciones));
        }
        sb.append("\n3. SIMs repetidas en hojas distintas (se cuentan en cada hoja): ")
                .append(lineas.size()).append('\n');
        lineas.forEach(l -> sb.append(l).append('\n'));
    }

    private static void seccionSinCliente(StringBuilder sb, ResultadoMatching resultado) {
        List<NoEncontrado> lista = new ArrayList<>(resultado.noEncontrados());
        lista.sort(Comparator.comparing(NoEncontrado::fuente)
                .thenComparing(Comparator.comparingDouble(NoEncontrado::consumoMb).reversed())
                .thenComparing(NoEncontrado::clave));
        sb.append("\n4. Consumo sin cliente (con consumo en la fuente, en ninguna hoja de la plantilla): ")
                .append(lista.size()).append('\n');
        for (FuenteConsumo fuente : FuenteConsumo.values()) {
            List<NoEncontrado> deFuente = lista.stream().filter(n -> n.fuente() == fuente).toList();
            if (deFuente.isEmpty()) {
                continue;
            }
            double total = deFuente.stream().mapToDouble(NoEncontrado::consumoMb).sum();
            sb.append("   ").append(fuente).append(": ").append(deFuente.size())
                    .append(fuente == FuenteConsumo.ALESTRA ? " líneas, " : " ICCID, ")
                    .append(mb(total)).append('\n');
            int i = 1;
            for (NoEncontrado n : deFuente) {
                sb.append(String.format(MX, "   %4d. %-22s %18s%n", i++, n.clave(), mb(n.consumoMb())));
            }
        }
    }

    private static void seccionConsistencia(StringBuilder sb, ResultadoMatching resultado) {
        sb.append("\n5. Consistencia de totales: ");
        if (resultado.advertenciasConsistencia().isEmpty()) {
            sb.append("cuadra\n");
            return;
        }
        sb.append(resultado.advertenciasConsistencia().size()).append(" descuadre(s)\n");
        for (AdvertenciaConsistencia a : resultado.advertenciasConsistencia()) {
            sb.append("   - ").append(a.fuente()).append(": diferencia ").append(mb(a.diferencia())).append('\n');
        }
    }

    private static void seccionAvisos(StringBuilder sb, ResultadoMatching resultado) {
        Set<String> clavesRepetidas = new HashSet<>();
        resultado.simsRepetidas().forEach(s -> clavesRepetidas.add(s.clave()));
        List<AvisoParseo> avisos = resultado.avisosParseo().stream()
                .filter(a -> !(a.tipo() == TipoAviso.CLAVE_DUPLICADA && clavesRepetidas.contains(a.clave())))
                .toList();
        sb.append("\n6. Otros avisos: ").append(avisos.size()).append('\n');
        for (AvisoParseo a : avisos) {
            sb.append("   - ").append(a.tipo()).append(' ').append(a.clave()).append(": ")
                    .append(a.detalle()).append('\n');
        }
    }

    private static Map<String, List<AparicionSim>> porHoja(SimRepetida sim) {
        Map<String, List<AparicionSim>> porHoja = new LinkedHashMap<>();
        for (AparicionSim a : sim.apariciones()) {
            porHoja.computeIfAbsent(a.hoja(), k -> new ArrayList<>()).add(a);
        }
        return porHoja;
    }

    private static String fila(AparicionSim a) {
        String fila = a.filaExcel() == null ? "fila ?" : "fila " + a.filaExcel();
        return a.campo().startsWith("LINEA_") ? fila + " (" + a.campo() + ")" : fila;
    }

    private static String mb(double valor) {
        return String.format(MX, "%,.2f MB", valor);
    }
}
