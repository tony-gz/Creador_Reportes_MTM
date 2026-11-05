package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleConsumer;

/**
 * Motor de coincidencia — spec 03. Puro: no conoce Excel ni Spring.
 * Recibe el mapeo ya resuelto de cada tabla de la plantilla (spec 01,
 * vía {@link TablaConFilas}) y los 3 {@link ResultadoParseo} ya
 * parseados (spec 02), y devuelve los valores a escribir por fila, el
 * reporte de "no encontrados" y el reporte de consistencia de totales.
 *
 * <p><b>Flujo de uso (dos fases, en este orden):</b></p>
 * <ol>
 *   <li>Llamar a {@link #resolverTabla} (o {@link #resolverTablas}) una
 *       vez por cada tabla de las 18 hojas, guardando los
 *       {@link ResultadoTabla} devueltos para escribirlos luego en el
 *       Excel de salida.</li>
 *   <li>Una vez agotadas **todas** las tablas, llamar a
 *       {@link #finalizar()} exactamente una vez para obtener "no
 *       encontrados" y la consistencia de totales — estos reportes solo
 *       tienen sentido después de saber qué se consultó en las 18
 *       hojas.</li>
 * </ol>
 *
 * <p>Si una clave (línea o ICCID) se consulta con éxito más de una vez
 * durante la corrida (misma hoja o hojas distintas), la segunda
 * aparición en adelante se delega en una {@link ReglaAmbigua} — por
 * defecto {@link PrimeraCoincidenciaGanaReglaAmbigua} — en vez de
 * repetir el valor automáticamente (spec 03, "Reglas ambiguas
 * pendientes de confirmar").</p>
 *
 * <p>Una instancia de {@code MotorCoincidencia} se usa para una sola
 * corrida completa (una subida de las 3 BDs + toda la plantilla); no se
 * reutiliza entre corridas.</p>
 */
public class MotorCoincidencia {

    /**
     * Tolerancia mínima por redondeo de {@code double} en la validación
     * de consistencia (spec 03: "no es una tolerancia de negocio").
     */
    private static final double MARGEN_TOLERANCIA_MB = 0.01;

    private final Map<String, Double> alestra;
    private final Map<String, Double> telcelAtt;
    private final List<AvisoParseo> avisosHeredados;
    private final ReglaAmbigua reglaAmbigua;

    // Claves que matchearon al menos una vez durante el matching, para
    // poder calcular al final qué quedó sin consultar ("no encontrados").
    private final Set<String> lineasConsultadasAlestra = new HashSet<>();
    private final Set<String> iccidsConsultadosTelcelAtt = new HashSet<>();

    // Cuántas veces se ha consultado con match cada clave en esta
    // corrida — a partir de la 2a aparición se delega en ReglaAmbigua.
    private final Map<String, Integer> aparicionesAlestra = new HashMap<>();
    private final Map<String, Integer> aparicionesTelcelAtt = new HashMap<>();

    // Avisos generados por el motor mismo durante el matching (hoy, los
    // que produce ReglaAmbigua ante claves repetidas). Se combinan con
    // los avisos heredados de los parsers al finalizar.
    private final List<AvisoParseo> avisosGeneradosEnMatching = new ArrayList<>();

    // Suma literal de cada MB escrito en la plantilla que vino de cada
    // fuente — spec 03, pseudocódigo de consistencia de totales. No se
    // dedupea por clave: si ReglaAmbigua decide no repetir un valor (como
    // hace la implementación default), simplemente no se vuelve a sumar.
    private double totalEscritoAlestra = 0.0;
    private double totalEscritoTelcelAtt = 0.0;

    public MotorCoincidencia(ResultadoParseo alestra, ResultadoParseo telcel, ResultadoParseo att) {
        this(alestra, telcel, att, new PrimeraCoincidenciaGanaReglaAmbigua());
    }

    public MotorCoincidencia(
            ResultadoParseo alestra,
            ResultadoParseo telcel,
            ResultadoParseo att,
            ReglaAmbigua reglaAmbigua) {
        Objects.requireNonNull(alestra, "alestra no puede ser null");
        Objects.requireNonNull(telcel, "telcel no puede ser null");
        Objects.requireNonNull(att, "att no puede ser null");
        this.reglaAmbigua = Objects.requireNonNull(reglaAmbigua, "reglaAmbigua no puede ser null");

        this.alestra = alestra.consumos();

        ResultadoParseo telcelAttCombinado = CombinadorTelcelAtt.combinar(telcel, att);
        this.telcelAtt = telcelAttCombinado.consumos();

        List<AvisoParseo> avisos = new ArrayList<>(alestra.avisos());
        avisos.addAll(telcelAttCombinado.avisos());
        this.avisosHeredados = List.copyOf(avisos);
    }

