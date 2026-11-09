package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;
import com.simreports.core.NormalizadorClave;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * defecto {@link PrimeraEnLaHojaGanaReglaAmbigua}: entre hojas distintas
 * se cuenta en cada una (D-001) y dentro de una hoja solo la primera
 * (D-006). Todas las repeticiones salen en
 * {@link ResultadoMatching#simsRepetidas()}.</p>
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

    // Cada aparición con match de cada clave en esta corrida (hoja, fila y
    // MB asignados) — a partir de la 2a se delega en ReglaAmbigua, y las
    // claves con 2 o más forman el reporte de SIMs repetidas.
    private final Map<String, List<AparicionSim>> aparicionesAlestra = new HashMap<>();
    private final Map<String, List<AparicionSim>> aparicionesTelcelAtt = new HashMap<>();

    // Avisos generados por el motor mismo durante el matching (hoy, los
    // que produce ReglaAmbigua ante claves repetidas). Se combinan con
    // los avisos heredados de los parsers al finalizar.
    private final List<AvisoParseo> avisosGeneradosEnMatching = new ArrayList<>();

    // Suma literal de cada MB escrito en la plantilla que vino de cada
    // fuente — spec 03, pseudocódigo de consistencia de totales. No se
    // dedupea por clave: si ReglaAmbigua decide no repetir un valor,
    // simplemente no se vuelve a sumar.
    private double totalEscritoAlestra = 0.0;
    private double totalEscritoTelcelAtt = 0.0;

    // Parte de lo escrito que corresponde a una clave en una hoja donde
    // todavía no había aparecido, pero que ya había aparecido en otra (D-001):
    // MB repetidos a propósito, que la consistencia descuenta. Lo que una
    // regla vuelva a escribir dentro de la MISMA hoja no se descuenta: eso
    // sí es un doble conteo y debe disparar la advertencia.
    private double totalRepetidoEntreHojasAlestra = 0.0;
    private double totalRepetidoEntreHojasTelcelAtt = 0.0;

    public MotorCoincidencia(ResultadoParseo alestra, ResultadoParseo telcel, ResultadoParseo att) {
        this(alestra, telcel, att, new PrimeraEnLaHojaGanaReglaAmbigua());
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

    /** Dónde está la clave que se resuelve: para la regla ambigua y el reporte. */
    private record Ubicacion(String hoja, String tabla, Integer filaExcel, String campo) {
    }

    private ResultadoFilaPlantilla resolverFila(TablaConFilas tabla, FilaPlantilla filaOriginal) {
        FilaPlantilla fila = normalizarClaves(filaOriginal);
        return switch (tabla.tipo()) {
            case SIMPLE_LINEA, SIMPLE_ICCID, SIMPLE_CASCADA -> ResultadoFilaPlantilla.simple(resolverSimple(fila, tabla));
            case DUAL_SIM -> ResultadoFilaPlantilla.dualSim(
                    resolverContraAlestra(fila.linea1(), ubicacion(tabla, fila, "LINEA_1")),
                    resolverContraAlestra(fila.linea2(), ubicacion(tabla, fila, "LINEA_2")));
        };
    }

    /**
     * Tablas simples — decisión D-016 (spec 10): la columna {@code SIM} /
     * {@code Carrier} decide dónde se busca. Alestra (y Multicarrier) solo
     * por línea; Telcel y ATT solo por ICCID. Si la columna falta o trae otro
     * valor, se busca según el tipo de tabla, como antes.
     */
    private Asignacion resolverSimple(FilaPlantilla fila, TablaConFilas tabla) {
        return switch (OperadorPlantilla.desde(fila.sim())) {
            case ALESTRA -> {
                Asignacion a = resolverContraAlestra(fila.linea(), ubicacion(tabla, fila, "LINEA"));
                if (!a.tieneMatch() && fila.iccid() != null && telcelAtt.containsKey(fila.iccid())) {
                    avisarCarrierNoCoincide(fila, tabla, fila.iccid(), "Telcel/ATT", "Alestra");
                }
                yield a;
            }
            case TELCEL_ATT -> {
                Asignacion a = resolverContraTelcelAtt(fila.iccid(), ubicacion(tabla, fila, "ICCID"));
                if (!a.tieneMatch() && fila.linea() != null && alestra.containsKey(fila.linea())) {
                    avisarCarrierNoCoincide(fila, tabla, fila.linea(), "Alestra", "Telcel/ATT");
                }
                yield a;
            }
            case NO_RECONOCIDO -> switch (tabla.tipo()) {
                case SIMPLE_LINEA -> resolverContraAlestra(fila.linea(), ubicacion(tabla, fila, "LINEA"));
                case SIMPLE_ICCID -> resolverContraTelcelAtt(fila.iccid(), ubicacion(tabla, fila, "ICCID"));
                case SIMPLE_CASCADA -> resolverCascada(fila, tabla);
                case DUAL_SIM -> throw new IllegalStateException("DUAL_SIM no es una tabla simple");
            };
        };
    }

    private void avisarCarrierNoCoincide(
            FilaPlantilla fila, TablaConFilas tabla, String clave, String dondeSiEsta, String operadorDeLaFila) {
        String ubicacion = fila.filaExcel() == null
                ? "'" + tabla.hoja() + "'"
                : "'" + tabla.hoja() + "' fila " + fila.filaExcel();
        avisosGeneradosEnMatching.add(new AvisoParseo(TipoAviso.CARRIER_NO_COINCIDE, clave,
                "En " + ubicacion + " la columna SIM/Carrier dice '" + fila.sim().strip() + "' ("
                        + operadorDeLaFila + "), pero la SIM solo está en " + dondeSiEsta
                        + ". Se dejó en 0 MB; revisar el operador o la clave en la plantilla."));
    }

    private static Ubicacion ubicacion(TablaConFilas tabla, FilaPlantilla fila, String campo) {
        return new Ubicacion(tabla.hoja(), tabla.identificador(), fila.filaExcel(), campo);
    }

    /**
     * Aplica {@link NormalizadorClave} a las claves leídas de la plantilla,
     * la misma limpieza que ya aplicaron los parsers a las claves de
     * origen. Una clave que queda vacía se trata como celda en blanco
     * ({@code null}).
     */
    private static FilaPlantilla normalizarClaves(FilaPlantilla fila) {
        return new FilaPlantilla(
                claveONull(fila.linea()),
                claveONull(fila.iccid()),
                claveONull(fila.linea1()),
                claveONull(fila.linea2()),
                fila.sim(),
                fila.filaExcel());
    }

    private static String claveONull(String valor) {
        String normalizada = NormalizadorClave.normalizar(valor);
        return normalizada.isEmpty() ? null : normalizada;
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
    private Asignacion resolverCascada(FilaPlantilla fila, TablaConFilas tabla) {
        String linea = fila.linea();
        if (linea != null && alestra.containsKey(linea)) {
            return resolverContraAlestra(linea, ubicacion(tabla, fila, "LINEA"));
        }
        return resolverContraTelcelAtt(fila.iccid(), ubicacion(tabla, fila, "ICCID"));
    }

    private Asignacion resolverContraAlestra(String linea, Ubicacion ubicacion) {
        if (linea == null) {
            return Asignacion.sinMatch();
        }
        Double consumo = alestra.get(linea);
        if (consumo == null) {
            return Asignacion.sinMatch();
        }
        lineasConsultadasAlestra.add(linea);

        List<AparicionSim> previas = aparicionesAlestra.computeIfAbsent(linea, k -> new ArrayList<>());
        Asignacion asignacion = resolverAparicion(linea, FuenteConsumo.ALESTRA, consumo, ubicacion, previas);
        totalEscritoAlestra += asignacion.consumoMb();
        if (repetidaEntreHojas(previas, ubicacion)) {
            totalRepetidoEntreHojasAlestra += asignacion.consumoMb();
        }
        previas.add(aparicion(ubicacion, asignacion));
        return asignacion;
    }

    private Asignacion resolverContraTelcelAtt(String iccid, Ubicacion ubicacion) {
        if (iccid == null) {
            return Asignacion.sinMatch();
        }
        Double consumo = telcelAtt.get(iccid);
        if (consumo == null) {
            return Asignacion.sinMatch();
        }
        iccidsConsultadosTelcelAtt.add(iccid);

        List<AparicionSim> previas = aparicionesTelcelAtt.computeIfAbsent(iccid, k -> new ArrayList<>());
        Asignacion asignacion = resolverAparicion(iccid, FuenteConsumo.TELCEL_ATT, consumo, ubicacion, previas);
        totalEscritoTelcelAtt += asignacion.consumoMb();
        if (repetidaEntreHojas(previas, ubicacion)) {
            totalRepetidoEntreHojasTelcelAtt += asignacion.consumoMb();
        }
        previas.add(aparicion(ubicacion, asignacion));
        return asignacion;
    }

    /**
     * Primera aparición: el consumo de la fuente. De la segunda en adelante:
     * lo que decida la {@link ReglaAmbigua}.
     */
    private Asignacion resolverAparicion(
            String clave,
            FuenteConsumo fuente,
            double consumoMb,
            Ubicacion ubicacion,
            List<AparicionSim> previas) {
        if (previas.isEmpty()) {
            return new Asignacion(consumoMb, fuente);
        }
        ContextoFila contexto = new ContextoFila(clave, fuente, consumoMb,
                ubicacion.hoja(), ubicacion.tabla(), ubicacion.filaExcel(),
                previas.size() + 1, yaAparecioEnLaHoja(previas, ubicacion.hoja()));
        ResolucionAmbigua resolucion = reglaAmbigua.resolver(contexto);
        resolucion.advertenciaOpcional().ifPresent(detalle ->
                avisosGeneradosEnMatching.add(new AvisoParseo(TipoAviso.CLAVE_DUPLICADA, clave, detalle)));
        return resolucion.asignacion();
    }

    /** La clave ya apareció en otra hoja, pero todavía no en esta (D-001). */
    private static boolean repetidaEntreHojas(List<AparicionSim> previas, Ubicacion ubicacion) {
        return !previas.isEmpty() && !yaAparecioEnLaHoja(previas, ubicacion.hoja());
    }

    private static boolean yaAparecioEnLaHoja(List<AparicionSim> previas, String hoja) {
        for (AparicionSim previa : previas) {
            if (previa.hoja().equals(hoja)) {
                return true;
            }
        }
        return false;
    }

    private static AparicionSim aparicion(Ubicacion ubicacion, Asignacion asignacion) {
        return new AparicionSim(ubicacion.hoja(), ubicacion.tabla(), ubicacion.filaExcel(),
                ubicacion.campo(), asignacion.consumoMb());
    }

    /**
     * Cierra el matching y calcula "no encontrados", totales por fuente y
     * consistencia de totales.
     *
     * <p>Alcance real de la consistencia interna: compara contadores que el
     * propio motor lleva, así que solo puede saltar si una
     * {@link ReglaAmbigua} vuelve a escribir el consumo de una clave
     * repetida <b>dentro de la misma hoja</b> (MB duplicados). Lo repetido
     * entre hojas distintas (D-001) es intencional y se descuenta
     * ({@link TotalesFuente#totalRepetidoEntreHojas()}). <b>No</b> verifica lo que quedó escrito en
     * el .xlsx: para eso está {@link VerificadorReporteEscrito}, que debe
     * llamarse después de escribir el archivo.</p>
     *
     * <p>Debe llamarse una sola vez, después de resolver <b>todas</b>
     * las tablas de la plantilla con {@link #resolverTabla} /
     * {@link #resolverTablas}. Llamarla antes de tiempo produce
     * "no encontrados" incompletos (claves que sí hubieran matcheado en
     * una hoja aún no procesada).</p>
     */
    public ResultadoMatching finalizar() {
        List<NoEncontrado> noEncontrados = new ArrayList<>();
        double totalNoEncontradoAlestra = acumularNoEncontrados(
                alestra, lineasConsultadasAlestra, FuenteConsumo.ALESTRA, noEncontrados);
        double totalNoEncontradoTelcelAtt = acumularNoEncontrados(
                telcelAtt, iccidsConsultadosTelcelAtt, FuenteConsumo.TELCEL_ATT, noEncontrados);
        // Orden estable (fuente, clave): los Map de origen no garantizan
        // orden, y el reporte debe salir igual en cada corrida.
        noEncontrados.sort(Comparator.comparing(NoEncontrado::fuente).thenComparing(NoEncontrado::clave));

        double totalOrigenAlestra = sumar(alestra);
        double totalOrigenTelcelAtt = sumar(telcelAtt);

        List<TotalesFuente> totales = List.of(
                new TotalesFuente(FuenteConsumo.ALESTRA, totalOrigenAlestra, totalEscritoAlestra,
                        totalNoEncontradoAlestra, totalRepetidoEntreHojasAlestra),
                new TotalesFuente(FuenteConsumo.TELCEL_ATT, totalOrigenTelcelAtt, totalEscritoTelcelAtt,
                        totalNoEncontradoTelcelAtt, totalRepetidoEntreHojasTelcelAtt));

        List<AdvertenciaConsistencia> advertencias = new ArrayList<>();
        for (TotalesFuente t : totales) {
            agregarSiInconsistente(advertencias, t);
        }

        List<AvisoParseo> avisos = new ArrayList<>(avisosHeredados);
        avisos.addAll(avisosGeneradosEnMatching);

        List<SimRepetida> repetidas = new ArrayList<>();
        acumularRepetidas(aparicionesAlestra, FuenteConsumo.ALESTRA, alestra, repetidas);
        acumularRepetidas(aparicionesTelcelAtt, FuenteConsumo.TELCEL_ATT, telcelAtt, repetidas);
        repetidas.sort(Comparator.comparing(SimRepetida::fuente).thenComparing(SimRepetida::clave));

        return new ResultadoMatching(noEncontrados, advertencias, avisos, totales, repetidas);
    }

    private static void acumularRepetidas(
            Map<String, List<AparicionSim>> apariciones,
            FuenteConsumo fuente,
            Map<String, Double> origen,
            List<SimRepetida> destino) {
        for (Map.Entry<String, List<AparicionSim>> entrada : apariciones.entrySet()) {
            if (entrada.getValue().size() >= 2) {
                destino.add(new SimRepetida(fuente, entrada.getKey(), origen.get(entrada.getKey()),
                        entrada.getValue()));
            }
        }
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

    private static void agregarSiInconsistente(List<AdvertenciaConsistencia> destino, TotalesFuente t) {
        AdvertenciaConsistencia candidata = new AdvertenciaConsistencia(t.fuente(), t.totalOrigen(),
                t.totalAsignado(), t.totalNoEncontrado(), t.totalRepetidoEntreHojas());
        if (Math.abs(candidata.diferencia()) > MARGEN_TOLERANCIA_MB) {
            destino.add(candidata);
        }
    }
}