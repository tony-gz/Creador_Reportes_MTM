package com.simreports.reportes;

import com.simreports.almacenamiento.AlmacenamientoArchivos;
import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.OrigenParser;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.matching.MotorCoincidencia;
import com.simreports.core.matching.ReporteFinalCorrida;
import com.simreports.core.matching.ResultadoMatching;
import com.simreports.core.matching.ResultadoTabla;
import com.simreports.core.matching.VerificadorReporteEscrito;
import com.simreports.core.parsers.AlestraParser;
import com.simreports.core.parsers.AttParser;
import com.simreports.core.parsers.TelcelParser;
import com.simreports.core.reporte.EscritorReporte;
import com.simreports.core.reporte.ReporteEscrito;
import com.simreports.plantilla.PlantillaParaReporte;
import com.simreports.plantilla.PlantillaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de reportes — Fase 2 punto 4 (spec 08).
 *
 * <p>{@link #generar}: con la plantilla activa (con lock, que dura toda la
 * corrida), parsea los 3 archivos, corre el motor, escribe el .xlsx, lo
 * <b>relee</b> y verifica que contenga los MB asignados
 * ({@link VerificadorReporteEscrito}), guarda el archivo y registra la corrida
 * con su reporte final. Si la verificación falla no se guarda nada.</p>
 */
@Service
public class ReporteService {

    private static final DateTimeFormatter CARPETA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final PlantillaService plantillaService;
    private final ReporteGeneradoRepository repositorio;
    private final AlmacenamientoArchivos almacenamiento;
    private final JsonMapper jsonMapper;
    private final EscritorReporte escritor = new EscritorReporte();

    public ReporteService(
            PlantillaService plantillaService,
            ReporteGeneradoRepository repositorio,
            AlmacenamientoArchivos almacenamiento,
            JsonMapper jsonMapper) {
        this.plantillaService = Objects.requireNonNull(plantillaService, "plantillaService no puede ser null");
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio no puede ser null");
        this.almacenamiento = Objects.requireNonNull(almacenamiento, "almacenamiento no puede ser null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper no puede ser null");
    }

    /**
     * Genera el reporte de un periodo.
     *
     * @throws com.simreports.plantilla.SinPlantillaActivaException si no hay plantilla.
     * @throws ArchivoFormatoInvalidoException si un archivo de operador no se reconoce;
     *         el mensaje dice cuál.
     * @throws com.simreports.core.plantilla.PlantillaInvalidaException si la plantilla
     *         activa ya no coincide con su mapeo.
     * @throws ReporteDescuadradoException si el archivo escrito no cuadra (error del escritor).
     */
    @Transactional
    public ReporteResultado generar(Periodo periodo, ArchivoEntrada alestra, ArchivoEntrada telcel, ArchivoEntrada att) {
        Objects.requireNonNull(periodo, "periodo no puede ser null");
        PlantillaParaReporte plantilla = plantillaService.leerActivaParaReporte();

        ResultadoParseo deAlestra = parsear(new AlestraParser(), "Alestra", alestra);
        ResultadoParseo deTelcel = parsear(new TelcelParser(), "Telcel", telcel);
        ResultadoParseo deAtt = parsear(new AttParser(), "ATT", att);

        MotorCoincidencia motor = new MotorCoincidencia(deAlestra, deTelcel, deAtt);
        List<ResultadoTabla> resultados = motor.resolverTablas(plantilla.leida().tablasConFilas());
        ResultadoMatching resultado = motor.finalizar();

        ReporteEscrito escrito = escritor.escribir(plantilla.contenido(), plantilla.leida(), resultados);
        double leido = EscritorReporte.sumarConsumos(escrito.contenido(), plantilla.leida());
        VerificadorReporteEscrito.verificar(resultado, leido).ifPresent(d -> {
            throw new ReporteDescuadradoException(d);
        });

        String carpeta = LocalDateTime.now().format(CARPETA) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String ruta = almacenamiento.guardarReporte(carpeta, periodo.nombreArchivo(), escrito.contenido());

        String reporteFinal = ReporteFinalCorrida.generar(resultado);
        if (escrito.formulasSinRecalcular() > 0) {
            reporteFinal += "\nNota: " + escrito.formulasSinRecalcular()
                    + " fórmula(s) no se pudieron recalcular; Excel las recalcula al abrir el archivo.\n";
        }
        ReporteGenerado registro = repositorio.save(new ReporteGenerado(
                plantilla.version(),
                periodo.anio(),
                periodo.mes(),
                periodo.nombreArchivo(),
                jsonMapper.writeValueAsString(List.of(
                        describir("ALESTRA", alestra), describir("TELCEL", telcel), describir("ATT", att))),
                ruta,
                jsonMapper.writeValueAsString(resultado.noEncontrados()),
                jsonMapper.writeValueAsString(avisos(resultado, escrito)),
                reporteFinal));
        return new ReporteResultado(registro, resultado);
    }

    /** Historial de corridas, la más reciente primero. */
    @Transactional(readOnly = true)
    public List<ReporteGenerado> listar() {
        return repositorio.findAllByOrderByFechaDescIdDesc();
    }

    /** Una corrida. */
    @Transactional(readOnly = true)
    public ReporteGenerado obtener(Long id) {
        return repositorio.findConVersionById(id).orElseThrow(() -> new ReporteNoEncontradoException(id));
    }

    /** El .xlsx generado en una corrida. */
    @Transactional(readOnly = true)
    public ArchivoDescarga descargar(Long id) {
        ReporteGenerado reporte = obtener(id);
        return new ArchivoDescarga(reporte.getNombreArchivo(), almacenamiento.leer(reporte.getRutaArchivoResultado()));
    }

    private static ResultadoParseo parsear(OrigenParser parser, String operador, ArchivoEntrada archivo) {
        Objects.requireNonNull(archivo, "falta el archivo de " + operador);
        try {
            return parser.parsear(new ByteArrayInputStream(archivo.contenido()));
        } catch (ArchivoFormatoInvalidoException e) {
            throw new ArchivoFormatoInvalidoException(
                    "Archivo de " + operador + " ('" + archivo.nombre() + "'): " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> describir(String fuente, ArchivoEntrada archivo) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("fuente", fuente);
        d.put("nombre", archivo.nombre());
        d.put("bytes", archivo.contenido().length);
        d.put("sha256", sha256(archivo.contenido()));
        return d;
    }

    private static Map<String, Object> avisos(ResultadoMatching resultado, ReporteEscrito escrito) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("totales", resultado.totales());
        a.put("advertenciasConsistencia", resultado.advertenciasConsistencia());
        a.put("simsRepetidas", resultado.simsRepetidas());
        a.put("avisosParseo", resultado.avisosParseo());
        a.put("formulasSinRecalcular", escrito.formulasSinRecalcular());
        return a;
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