    /**
     * Resuelve varias tablas de una sola vez. Equivalente a llamar
     * {@link #resolverTabla} por cada elemento, en orden.
     */
    public List<ResultadoTabla> resolverTablas(List<TablaConFilas> tablas) {
        Objects.requireNonNull(tablas, "tablas no puede ser null");
        List<ResultadoTabla> resultado = new ArrayList<>();
        for (TablaConFilas tabla : tablas) {
            resultado.add(resolverTabla(tabla));
        }
        return resultado;
    }

    /** Resuelve una sola tabla. Ver flujo de uso en el Javadoc de la clase. */
    public ResultadoTabla resolverTabla(TablaConFilas tabla) {
        Objects.requireNonNull(tabla, "tabla no puede ser null");
        List<ResultadoFilaPlantilla> filas = new ArrayList<>();
        for (FilaPlantilla fila : tabla.filas()) {
            filas.add(resolverFila(tabla, fila));
        }
        return new ResultadoTabla(tabla.identificador(), filas);
    }

    private ResultadoFilaPlantilla resolverFila(TablaConFilas tabla, FilaPlantilla fila) {
        String id = tabla.identificador();
        return switch (tabla.tipo()) {
            case SIMPLE_LINEA -> ResultadoFilaPlantilla.simple(resolverContraAlestra(fila.linea(), id));
            case SIMPLE_ICCID -> ResultadoFilaPlantilla.simple(resolverContraTelcelAtt(fila.iccid(), id));
            case SIMPLE_CASCADA -> ResultadoFilaPlantilla.simple(resolverCascada(fila.linea(), fila.iccid(), id));
            case DUAL_SIM -> ResultadoFilaPlantilla.dualSim(
                    resolverContraAlestra(fila.linea1(), id),
                    resolverContraAlestra(fila.linea2(), id));
        };
    }

    /**
     * Cascada de spec 03, regla 1: primero {@code LINEA} contra Alestra;
     * si no hay match, {@code ICCID} contra Telcel + ATT. No se usa la
     * columna {@code SIM} (operador) para decidir dónde buscar — se
     * asume que la misma línea/ICCID nunca aparece en dos operadores a
     * la vez (spec 03).
     *
     * <p>La decisión de caer a {@code ICCID} se toma según si la línea
     * <b>existe</b> en Alestra, no según la {@link Asignacion} devuelta:
     * cuando la línea está repetida en la plantilla, la {@link ReglaAmbigua}
     * puede devolver una asignación sin fuente (p. ej. la regla default deja
     * la fila en 0 MB), y eso no significa que la línea no haya matcheado.
     * Si se usara {@code tieneMatch()} para decidir, la fila repetida
     * buscaría su ICCID en Telcel + ATT y podría quedarse con el consumo
     * de otro operador.</p>
     */
    private Asignacion resolverCascada(String linea, String iccid, String tablaIdentificador) {
        if (linea != null && alestra.containsKey(linea)) {
            return resolverContraAlestra(linea, tablaIdentificador);
        }
        return resolverContraTelcelAtt(iccid, tablaIdentificador);
    }

    private Asignacion resolverContraAlestra(String linea, String tablaIdentificador) {
        if (linea == null) {
            return Asignacion.sinMatch();
        }
        Double consumo = alestra.get(linea);
        if (consumo == null) {
            return Asignacion.sinMatch();
        }
        lineasConsultadasAlestra.add(linea);

        int aparicion = aparicionesAlestra.merge(linea, 1, Integer::sum);
        if (aparicion >= 2) {
            return resolverAmbiguo(linea, FuenteConsumo.ALESTRA, consumo, tablaIdentificador, aparicion,
                    valor -> totalEscritoAlestra += valor);
        }

        totalEscritoAlestra += consumo;
        return new Asignacion(consumo, FuenteConsumo.ALESTRA);
    }

