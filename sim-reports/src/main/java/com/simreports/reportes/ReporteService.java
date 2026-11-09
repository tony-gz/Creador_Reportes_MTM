package com.simreports.reportes;

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
import com.simreports.core.plantilla.PlantillaInvalidaException;
import com.simreports.core.plantilla.PlantillaLeida;
import com.simreports.core.reporte.EscritorReporte;
import com.simreports.core.reporte.ReporteEscrito;
import com.simreports.plantilla.PlantillaService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;

/**
 * Genera un reporte — D-023 (spec 12): todo en memoria, nada se guarda.
 *
 * <p>Lee la plantilla subida, parsea los 3 archivos, corre el motor, escribe
 * el .xlsx, lo <b>relee</b> y verifica que contenga los MB asignados
 * ({@link VerificadorReporteEscrito}).</p>
 */
@Service
public class ReporteService {

    private final PlantillaService plantillaService;
    private final EscritorReporte escritor = new EscritorReporte();

    public ReporteService(PlantillaService plantillaService) {
        this.plantillaService = Objects.requireNonNull(plantillaService, "plantillaService no puede ser null");
    }

    /**
     * @throws PlantillaInvalidaException si la plantilla tiene problemas (todos en la lista).
     * @throws ArchivoFormatoInvalidoException si un archivo de operador no se reconoce;
     *         el mensaje dice cuál.
     * @throws ReporteDescuadradoException si el archivo escrito no cuadra (error del escritor).
     */
    public ReporteResultado generar(Periodo periodo, ArchivoEntrada plantilla, ArchivoEntrada alestra,
                                    ArchivoEntrada telcel, ArchivoEntrada att) {
        Objects.requireNonNull(periodo, "periodo no puede ser null");
        Objects.requireNonNull(plantilla, "falta la plantilla");
        PlantillaLeida leida = plantillaService.leer(plantilla.contenido());

        ResultadoParseo deAlestra = parsear(new AlestraParser(), "Alestra", alestra);
        ResultadoParseo deTelcel = parsear(new TelcelParser(), "Telcel", telcel);
        ResultadoParseo deAtt = parsear(new AttParser(), "ATT", att);

        MotorCoincidencia motor = new MotorCoincidencia(deAlestra, deTelcel, deAtt);
        List<ResultadoTabla> resultados = motor.resolverTablas(leida.tablasConFilas());
        ResultadoMatching resultado = motor.finalizar();

        ReporteEscrito escrito = escritor.escribir(plantilla.contenido(), leida, resultados);
        double leido = EscritorReporte.sumarConsumos(escrito.contenido(), leida);
        VerificadorReporteEscrito.verificar(resultado, leido).ifPresent(d -> {
            throw new ReporteDescuadradoException(d);
        });

        String reporteFinal = ReporteFinalCorrida.generar(resultado);
        if (escrito.formulasSinRecalcular() > 0) {
            reporteFinal += "\nNota: " + escrito.formulasSinRecalcular()
                    + " fórmula(s) no se pudieron recalcular; Excel las recalcula al abrir el archivo.\n";
        }
        return new ReporteResultado(periodo, escrito.contenido(), reporteFinal, resultado,
                escrito.formulasSinRecalcular(), List.of(
                        ReporteResultado.ArchivoUsado.de("PLANTILLA", plantilla),
                        ReporteResultado.ArchivoUsado.de("ALESTRA", alestra),
                        ReporteResultado.ArchivoUsado.de("TELCEL", telcel),
                        ReporteResultado.ArchivoUsado.de("ATT", att)));
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
}
