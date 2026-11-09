package com.simreports.reportes;

import com.simreports.core.matching.ResultadoMatching;

import java.util.List;
import java.util.Objects;

/**
 * Lo que devuelve una corrida. Vive solo en memoria mientras se arma la
 * respuesta: no se guarda (D-023).
 *
 * @param contenido             el .xlsx generado.
 * @param reporteFinal          texto del reporte final de la corrida.
 * @param resultado             resultado completo del motor.
 * @param formulasSinRecalcular fórmulas que POI no pudo recalcular.
 * @param archivosEntrada       qué archivos se usaron (fuente, nombre y tamaño).
 */
public record ReporteResultado(
        Periodo periodo,
        byte[] contenido,
        String reporteFinal,
        ResultadoMatching resultado,
        int formulasSinRecalcular,
        List<ArchivoUsado> archivosEntrada) {

    public ReporteResultado {
        Objects.requireNonNull(periodo, "periodo no puede ser null");
        Objects.requireNonNull(contenido, "contenido no puede ser null");
        Objects.requireNonNull(reporteFinal, "reporteFinal no puede ser null");
        Objects.requireNonNull(resultado, "resultado no puede ser null");
        archivosEntrada = List.copyOf(archivosEntrada);
    }

    public String nombreArchivo() {
        return periodo.nombreArchivo();
    }

    /** Un archivo usado en la corrida. */
    public record ArchivoUsado(String fuente, String nombre, long bytes) {

        static ArchivoUsado de(String fuente, ArchivoEntrada archivo) {
            return new ArchivoUsado(fuente, archivo.nombre(), archivo.contenido().length);
        }
    }
}