    private Asignacion resolverContraTelcelAtt(String iccid, String tablaIdentificador) {
        if (iccid == null) {
            return Asignacion.sinMatch();
        }
        Double consumo = telcelAtt.get(iccid);
        if (consumo == null) {
            return Asignacion.sinMatch();
        }
        iccidsConsultadosTelcelAtt.add(iccid);

        int aparicion = aparicionesTelcelAtt.merge(iccid, 1, Integer::sum);
        if (aparicion >= 2) {
            return resolverAmbiguo(iccid, FuenteConsumo.TELCEL_ATT, consumo, tablaIdentificador, aparicion,
                    valor -> totalEscritoTelcelAtt += valor);
        }

        totalEscritoTelcelAtt += consumo;
        return new Asignacion(consumo, FuenteConsumo.TELCEL_ATT);
    }

    private Asignacion resolverAmbiguo(
            String clave,
            FuenteConsumo fuente,
            double consumoMb,
            String tablaIdentificador,
            int numeroDeAparicion,
            DoubleConsumer acumularEnTotalEscrito) {
        ContextoFila contexto = new ContextoFila(clave, fuente, consumoMb, tablaIdentificador, numeroDeAparicion);
        ResolucionAmbigua resolucion = reglaAmbigua.resolver(contexto);

        acumularEnTotalEscrito.accept(resolucion.asignacion().consumoMb());
        resolucion.advertenciaOpcional().ifPresent(detalle ->
                avisosGeneradosEnMatching.add(new AvisoParseo(TipoAviso.CLAVE_DUPLICADA, clave, detalle)));

        return resolucion.asignacion();
    }

    /**
     * Cierra el matching y calcula "no encontrados" y consistencia de
     * totales. Debe llamarse una sola vez, después de resolver **todas**
     * las tablas de la plantilla con {@link #resolverTabla} /
     * {@link #resolverTablas}. Llamarla antes de tiempo produce
     * "no encontrados" incompletos (claves que sí hubieran matcheado en
     * una hoja aún no procesada).
     */
    public ResultadoMatching finalizar() {
        List<NoEncontrado> noEncontrados = new ArrayList<>();
        double totalNoEncontradoAlestra = acumularNoEncontrados(
                alestra, lineasConsultadasAlestra, FuenteConsumo.ALESTRA, noEncontrados);
        double totalNoEncontradoTelcelAtt = acumularNoEncontrados(
                telcelAtt, iccidsConsultadosTelcelAtt, FuenteConsumo.TELCEL_ATT, noEncontrados);

        double totalOrigenAlestra = sumar(alestra);
        double totalOrigenTelcelAtt = sumar(telcelAtt);

        List<AdvertenciaConsistencia> advertencias = new ArrayList<>();
        agregarSiInconsistente(advertencias, FuenteConsumo.ALESTRA,
                totalOrigenAlestra, totalEscritoAlestra, totalNoEncontradoAlestra);
        agregarSiInconsistente(advertencias, FuenteConsumo.TELCEL_ATT,
                totalOrigenTelcelAtt, totalEscritoTelcelAtt, totalNoEncontradoTelcelAtt);

        List<AvisoParseo> avisos = new ArrayList<>(avisosHeredados);
        avisos.addAll(avisosGeneradosEnMatching);

        return new ResultadoMatching(noEncontrados, advertencias, avisos);
    }

    private static double sumar(Map<String, Double> mapa) {
        double total = 0.0;
        for (double valor : mapa.values()) {
            total += valor;
        }
        return total;
    }

    private static double acumularNoEncontrados(
            Map<String, Double> origen,
            Set<String> consultadas,
            FuenteConsumo fuente,
            List<NoEncontrado> destino) {
        double total = 0.0;
        for (Map.Entry<String, Double> entrada : origen.entrySet()) {
            if (!consultadas.contains(entrada.getKey())) {
                destino.add(new NoEncontrado(fuente, entrada.getKey(), entrada.getValue()));
                total += entrada.getValue();
            }
        }
        return total;
    }

    private static void agregarSiInconsistente(
            List<AdvertenciaConsistencia> destino,
            FuenteConsumo fuente,
            double totalOrigen,
            double totalAsignado,
            double totalNoEncontrado) {
        double diferencia = totalOrigen - (totalAsignado + totalNoEncontrado);
        if (Math.abs(diferencia) > MARGEN_TOLERANCIA_MB) {
            destino.add(new AdvertenciaConsistencia(fuente, totalOrigen, totalAsignado, totalNoEncontrado));
        }
    }
}